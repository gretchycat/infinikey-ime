#!/usr/bin/env python3
"""Regression checks for fresh-checkout SDK/native-tool setup; no Android build needed."""
from pathlib import Path
import os
import platform
import shutil
import subprocess
import tempfile
import unittest

HELPER = Path(__file__).with_name('android-build-env.sh').resolve()

class AndroidBuildEnvironmentTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix='infini-build-env-')
        self.root = Path(self.tmp.name)
        self.home = self.root / 'home with spaces'
        self.home.mkdir()
        self.env = os.environ.copy()
        for name in ('ANDROID_HOME', 'ANDROID_SDK_ROOT', 'TERMUX_VERSION'):
            self.env.pop(name, None)
        self.env['HOME'] = str(self.home)

    def tearDown(self):
        self.tmp.cleanup()

    def invoke(self):
        return subprocess.run(['sh', '-c', '. "$1"; printf "%s\\n" "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$INFINI_AAPT2" "$INFINI_TARGET_ABI"',
                               'build-env-test', str(HELPER)], cwd=self.root, env=self.env, capture_output=True, text=True)

    def test_missing_sdk_fails_clearly(self):
        # The caller uses set -e, as every build.sh does.
        result = subprocess.run(['sh', '-ec', '. "$1"', 'test', str(HELPER)], cwd=self.root, env=self.env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('Android SDK not found', result.stderr)

    def test_termux_conventional_sdk_with_spaces(self):
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        result = self.invoke()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(str(self.home / 'android-sdk'), result.stdout.splitlines()[0])
        self.assertFalse((self.root / 'local.properties').exists())

    def test_standard_android_studio_sdk(self):
        (self.home / 'Android/Sdk/platforms').mkdir(parents=True)
        self.assertEqual(str(self.home / 'Android/Sdk'), self.invoke().stdout.splitlines()[0])

    def test_explicit_android_home_wins(self):
        self.env['ANDROID_HOME'] = str(self.root / 'explicit SDK')
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        self.assertEqual(self.env['ANDROID_HOME'], self.invoke().stdout.splitlines()[0])

    def test_sdk_root_is_respected(self):
        self.env['ANDROID_SDK_ROOT'] = str(self.root / 'root SDK')
        self.assertEqual(self.env['ANDROID_SDK_ROOT'], self.invoke().stdout.splitlines()[1])

    def test_local_properties_is_respected(self):
        (self.root / 'local.properties').write_text('sdk.dir=/explicit/sdk\n')
        self.assertEqual(0, self.invoke().returncode)
        self.assertEqual('', self.invoke().stdout.splitlines()[0])

    def test_termux_requires_native_aapt2(self):
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        self.env['TERMUX_VERSION'] = 'test'
        self.env['PATH'] = '/nonexistent'
        # Call the shell by absolute path because PATH is intentionally empty.
        shell = subprocess.check_output(['sh', '-c', 'command -v sh'], text=True).strip()
        result = subprocess.run([shell, '-ec', '. "$1"', 'test', str(HELPER)], cwd=self.root, env=self.env, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('native aapt2', result.stderr)


    def test_native_aapt2_path_and_abi(self):
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        binary = self.root / 'tools with spaces/aapt2'
        binary.parent.mkdir()
        binary.write_text('#!/usr/bin/env sh\nexit 0\n')
        binary.chmod(0o755)
        self.env['TERMUX_VERSION'] = 'test'
        self.env['PATH'] = str(binary.parent) + os.pathsep + self.env['PATH']
        lines = self.invoke().stdout.splitlines()
        self.assertEqual(str(binary), lines[2])
        expected = 'arm64-v8a' if platform.machine() in ('aarch64', 'arm64') else ''
        self.assertEqual(expected, lines[3])

    def test_build_script_preserves_arguments_and_working_directory(self):
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        (self.root / 'scripts').mkdir()
        shutil.copyfile(HELPER, self.root / 'scripts/android-build-env.sh')
        shutil.copyfile(HELPER.parent.parent / 'build.sh', self.root / 'build.sh')
        (self.root / 'build.sh').chmod(0o755)
        (self.root / 'scripts/fetch-dependencies.py').write_text('pass\n')
        wrapper = self.root / 'gradlew'
        wrapper.write_text('#!/usr/bin/env sh\nprintf "%s\\n" "$@"\n')
        wrapper.chmod(0o755)
        args = ['-Pexample=value with spaces', '--max-workers=1']
        result = subprocess.run([str(self.root / 'build.sh'), *args], cwd=self.home, env=self.env, capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(args, result.stdout.splitlines()[-2:])
        self.assertIn('-Dandroid.home=' + str(self.home / 'android-sdk'), result.stdout.splitlines())

    def test_build_script_supplies_native_flags_before_user_overrides(self):
        self.test_build_script_preserves_arguments_and_working_directory()
        self.env['TERMUX_VERSION'] = 'test'
        binary = self.root / 'native tools/aapt2'
        binary.parent.mkdir()
        binary.write_text('#!/usr/bin/env sh\nexit 0\n')
        binary.chmod(0o755)
        self.env['PATH'] = str(binary.parent) + os.pathsep + self.env['PATH']
        result = subprocess.run([str(self.root / 'build.sh'), '-PtargetAbi=override'], cwd=self.home, env=self.env, capture_output=True, text=True)
        self.assertEqual(0, result.returncode, result.stderr)
        args = result.stdout.splitlines()
        self.assertIn('-Pandroid.aapt2FromMavenOverride=' + str(binary), args)
        if platform.machine() in ('aarch64', 'arm64'):
            self.assertLess(args.index('-PtargetAbi=arm64-v8a'), args.index('-PtargetAbi=override'))
        self.assertEqual('-PtargetAbi=override', args[-1])

    def test_build_script_propagates_gradle_failure(self):
        (self.home / 'android-sdk/platforms').mkdir(parents=True)
        (self.root / 'scripts').mkdir()
        shutil.copyfile(HELPER, self.root / 'scripts/android-build-env.sh')
        shutil.copyfile(HELPER.parent.parent / 'build.sh', self.root / 'build.sh')
        (self.root / 'build.sh').chmod(0o755)
        (self.root / 'scripts/fetch-dependencies.py').write_text('pass\n')
        wrapper = self.root / 'gradlew'
        wrapper.write_text('#!/usr/bin/env sh\nexit 39\n')
        wrapper.chmod(0o755)
        result = subprocess.run([str(self.root / 'build.sh')], cwd=self.home, env=self.env, capture_output=True, text=True)
        self.assertEqual(39, result.returncode)

if __name__ == '__main__':
    unittest.main()

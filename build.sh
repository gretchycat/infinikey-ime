#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
. scripts/android-build-env.sh
copy_debug_apks() {
    apk_directory=$1
    set -- "$apk_directory"/*.apk
    if [ ! -f "$1" ]; then
        printf 'No APK was produced in %s\n' "$apk_directory" >&2
        return 1
    fi
    for apk_path do
        cp -f "$apk_path" .
        printf 'Copied APK to repository root: %s\n' "${apk_path##*/}"
    done
}
if [ -n "$INFINI_ANDROID_SDK" ]; then
    set -- "-Dandroid.home=$INFINI_ANDROID_SDK" "$@"
fi
if [ -n "$INFINI_AAPT2" ]; then
    set -- "-Pandroid.aapt2FromMavenOverride=$INFINI_AAPT2" "$@"
fi
if [ -n "$INFINI_TARGET_ABI" ]; then
    set -- "-PtargetAbi=$INFINI_TARGET_ABI" "$@"
fi
./gradlew :app:assembleDebug :app:testDebugUnitTest "$@"
copy_debug_apks app/build/outputs/apk/debug

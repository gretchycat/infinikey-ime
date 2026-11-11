#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
. scripts/android-build-env.sh
if [ -n "$INFINI_ANDROID_SDK" ]; then
    set -- "-Dandroid.home=$INFINI_ANDROID_SDK" "$@"
fi
if [ -n "$INFINI_AAPT2" ]; then
    set -- "-Pandroid.aapt2FromMavenOverride=$INFINI_AAPT2" "$@"
fi
if [ -n "$INFINI_TARGET_ABI" ]; then
    set -- "-PtargetAbi=$INFINI_TARGET_ABI" "$@"
fi
exec ./gradlew :app:assembleDebug :app:testDebugUnitTest "$@"

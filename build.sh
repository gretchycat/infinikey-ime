#!/usr/bin/env sh
set -eu
cd -- "$(dirname -- "$0")"
exec ./gradlew :app:assembleDebug :app:testDebugUnitTest "$@"

# Sourced by build.sh after changing to this checkout. No local files are written.
# Respect explicit SDK configuration; otherwise discover standard user locations.
if [ -z "${ANDROID_HOME:-}" ] && [ -z "${ANDROID_SDK_ROOT:-}" ]; then
    if [ -f local.properties ] && grep -Eq '^[[:space:]]*sdk[.]dir[[:space:]]*[:=]' local.properties; then
        : # Gradle reads this checkout's local.properties.
    else
        for infini_sdk_candidate in "${HOME}/android-sdk" "${HOME}/Android/Sdk" "${HOME}/Library/Android/sdk"; do
            if [ -d "$infini_sdk_candidate/platforms" ]; then
                ANDROID_HOME=$infini_sdk_candidate
                export ANDROID_HOME
                break
            fi
        done
        if [ -z "${ANDROID_HOME:-}" ]; then
            printf '%s\n' 'Android SDK not found. Set ANDROID_HOME or sdk.dir in this checkout’s local.properties.' >&2
            return 1
        fi
    fi
fi

# Gradle cannot update daemon environment variables on Termux. Forward the SDK
# as a JVM property as well, so an already running daemon sees this discovery.
INFINI_ANDROID_SDK=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}

INFINI_AAPT2=
INFINI_TARGET_ABI=
if [ -n "${TERMUX_VERSION:-}" ]; then
    INFINI_AAPT2=$(command -v aapt2 || true)
    if [ -z "$INFINI_AAPT2" ]; then
        printf '%s\n' 'Termux needs its native aapt2 executable. Install it with: pkg install aapt2' >&2
        return 1
    fi
    case "$(uname -m)" in
        aarch64|arm64) INFINI_TARGET_ABI=arm64-v8a ;;
    esac
fi

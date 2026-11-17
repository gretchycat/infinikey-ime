#!/bin/sh
set -e

# Usage: ./scripts/publish-release.sh ["Optional release notes text"]
#
# Builds the release APK, tags the commit as vX.Y.Z, pushes to GitHub,
# and creates or updates a GitHub Release with the built APK asset.

RELEASE_NOTES_INPUT="$1"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLE_PROPS="$ROOT_DIR/gradle.properties"

cd "$ROOT_DIR"

# Keep release builds on the same SDK/toolchain setup as build.sh. This is
# needed on Termux because Gradle daemons may not inherit Android SDK env vars.
. "$ROOT_DIR/scripts/android-build-env.sh"

# Accept both plain environment names and Gradle's ORG_GRADLE_PROJECT_ spelling.
INFINIKEY_RELEASE_STORE_FILE=${INFINIKEY_RELEASE_STORE_FILE:-${ORG_GRADLE_PROJECT_INFINIKEY_RELEASE_STORE_FILE:-}}
INFINIKEY_RELEASE_STORE_PASSWORD=${INFINIKEY_RELEASE_STORE_PASSWORD:-${ORG_GRADLE_PROJECT_INFINIKEY_RELEASE_STORE_PASSWORD:-}}
INFINIKEY_RELEASE_KEY_ALIAS=${INFINIKEY_RELEASE_KEY_ALIAS:-${ORG_GRADLE_PROJECT_INFINIKEY_RELEASE_KEY_ALIAS:-}}
INFINIKEY_RELEASE_KEY_PASSWORD=${INFINIKEY_RELEASE_KEY_PASSWORD:-${ORG_GRADLE_PROJECT_INFINIKEY_RELEASE_KEY_PASSWORD:-}}

# If any signing setting is unset, try loading from release-signing.properties
if [ -f "$ROOT_DIR/release-signing.properties" ]; then
  eval "$(python3 - "$ROOT_DIR/release-signing.properties" <<'PY'
import sys
from pathlib import Path

props_file = Path(sys.argv[1])
if props_file.is_file():
    for line in props_file.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line or line.startswith(("#", "!")):
            continue
        if "=" in line:
            k, v = line.split("=", 1)
            k, v = k.strip(), v.strip()
            if k in (
                "INFINIKEY_RELEASE_STORE_FILE",
                "INFINIKEY_RELEASE_STORE_PASSWORD",
                "INFINIKEY_RELEASE_KEY_ALIAS",
                "INFINIKEY_RELEASE_KEY_PASSWORD",
            ):
                val = v.replace("'", "'\\''")
                print(f"if [ -z \"${{{k}:-}}\" ]; then {k}='{val}'; fi")
PY
)"
fi

# Fall back to repository release keystore if present and unconfigured
if [ -z "${INFINIKEY_RELEASE_STORE_FILE:-}" ] && [ -f "$ROOT_DIR/app/release.keystore" ]; then
  INFINIKEY_RELEASE_STORE_FILE="app/release.keystore"
  INFINIKEY_RELEASE_STORE_PASSWORD="${INFINIKEY_RELEASE_STORE_PASSWORD:-programmer123}"
  INFINIKEY_RELEASE_KEY_ALIAS="${INFINIKEY_RELEASE_KEY_ALIAS:-programmer_keyboard}"
  INFINIKEY_RELEASE_KEY_PASSWORD="${INFINIKEY_RELEASE_KEY_PASSWORD:-programmer123}"
fi

# Publishing requires the same release key that users already have installed.
# Gradle can build an unsigned release for reproducible/F-Droid builds, but this
# script must never upload one that Android cannot install or upgrade.
if [ -z "${INFINIKEY_RELEASE_STORE_FILE:-}" ] ||
   [ -z "${INFINIKEY_RELEASE_STORE_PASSWORD:-}" ] ||
   [ -z "${INFINIKEY_RELEASE_KEY_ALIAS:-}" ] ||
   [ -z "${INFINIKEY_RELEASE_KEY_PASSWORD:-}" ]; then
  echo "Error: Release signing is not configured. Set INFINIKEY_RELEASE_STORE_FILE, INFINIKEY_RELEASE_STORE_PASSWORD, INFINIKEY_RELEASE_KEY_ALIAS, and INFINIKEY_RELEASE_KEY_PASSWORD." >&2
  exit 1
fi

case "$INFINIKEY_RELEASE_STORE_FILE" in
  /*) RELEASE_KEYSTORE_PATH="$INFINIKEY_RELEASE_STORE_FILE" ;;
  *) RELEASE_KEYSTORE_PATH="$ROOT_DIR/$INFINIKEY_RELEASE_STORE_FILE" ;;
esac
if [ ! -f "$RELEASE_KEYSTORE_PATH" ]; then
  echo "Error: Release keystore does not exist: $RELEASE_KEYSTORE_PATH" >&2
  exit 1
fi

# Pass signing values to Gradle through an ignored, short-lived properties file.
# This also works with Termux daemons that cannot refresh their environment.
SIGNING_PROPERTIES_FILE="$ROOT_DIR/build/release-signing.properties"
mkdir -p "$ROOT_DIR/build"
trap 'rm -f "$SIGNING_PROPERTIES_FILE"' EXIT
SIGNING_OLD_UMASK=$(umask)
umask 077
INFINIKEY_RELEASE_STORE_FILE="$INFINIKEY_RELEASE_STORE_FILE" \
INFINIKEY_RELEASE_STORE_PASSWORD="$INFINIKEY_RELEASE_STORE_PASSWORD" \
INFINIKEY_RELEASE_KEY_ALIAS="$INFINIKEY_RELEASE_KEY_ALIAS" \
INFINIKEY_RELEASE_KEY_PASSWORD="$INFINIKEY_RELEASE_KEY_PASSWORD" \
python3 - "$SIGNING_PROPERTIES_FILE" <<'PY'
import os
import sys
from pathlib import Path

def escape(value):
    escaped = []
    for index, char in enumerate(value):
        if char == "\\":
            escaped.append("\\\\")
        elif char == "\\t":
            escaped.append("\\t")
        elif char == "\\n":
            escaped.append("\\n")
        elif char == "\\r":
            escaped.append("\\r")
        elif char == "\\f":
            escaped.append("\\f")
        elif char in "=:#!" or (char == " " and index == 0):
            escaped.append("\\" + char)
        else:
            escaped.append(char)
    return "".join(escaped)
keys = (
    "INFINIKEY_RELEASE_STORE_FILE",
    "INFINIKEY_RELEASE_STORE_PASSWORD",
    "INFINIKEY_RELEASE_KEY_ALIAS",
    "INFINIKEY_RELEASE_KEY_PASSWORD",
)
path = Path(sys.argv[1])
path.write_text("".join(f"{key}={escape(os.environ[key])}\n" for key in keys), encoding="utf-8")
PY
umask "$SIGNING_OLD_UMASK"

APKSIGNER=$(command -v apksigner || true)
SDK_ROOT=${INFINI_ANDROID_SDK:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}}
if [ -z "$SDK_ROOT" ] && [ -f "$ROOT_DIR/local.properties" ]; then
  SDK_ROOT=$(sed -n -E 's/^[[:space:]]*sdk[.]dir[[:space:]]*[:=][[:space:]]*//p' "$ROOT_DIR/local.properties" | head -n 1 | sed 's/\\:/:/g')
fi
if [ -z "$APKSIGNER" ] && [ -n "$SDK_ROOT" ]; then
  for infini_apksigner_candidate in "$SDK_ROOT"/build-tools/*/apksigner; do
    if [ -x "$infini_apksigner_candidate" ]; then
      APKSIGNER=$infini_apksigner_candidate
    fi
  done
fi
if [ -z "$APKSIGNER" ]; then
  echo "Error: Could not find apksigner. Install Android SDK build-tools or add apksigner to PATH." >&2
  exit 1
fi

set -- --warning-mode=all
if [ -n "$INFINI_ANDROID_SDK" ]; then
  set -- "-Dandroid.home=$INFINI_ANDROID_SDK" "$@"
fi
if [ -n "$INFINI_AAPT2" ]; then
  set -- "-Pandroid.aapt2FromMavenOverride=$INFINI_AAPT2" "$@"
fi
if [ -n "$INFINI_TARGET_ABI" ]; then
  set -- "-PtargetAbi=$INFINI_TARGET_ABI" "$@"
fi

if [ ! -f "$GRADLE_PROPS" ]; then
  echo "Error: Cannot find $GRADLE_PROPS" >&2
  exit 1
fi

VERSION=$(grep -E '^versionName=' "$GRADLE_PROPS" | cut -d= -f2 | tr -d '\r')
TAG_NAME="v${VERSION}"

echo "=========================================="
echo " Publishing Release: $TAG_NAME"
echo "=========================================="

# 1. Commit any uncommitted changes first (version bump, metadata, layout updates)
if [ -n "$(git status --porcelain)" ]; then
  echo "--> Committing release updates for $TAG_NAME..."
  git add -A
  git commit -m "Release $TAG_NAME"
fi

# 2. Ensure metadata commit reference is set to tag name
METADATA_YML="$ROOT_DIR/metadata/com.infinikey_ime.yml"
if [ -f "$METADATA_YML" ]; then
  sed -i -E "s/commit: .*/commit: $TAG_NAME/" "$METADATA_YML"
  if [ -n "$(git status --porcelain "$METADATA_YML")" ]; then
    git add "$METADATA_YML"
    git commit --amend --no-edit
  fi
fi

# 3. Sync with remote main first so push is never rejected
echo "--> Syncing with origin/main..."
git fetch origin main || true
if git rev-parse --verify origin/main >/dev/null 2>&1; then
  git rebase origin/main || git merge origin/main --no-edit || true
fi

# 4. Build release APK. Remove any stale unsigned artifact before Gradle runs.
APK_PATH="$ROOT_DIR/app/build/outputs/apk/release/infinikey-ime-v${VERSION}-release.apk"
rm -f "$APK_PATH"
echo "--> Building release APK with ./gradlew :app:assembleRelease..."
./gradlew :app:assembleRelease "$@"

if [ ! -f "$APK_PATH" ]; then
  echo "Error: Could not find generated release APK: $APK_PATH" >&2
  exit 1
fi

echo "--> Verifying release APK signature..."
if ! "$APKSIGNER" verify --verbose "$APK_PATH"; then
  echo "Error: Release APK signature verification failed; refusing to tag or publish it." >&2
  exit 1
fi

echo "--> Built and verified APK successfully: $APK_PATH"

# 5. Tag current commit (force update local tag if it already exists)
echo "--> Tagging $TAG_NAME..."
git tag -f "$TAG_NAME"

# 6. Push main & tag to origin (force push tag so origin points to latest built commit)
echo "--> Pushing main and tag $TAG_NAME to origin..."
git push origin main
git push origin "$TAG_NAME" --force

# 7. Prepare Release Notes
if [ -n "$RELEASE_NOTES_INPUT" ]; then
  NOTES="$RELEASE_NOTES_INPUT"
else
  NOTES="Release $TAG_NAME for Infinikey IME."
fi

# 8. Create or update GitHub release via gh CLI
echo "--> Publishing GitHub release $TAG_NAME..."
if [ -z "${GH_TOKEN:-}" ] && ! gh auth status >/dev/null 2>&1 && [ -f "$HOME/.git-credentials" ]; then
  _GH_TOKEN_CANDIDATE=$(sed -n -E 's#https://([^:]+):([^@]+)@github\.com.*#\2#p' "$HOME/.git-credentials" | head -n 1)
  if [ -n "$_GH_TOKEN_CANDIDATE" ]; then
    export GH_TOKEN="$_GH_TOKEN_CANDIDATE"
  fi
fi

if gh release view "$TAG_NAME" >/dev/null 2>&1; then
  echo "--> Updating existing GitHub release $TAG_NAME..."
  gh release upload "$TAG_NAME" "$APK_PATH" --clobber
else
  gh release create "$TAG_NAME" "$APK_PATH" --title "$TAG_NAME" --notes "$NOTES"
fi

echo "=========================================="
echo " Release $TAG_NAME published successfully!"
echo " URL: https://github.com/infiniforge/infinikey-ime/releases/tag/$TAG_NAME"
echo "=========================================="

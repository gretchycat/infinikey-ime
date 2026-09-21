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

# 4. Build signed release APK
echo "--> Building release APK with ./gradlew assembleRelease..."
./gradlew assembleRelease

APK_PATH=$(ls app/build/outputs/apk/release/infinikey-ime-v${VERSION}*.apk 2>/dev/null | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
  echo "Error: Could not find generated release APK matching app/build/outputs/apk/release/infinikey-ime-v${VERSION}*.apk" >&2
  exit 1
fi

echo "--> Built APK successfully: $APK_PATH"

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
if gh release view "$TAG_NAME" >/dev/null 2>&1; then
  echo "--> Updating existing GitHub release $TAG_NAME..."
  gh release upload "$TAG_NAME" "$APK_PATH" --clobber
else
  gh release create "$TAG_NAME" "$APK_PATH" --title "$TAG_NAME" --notes "$NOTES"
fi

echo "=========================================="
echo " Release $TAG_NAME published successfully!"
echo " URL: https://github.com/gretchycat/infinikey-ime/releases/tag/$TAG_NAME"
echo "=========================================="

#!/bin/sh
set -e

# Usage: ./scripts/publish-release.sh ["Optional release notes text"]
#
# Builds the release APK, tags the commit as vX.Y.Z, pushes to GitHub,
# and creates a GitHub Release with the built APK asset.

RELEASE_NOTES_INPUT="$1"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
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

# 1. Build signed release APK
echo "--> Building release APK with sh gradlew assembleRelease..."
sh gradlew assembleRelease

APK_PATH=$(ls app/build/outputs/apk/release/infinikey-ime-v${VERSION}*.apk 2>/dev/null | head -n 1)

if [ -z "$APK_PATH" ] || [ ! -f "$APK_PATH" ]; then
  echo "Error: Could not find generated release APK matching app/build/outputs/apk/release/infinikey-ime-v${VERSION}*.apk" >&2
  exit 1
fi

echo "--> Built APK successfully: $APK_PATH"

# 2. Update metadata recipe with release commit SHA and commit changes
METADATA_YML="$ROOT_DIR/metadata/com.infinikey_ime.yml"

if [ -n "$(git status --porcelain)" ]; then
  echo "--> Committing version update..."
  git add -A
  git commit -m "Release $TAG_NAME"
fi

if [ -f "$METADATA_YML" ]; then
  sed -i -E "s/commit: .*/commit: $TAG_NAME/" "$METADATA_YML"
  if [ -n "$(git status --porcelain "$METADATA_YML")" ]; then
    git add "$METADATA_YML"
    git commit --amend --no-edit
  fi
fi

# 3. Create tag if it doesn't exist
if git rev-parse "$TAG_NAME" >/dev/null 2>&1; then
  echo "--> Tag $TAG_NAME already exists locally."
else
  echo "--> Tagging $TAG_NAME..."
  git tag "$TAG_NAME"
fi

# 4. Push main & tag to GitHub
echo "--> Pushing main and tag $TAG_NAME to origin..."
git push origin main
git push origin "$TAG_NAME"

# 5. Prepare Release Notes
if [ -n "$RELEASE_NOTES_INPUT" ]; then
  NOTES="$RELEASE_NOTES_INPUT"
else
  NOTES="Release $TAG_NAME for Infinikey IME."
fi

# 6. Publish Release using GitHub CLI
echo "--> Publishing GitHub release $TAG_NAME..."
gh release create "$TAG_NAME" "$APK_PATH" --title "$TAG_NAME" --notes "$NOTES"

echo "=========================================="
echo " Release $TAG_NAME published successfully!"
echo " URL: https://github.com/gretchycat/infinikey-ime/releases/tag/$TAG_NAME"
echo "=========================================="

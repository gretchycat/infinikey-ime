#!/bin/sh
set -e

# Usage: ./scripts/bump-version.sh [patch|minor|major]
#   patch (or debug/bugfix): 0.2.6 -> 0.2.7
#   minor:                   0.2.6 -> 0.3.0
#   major:                   0.2.6 -> 1.0.0

BUMP_TYPE="${1:-patch}"
BUMP_TYPE=$(echo "$BUMP_TYPE" | tr '[:upper:]' '[:lower:]')

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
GRADLE_PROPS="$ROOT_DIR/gradle.properties"

if [ ! -f "$GRADLE_PROPS" ]; then
    echo "Error: Cannot find $GRADLE_PROPS" >&2
    exit 1
fi

# Extract current versionName from gradle.properties
CURRENT_VERSION=$(grep -E '^versionName=' "$GRADLE_PROPS" | cut -d= -f2 | tr -d '\r')

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Could not extract versionName from $GRADLE_PROPS" >&2
    exit 1
fi

MAJOR=$(echo "$CURRENT_VERSION" | cut -d. -f1)
MINOR=$(echo "$CURRENT_VERSION" | cut -d. -f2)
PATCH=$(echo "$CURRENT_VERSION" | cut -d. -f3)

case "$BUMP_TYPE" in
    patch|debug|bugfix|build)
        PATCH=$((PATCH + 1))
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    *)
        echo "Error: Invalid version bump parameter '$1'." >&2
        echo "Must be 'patch' (or 'debug'), 'minor', or 'major'." >&2
        echo "Usage: $0 [patch|minor|major]" >&2
        exit 1
        ;;
esac

NEW_VERSION="${MAJOR}.${MINOR}.${PATCH}"

echo "Bumping versionName: $CURRENT_VERSION -> $NEW_VERSION (bump level: $BUMP_TYPE)"

# Calculate next versionCode based on git commit count and current metadata/properties versionCode
GIT_VC=$(git rev-list --count HEAD 2>/dev/null || echo 0)
PREV_VC=$(grep -E '^versionCode=' "$GRADLE_PROPS" | cut -d= -f2 | tr -d '\r')
[ -z "$PREV_VC" ] && PREV_VC=255

NEW_VC=$((GIT_VC + 1))
if [ "$NEW_VC" -le "$PREV_VC" ]; then
    NEW_VC=$((PREV_VC + 1))
fi

# Update gradle.properties
sed -i -E "s/^versionName=.*/versionName=$NEW_VERSION/" "$GRADLE_PROPS"
sed -i -E "s/^versionCode=.*/versionCode=$NEW_VC/" "$GRADLE_PROPS"

# Update app/build.gradle.kts (literal values for fdroidserver regex parser)
GRADLE_KTS="$ROOT_DIR/app/build.gradle.kts"
if [ -f "$GRADLE_KTS" ]; then
    sed -i -E "s/versionName = \"[^\"]+\"/versionName = \"$NEW_VERSION\"/" "$GRADLE_KTS"
    sed -i -E "s/versionCode = [0-9]+/versionCode = $NEW_VC/" "$GRADLE_KTS"
fi

# Update version in assets/layouts/*.json
for layout_file in "$ROOT_DIR"/app/src/main/assets/layouts/*.json; do
    if [ -f "$layout_file" ]; then
        sed -i -E "s/\"version\": \"[^\"]+\"/\"version\": \"$NEW_VERSION\"/" "$layout_file"
    fi
done

# Update metadata/com.infinikey_ime.yml or .fdroid.yml if present
METADATA_YML="$ROOT_DIR/metadata/com.infinikey_ime.yml"
if [ -f "$METADATA_YML" ]; then
    COMMIT_SHA=$( (git rev-parse --verify "v$NEW_VERSION^{commit}" 2>/dev/null || git rev-parse HEAD 2>/dev/null || echo "HEAD") | tr -d '\r\n' )
    sed -i -E "s/versionName: .*/versionName: $NEW_VERSION/" "$METADATA_YML"
    sed -i -E "s/versionCode: [0-9]+/versionCode: $NEW_VC/" "$METADATA_YML"
    sed -i -E "s/commit: .*/commit: $COMMIT_SHA/" "$METADATA_YML"
    sed -i -E "s/CurrentVersion: .*/CurrentVersion: $NEW_VERSION/" "$METADATA_YML"
    sed -i -E "s/CurrentVersionCode: [0-9]+/CurrentVersionCode: $NEW_VC/" "$METADATA_YML"
fi

FDROID_YML="$ROOT_DIR/.fdroid.yml"
if [ -f "$FDROID_YML" ]; then
    sed -i -E "s/CurrentVersion: '[^']+'/CurrentVersion: '$NEW_VERSION'/" "$FDROID_YML"
    sed -i -E "s/CurrentVersionCode: [0-9]+/CurrentVersionCode: $NEW_VC/" "$FDROID_YML"
fi

echo "Successfully updated version to $NEW_VERSION (versionCode $NEW_VC) in gradle.properties, assets layout files, and metadata recipe."

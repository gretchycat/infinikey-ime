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
GRADLE_KTS="$ROOT_DIR/app/build.gradle.kts"

if [ ! -f "$GRADLE_KTS" ]; then
    echo "Error: Cannot find $GRADLE_KTS" >&2
    exit 1
fi

# Extract current baseVersionName
CURRENT_VERSION=$(grep -E 'val baseVersionName = "' "$GRADLE_KTS" | sed -E 's/.*"([^"]+)".*/\1/')

if [ -z "$CURRENT_VERSION" ]; then
    echo "Error: Could not extract baseVersionName from $GRADLE_KTS" >&2
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

echo "Bumping baseVersionName: $CURRENT_VERSION -> $NEW_VERSION (bump level: $BUMP_TYPE)"

# Update app/build.gradle.kts
sed -i -E "s/val baseVersionName = \"[^\"]+\"/val baseVersionName = \"$NEW_VERSION\"/" "$GRADLE_KTS"

# Update version in assets/layouts/*.json
for layout_file in "$ROOT_DIR"/app/src/main/assets/layouts/*.json; do
    if [ -f "$layout_file" ]; then
        sed -i -E "s/\"version\": \"[^\"]+\"/\"version\": \"$NEW_VERSION\"/" "$layout_file"
    fi
done

echo "Successfully updated version to $NEW_VERSION in app/build.gradle.kts and assets layout files."

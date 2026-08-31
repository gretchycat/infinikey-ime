# Finds wrapper URL, extracts version/type, queries/computes hash, and appends to properties
PROP_FILE="gradle/wrapper/gradle-wrapper.properties"
DIST_URL=$(grep '^distributionUrl=' "$PROP_FILE" | sed 's/\\//g' | cut -d= -f2)

# Download checksum file directly from services.gradle.org
SHA=$(curl -sL "${DIST_URL}.sha256")

if [ -n "$SHA" ] && [ ${#SHA} -eq 64 ]; then
  sed -i '/distributionSha256Sum/d' "$PROP_FILE"
  echo "distributionSha256Sum=$SHA" >>"$PROP_FILE"
  echo "Updated $PROP_FILE with SHA-256: $SHA"
else
  echo "Failed to fetch valid SHA-256 for $DIST_URL"
fi

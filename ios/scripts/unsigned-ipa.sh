#!/bin/sh
# Build an unsigned TokiNote.ipa (Payload/*.app) for SideStore.
# Usage: ios/scripts/unsigned-ipa.sh [output.ipa]
set -eu
cd "$(dirname "$0")/.."
OUT="${1:-build/TokiNote-ios-0.3.7.ipa}"
DERIVED="${DERIVED_DATA_PATH:-build/DerivedData}"
mkdir -p "$(dirname "$OUT")" build
xcodebuild \
  -project TokiNote.xcodeproj \
  -scheme TokiNote \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath "$DERIVED" \
  build \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY=""
APP="$(find "$DERIVED" -path '*Release-iphoneos/TokiNote.app' -type d | head -n 1)"
if [ -z "$APP" ]; then
  echo "TokiNote.app was not produced" >&2
  exit 1
fi
ROOT="$(pwd)"
case "$OUT" in
  /*) ABS="$OUT" ;;
  *) ABS="$ROOT/$OUT" ;;
esac
rm -rf "$ROOT/build/Payload"
mkdir -p "$ROOT/build/Payload" "$(dirname "$ABS")"
cp -R "$APP" "$ROOT/build/Payload/"
rm -f "$ABS"
(
  cd "$ROOT/build"
  zip -r -y "$ABS" Payload
)
echo "Wrote $ABS"

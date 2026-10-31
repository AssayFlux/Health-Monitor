#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "\${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"
command -v gradle >/dev/null 2>&1 || { echo "Gradle 8.13+ and Android SDK 36 are required." >&2; exit 2; }
BUILD_DEBUG="\${BUILD_DEBUG:-true}"
BUILD_RELEASE="\${BUILD_RELEASE:-true}"
if [[ "$BUILD_DEBUG" == "true" ]]; then gradle :app:assembleDebug :app:bundleDebug --no-daemon; fi
if [[ "$BUILD_RELEASE" == "true" ]]; then gradle :app:assembleRelease :app:bundleRelease --no-daemon; fi
OUT="$ROOT/build-artifacts"
mkdir -p "$OUT/debug" "$OUT/release"
if [[ "$BUILD_DEBUG" == "true" ]]; then
  find app/build/outputs/apk/debug app/build/outputs/bundle/debug -type f \\( -name '*.apk' -o -name '*.aab' \\) -exec cp {} "$OUT/debug/" \\; 2>/dev/null || true
fi
if [[ "$BUILD_RELEASE" == "true" ]]; then
  find app/build/outputs/apk/release app/build/outputs/bundle/release -type f \\( -name '*.apk' -o -name '*.aab' \\) -exec cp {} "$OUT/release/" \\; 2>/dev/null || true
fi
echo "Artifacts copied to $OUT"
echo "AAB files are not directly installable. Release outputs may be unsigned."

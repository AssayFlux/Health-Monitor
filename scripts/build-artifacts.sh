#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

command -v gradle >/dev/null 2>&1 || {
  echo "Gradle 8.13+ and Android SDK 36 are required." >&2
  exit 2
}

BUILD_DEBUG="${BUILD_DEBUG:-true}"
BUILD_RELEASE="${BUILD_RELEASE:-true}"

if [[ "$BUILD_DEBUG" != "true" && "$BUILD_DEBUG" != "false" ]]; then
  echo "BUILD_DEBUG must be true or false." >&2
  exit 2
fi
if [[ "$BUILD_RELEASE" != "true" && "$BUILD_RELEASE" != "false" ]]; then
  echo "BUILD_RELEASE must be true or false." >&2
  exit 2
fi
if [[ "$BUILD_DEBUG" == "false" && "$BUILD_RELEASE" == "false" ]]; then
  echo "At least one build type must be enabled." >&2
  exit 2
fi

if [[ "$BUILD_DEBUG" == "true" ]]; then
  gradle :app:assembleDebug :app:bundleDebug --no-daemon
fi
if [[ "$BUILD_RELEASE" == "true" ]]; then
  gradle :app:assembleRelease :app:bundleRelease --no-daemon
fi

OUT="$ROOT/build-artifacts"
mkdir -p "$OUT/debug" "$OUT/release"

if [[ "$BUILD_DEBUG" == "true" ]]; then
  mapfile -t debug_apks < <(find app/build/outputs/apk/debug -type f -name '*.apk' -print)
  mapfile -t debug_aabs < <(find app/build/outputs/bundle/debug -type f -name '*.aab' -print)
  (("${#debug_apks[@]}" > 0)) || { echo "Debug APK was not produced." >&2; exit 1; }
  (("${#debug_aabs[@]}" > 0)) || { echo "Debug AAB was not produced." >&2; exit 1; }
  cp "${debug_apks[@]}" "${debug_aabs[@]}" "$OUT/debug/"
fi

if [[ "$BUILD_RELEASE" == "true" ]]; then
  mapfile -t release_apks < <(find app/build/outputs/apk/release -type f -name '*.apk' -print)
  mapfile -t release_aabs < <(find app/build/outputs/bundle/release -type f -name '*.aab' -print)
  (("${#release_apks[@]}" > 0)) || { echo "Release APK was not produced." >&2; exit 1; }
  (("${#release_aabs[@]}" > 0)) || { echo "Release AAB was not produced." >&2; exit 1; }
  cp "${release_apks[@]}" "${release_aabs[@]}" "$OUT/release/"
fi

echo "Artifacts copied to $OUT"
echo "AAB files are not directly installable. Release outputs may be unsigned."

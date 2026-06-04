#!/usr/bin/env bash
# Copy sample vocab files to Android Downloads/MinLish (visible in Files app)
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET="/sdcard/Download/MinLish"

if ! command -v adb >/dev/null 2>&1; then
  echo "adb not found. Install Android platform-tools or set PATH."
  exit 1
fi

if ! adb get-state >/dev/null 2>&1; then
  echo "No device/emulator connected. Start an emulator or connect a phone with USB debugging."
  exit 1
fi

adb shell mkdir -p "$TARGET"
for f in "$SCRIPT_DIR"/*.csv "$SCRIPT_DIR"/*.xlsx; do
  [ -f "$f" ] || continue
  adb push "$f" "$TARGET/"
  echo "Pushed $(basename "$f") -> $TARGET/"
done

echo ""
echo "Done. Open Files -> Downloads -> MinLish on your device."

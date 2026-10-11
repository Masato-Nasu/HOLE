#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
FILE="${1:-docs/screenshots/HOLE-actual-device.png}"
command -v adb >/dev/null || { echo 'adb をインストールしてください: sudo apt install adb'; exit 1; }
adb devices
mkdir -p "$(dirname "$FILE")"
adb exec-out screencap -p > "$FILE"
file "$FILE"
echo "実機画面を保存しました: $FILE"

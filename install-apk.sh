#!/bin/sh
set -eu
cd "$(dirname "$0")"
adb install -r app/build/outputs/apk/debug/app-debug.apk

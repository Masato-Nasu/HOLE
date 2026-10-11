#!/bin/sh
set -eu
cd "$(dirname "$0")"
./gradlew assembleDebug
printf '\nAPK:\n%s\n' "$(pwd)/app/build/outputs/apk/debug/app-debug.apk"

#!/bin/sh
set -eu
cd "$(dirname "$0")"
MANIFEST=app/src/main/AndroidManifest.xml
for p in android.permission.CAMERA android.permission.SYSTEM_ALERT_WINDOW android.permission.FOREGROUND_SERVICE_CAMERA; do
  grep -q "$p" "$MANIFEST" || { echo "Missing $p"; exit 1; }
done
if grep -q 'android.permission.INTERNET' "$MANIFEST"; then
  echo "Unexpected INTERNET permission"; exit 1
fi
grep -q 'foregroundServiceType="camera"' "$MANIFEST" || { echo "Missing camera foreground service type"; exit 1; }
grep -q 'FLAG_NOT_TOUCHABLE' app/src/main/java/com/masatolab/hole/OverlayService.java || { echo "Overlay is not touch-through"; exit 1; }
grep -q 'params.alpha = Prefs.alpha' app/src/main/java/com/masatolab/hole/OverlayService.java || { echo "Window opacity application missing"; exit 1; }
echo "Project structure checks: PASS"

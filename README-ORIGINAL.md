# HOLE — Transparent Phone P0.1

HOLE keeps the rear camera running as a touch-through, full-screen Android overlay. Any app underneath remains usable while the real world is mixed into the display.

## P0.1

- Camera overlay over any app
- Default 50% camera layer
- Adjustable 10–79% touch-through camera opacity
- `HOLE 100%` camera-only illusion mode (view-only on Android 12+)
- Touch-through overlay in the practical range; 50% is the default
- Manual optical calibration: scale + X/Y camera offset
- Foreground camera service with persistent STOP notification
- No network permission, no analytics, no image/video saving
- Android 8.0+ (API 26), target API 35

## Why calibration matters

The rear camera is not physically located at the center of the display and its field of view does not match the display. HOLE therefore lets you manually compensate for scale and camera offset. The best calibration depends on viewing distance because parallax is real; P0.1 stores one practical calibration.

## Build

Open the folder in Android Studio, let Gradle sync, and run the `app` configuration on the phone.

Command-line build on Linux (Android SDK with API 35 installed). The first run downloads and verifies the official Gradle 8.11.1 wrapper/distribution:

```bash
cd ~/ダウンロード/HOLE-Transparent-Phone
./gradlew assembleDebug
```

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install with ADB:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First use

1. Allow CAMERA.
2. Allow “Display over other apps”.
3. Open CALIBRATE and aim at a straight-edged scene roughly 30–50 cm away.
4. Adjust SCALE / X OFFSET / Y OFFSET until the scene appears to continue across the physical edge of the phone.
5. Save.
6. Choose 50% or another touch-through opacity (10–79%). `HOLE 100%` is a view-only optical test.
7. Tap START TRANSPARENT PHONE.
8. Use any other app normally. Stop HOLE from its persistent notification or reopen HOLE and tap STOP.

## Android limitations

- Some security-sensitive screens can suppress third-party overlays.
- Android may show camera/privacy indicators while HOLE is active.
- Android 12+ blocks pass-through touches when an untrusted overlay is too opaque. HOLE therefore keeps interactive opacity at 79% or below; 100% is view-only.
- 100% opacity hides the app UI underneath; 50% mixes camera and app approximately equally.
- A single 2D calibration cannot perfectly cancel camera parallax at every viewing distance.

## Privacy

HOLE does not request Internet access and does not record or save camera frames. The camera is used only for the live preview.

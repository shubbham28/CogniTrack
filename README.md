# CogniTrack

CogniTrack turns digital behavior into fitness-style analytics:

- Workout -> app session
- Distance -> time spent
- Pace -> intensity from app switches and interruptions
- Heart rate -> cognitive load from notifications and multitasking
- Route -> app flow sequence

Current app behavior:

- Imports real on-device `UsageStats` after you grant Usage Access
- Starts a foreground tracker service that records `SCREEN_ON`, `SCREEN_OFF`, and `USER_PRESENT` events
- Builds the dashboard from actual app sessions, pickups, app switches, and app-flow sequences
- Uses 7 days of detailed event import for timeline and heatmap
- Uses 7/30/90 day aggregate usage totals for trend cards
- Persists imported raw events, stitched sessions, and daily summaries into Room

The project is split into:

- `capture-core` for collectors and permission routing
- `analytics-core` for session stitching and digital fitness metrics
- `storage` for Room persistence and retention jobs
- `ui-dashboard` for the athletic-editorial Compose dashboard
- `app` for the Android entry point

## Local Android setup

This repo is already configured to build with:

- Android SDK root: `/opt/homebrew/share/android-commandlinetools`
- Platform: `android-35`
- Build tools: `35.0.0`
- Platform tools / `adb`

`local.properties` is intentionally ignored by git. In this workspace it should contain:

```properties
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

## Test on Pixel 7

1. Enable Developer Options and USB debugging on the phone.
2. Connect the phone by USB and accept the debugging prompt.
3. Verify the device is visible:

```bash
adb devices -l
```

4. Build and install:

```bash
./gradlew installDebug
```

5. Launch manually from the phone, or with:

```bash
adb shell am start -n com.digitalwellbeing.app/.MainActivity
```

6. In the app, tap `Open Usage Access`, enable CogniTrack, then return to the app.
7. The app will refresh on resume, import real device activity, and start a foreground tracking notification.

If `adb devices -l` shows no devices:

- unlock the phone
- confirm the USB mode is not charge-only
- replug the cable
- accept the RSA debugging prompt on the phone
- run `adb kill-server && adb start-server && adb devices -l`

APK output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

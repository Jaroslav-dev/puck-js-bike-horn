# Puck.js Bike Horn

A wireless bike horn and crash detector. Press a [Puck.js](https://www.espruino.com/Puck.js) button mounted on your handlebar to blast a horn through your Bluetooth speaker. The accelerometer detects crashes and sends an SMS emergency alert.

## Hardware Required

- **[Puck.js](https://www.espruino.com/Puck.js)** — the wireless button/sensor (v2 recommended for motion sounds)
- **Android phone** running Android 8.0 (API 26) or higher
- **Bluetooth speaker** — mounts to your handlebar, horn plays through it

## Features

- **Wireless horn button** — tap the Puck.js to trigger sounds from up to ~10m away
- **4 button patterns** — 1 tap, 2 taps, 3 taps, long press (2s), each assignable to a different sound
- **Custom sounds** — assign any audio file from your phone's storage to any button
- **750Hz pedestrian bell** — built-in tone at 750Hz, a frequency that penetrates noise-cancelling headphones
- **Crash detection** — accelerometer spike triggers a countdown alarm; SMS sent to emergency contact if not dismissed
- **Motion sounds** — plays sounds on hard acceleration or braking (Puck.js v2 only)
- **NFC tap-to-pair** — tap your phone to the Puck.js to open the app and connect automatically
- **Battery indicators** — shows Puck.js battery level and Bluetooth speaker battery (if supported)
- **Foreground service** — keeps BLE connection alive when the app is in the background
- **Audio auto-routing** — sound plays through whichever Bluetooth speaker is connected; falls back to phone speaker

## Installation

### Option A — Download APK (easiest)

1. Go to the [Releases](../../releases) page and download the latest `app-release.apk`
2. On your Android phone, go to **Settings → Apps → Special app access → Install unknown apps** and allow your browser or file manager
3. Open the downloaded APK and tap **Install**

### Option B — Build from source

Requirements: Android Studio or the Android SDK command-line tools.

```bash
git clone https://github.com/your-username/puck-js-bike-horn.git
cd puck-js-bike-horn/android
./gradlew assembleDebug
# APK output: app/build/outputs/apk/debug/app-debug.apk
```

## Flashing the Firmware

1. Open the [Espruino Web IDE](https://www.espruino.com/ide) in Chrome
2. Connect to your Puck.js over BLE
3. Open `firmware/bike_horn.js`
4. Click **Send to Espruino**, then **Save to Flash** — the firmware must be saved to flash to survive reboots

The Puck.js LED will blink green when connected to the app and red when disconnected or a crash is detected.

## Setup

1. Open the app and tap **Scan for Puck.js**
2. Tap your Puck.js in the list to connect
   - Alternatively, tap your phone to the Puck.js (NFC) to connect automatically
3. Tap **Sound Setup** to assign sounds to each button pattern
4. Go to **Settings** to configure your emergency contact for crash alerts

> **Tip:** Do not pair the Puck.js via Android's system Bluetooth settings. Pair from within the app only — system pairing prevents the Puck.js from advertising for BLE scan.

## Button Patterns

| Press | Default sound |
|-------|--------------|
| 1 tap | Bicycle Bell |
| 2 taps | Tram Bell |
| 3 taps | Police Siren |
| Long press (2s) | UFO |

All patterns are reassignable, including to your own audio files or media controls (next track, play/pause, etc.).

## Crash Detection

When the Puck.js accelerometer detects a sudden impact:

1. The app takes over the screen with a countdown (default 10 seconds)
2. Tap **I'M OK** to dismiss
3. If not dismissed, an alarm plays and an SMS is sent to your emergency contact with your GPS location

The crash threshold and countdown duration are configurable in Settings.

## Building a Release APK

1. Generate a signing keystore (one-time):
   ```bash
   keytool -genkey -v -keystore bikehorn.jks -alias bikehorn \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Copy `keystore.properties.example` to `keystore.properties` and fill in your keystore path and passwords
3. Build:
   ```bash
   cd android && ./gradlew assembleRelease
   # APK output: app/build/outputs/apk/release/app-release.apk
   ```

## Project Structure

```
firmware/bike_horn.js        # Flash this to the Puck.js via Espruino Web IDE
android/                     # Android app (Kotlin + Jetpack Compose)
```

See [CLAUDE.md](CLAUDE.md) for full architecture details and design decisions.

## License

MIT

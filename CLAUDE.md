# Puck.js Bike Horn & Crash Detection

## What This Is

A bike-mounted system using a Puck.js as a wireless button/sensor that connects to an Android phone via BLE. Press the Puck.js button to play horn/bell sounds through a Bluetooth speaker (or phone speaker as fallback). The accelerometer detects crashes and triggers an emergency alert.

## Business Context

The developer already sells 3D-printed bike handlebar mounts for Sonos and JBL portable speakers. This app is meant to increase sales by bundling a Puck.js horn button kit as an add-on accessory. The horn plays through the customer's mounted Bluetooth speaker. The crash detection adds a safety angle for marketing. The app itself should remain free — revenue comes from hardware sales.

## Architecture

Two components communicating over BLE Nordic UART Service (NUS):

1. **Puck.js firmware** (`firmware/bike_horn.js`) - JavaScript running on the Puck.js hardware. Classifies button press patterns and monitors the accelerometer. Sends JSON events via `Bluetooth.println()`.

2. **Android app** (`android/`) - Kotlin + Jetpack Compose. Receives BLE events, plays sounds via SoundPool, manages crash alerts with countdown and optional SMS.

### Communication Protocol

Puck.js sends newline-delimited JSON over BLE UART TX characteristic:
- `{"t":"short_press"}` - button tap < 2s
- `{"t":"long_press"}` - button held 2-4s
- `{"t":"very_long_press"}` - button held > 4s
- `{"t":"repeated_press"}` - 3+ short taps within 500ms windows
- `{"t":"crash","d":4.2}` - accelerometer spike, `d` = g-force magnitude

## Project Structure

```
firmware/bike_horn.js              # Puck.js firmware (flash via Espruino Web IDE)

android/
├── build.gradle.kts               # Project-level Gradle (Kotlin DSL)
├── settings.gradle.kts
├── gradle/libs.versions.toml      # Version catalog
└── app/src/main/
    ├── AndroidManifest.xml
    ├── java/com/bikehorn/app/
    │   ├── BikeHornApp.kt         # Application class, notification channels
    │   ├── MainActivity.kt        # Single activity, permissions, service binding
    │   ├── MainViewModel.kt       # Central event routing, state management
    │   ├── navigation/NavGraph.kt
    │   ├── ble/
    │   │   ├── BleManager.kt      # Scan, connect, auto-reconnect, SharedFlow<PuckEvent>
    │   │   ├── BleConnectionService.kt  # Foreground service for background BLE
    │   │   └── PuckJsProtocol.kt  # JSON parsing into sealed PuckEvent class
    │   ├── sound/SoundManager.kt  # SoundPool wrapper, preloads bundled sounds
    │   ├── crash/CrashAlertManager.kt   # Countdown, alarm, SMS with GPS
    │   ├── data/
    │   │   ├── models.kt          # ButtonPattern enum, BundledSound, AppSettings
    │   │   └── PreferencesRepo.kt # Jetpack DataStore persistence
    │   └── ui/
    │       ├── theme/Theme.kt
    │       └── screens/
    │           ├── HomeScreen.kt
    │           ├── SoundAssignmentScreen.kt
    │           ├── CrashAlertScreen.kt
    │           └── SettingsScreen.kt
    └── res/raw/                   # Bundled .ogg sound files (placeholder tones)
```

## Key Design Decisions

- **No BLE library** - uses Android's built-in `BluetoothLeScanner` and `BluetoothGatt` directly
- **SoundPool** over MediaPlayer for low-latency short sound effects
- **Audio routes to Bluetooth speaker automatically** via `USAGE_MEDIA` — Android handles A2DP routing. HomeScreen shows connected BT speaker name via `AudioDeviceCallback`.
- **Foreground service** keeps BLE connection alive when app is backgrounded
- **BLE service deferred until permissions granted** — avoids SecurityException crash on SDK 34
- **DataStore** (not SharedPreferences) for async-safe preference persistence
- **Accelerometer at 12.5Hz** on Puck.js for battery efficiency
- **Crash debounce** of 5s to prevent duplicate alerts from aftershocks

## Android Build

- Target SDK 34, min SDK 26 (Android 8+)
- AGP 9.0.1, Kotlin 2.2.10 with Compose compiler plugin
- Kotlin DSL Gradle with version catalog (`gradle/libs.versions.toml`)
- Compose BOM 2024.02.00 (older — `MenuAnchorType` not available, use `.menuAnchor()` without params)
- Accompanist for runtime permissions

## Firmware Notes

- Flash `firmware/bike_horn.js` to Puck.js via Espruino Web IDE (espruino.com/ide)
- The firmware uses `setWatch` for button events and `Puck.accelOn()` for accelerometer
- LED feedback: green = connected/ready, red = disconnected/crash detected
- Multi-tap detection uses a 500ms window after each short press to accumulate taps

## Sound Files

The `res/raw/` sounds are placeholder sine wave tones generated with ffmpeg. Replace with real sound effects for production. Sound IDs in `models.kt` must match filenames:
- 1=bell_ding, 2=horn_short, 3=horn_loud, 4=alarm, 5=bicycle_bell, 6=air_horn

## Puck.js Hardware Sensors

Available sensors beyond what's currently used:
- **NFC** — planned for tap-to-pair and emergency info tag for bystanders
- **Magnetometer** — potential for turn-by-turn LED navigation
- **Gyroscope** — planned to combine with accelerometer for better crash detection (fewer false positives)
- **Temperature** — potential for ice/freeze warnings
- **IR transmitter** — limited use in silicone case

## Development Notes

- **No real Android phone available yet** — corporate phone can't enable developer mode. Emulator works for UI but BLE requires a real device.
- **Emulators have no Bluetooth hardware** — BLE scanning won't work. Consider adding a mock/demo mode.
- See `TODO.md` for the full roadmap.
- Always add comments to code changes.

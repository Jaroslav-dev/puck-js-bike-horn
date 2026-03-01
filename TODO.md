# Puck.js Bike Horn - TODO

## Pre-Launch (Must Do)

- [ ] Test on real Android phone with Puck.js hardware
- [x] Replace placeholder sine wave sound files with real audio (bell, horn, alarm, etc.)
- [ ] Verify BLE scanning and connection works reliably on real hardware
- [ ] Test crash detection end-to-end (accelerometer → alert → SMS)
- [ ] Test SMS sending with a real emergency contact number
- [ ] Verify foreground service keeps BLE alive when app is backgrounded

## Bug Fixes & Reliability

- [ ] Add error handling for BLE connection failures (show user-friendly messages)
- [ ] Prevent duplicate scan/connect attempts if user taps rapidly
- [ ] Validate phone number format in emergency contact settings
- [ ] Add SMS delivery status feedback to the user
- [ ] Handle edge case: crash alert triggered while phone has no GPS signal

## Testing

- [x] Add unit tests for `PuckJsProtocol` JSON parsing
- [x] Add unit tests for `ButtonPattern` event type mapping
- [ ] Add unit tests for `MainViewModel` event routing (needs Robolectric)
- [ ] Add unit tests for `PreferencesRepo` persistence (needs Robolectric)
- [ ] Add UI tests for CrashAlertScreen countdown flow
- [ ] Add integration test for BLE scan → connect → event flow (with mocks)

## UI / UX Improvements

- [ ] Add a mock/demo mode for testing without real Puck.js hardware
- [ ] Add visual feedback when a sound plays (e.g. animation on HomeScreen)
- [ ] Improve HomeScreen to show battery level of connected Puck.js
- [ ] Add onboarding/first-run tutorial explaining button patterns
- [ ] Add accessibility labels and content descriptions throughout UI
- [ ] Add color-blind-friendly indicators (not just red/green for status)

## Sound System

- [x] Detect connected Bluetooth speaker and show audio output status on HomeScreen
- [x] Support user-uploaded custom sound files (pick from device storage)
- [x] Rename custom sound files from within the app
- [x] Add sound duration preview indicator
- [ ] Add volume control per sound assignment

## Button Patterns

- [x] Replace time-based press patterns (short/long/very-long/repeated) with tap-count system (1 tap, 2 taps, 3 taps, long press) for faster triggering on a bike

## Crash Detection

- [ ] Improve crash detection by combining gyroscope + accelerometer data (rotation + g-force) to reduce false positives from potholes/bumps
- [ ] Add crash event history log (date, time, location, g-force)
- [ ] Add GPS track logging during rides for crash location context
- [ ] Add option to send crash alert via app notification (not just SMS)
- [ ] Consider adding multiple emergency contacts support
- [ ] Add configurable crash debounce time in settings (currently hardcoded 5s)

## Motion Sounds (Puck.js v2 only)

- [x] Detect acceleration and braking via accelerometer delta vs rolling average
- [x] Play configurable sounds on acceleration and braking events
- [x] Guard accelerometer code so firmware works on both Puck.js v1 and v2
- [ ] Tune thresholds after real-world bike testing (currently ACCEL_THRESHOLD = 0.3g)
- [ ] Consider combining gyroscope data to reduce false positives

## NFC Features

- [ ] Tap-to-pair: use NFC to instantly open the app and connect to the Puck.js (skip BLE scanning)
- [ ] Emergency info tag: after a crash is detected, bystanders can tap the Puck.js with their phone to see emergency contact info, medical details, and crash location

## Firmware

- [x] Fix invalid 12.5 Hz accelerometer rate — changed to 10 Hz (valid LIS3DH rates: 1, 10, 25, 50, 100, 200, 400)
- [ ] Add battery level reporting over BLE
- [ ] Add firmware version reporting over BLE
- [ ] Make multi-tap window configurable via BLE command from app
- [ ] Add option to adjust accelerometer sensitivity from app
- [ ] Consider power optimization for longer Puck.js battery life

## Build & Distribution

- [ ] Set up app signing for release builds
- [ ] Create proper app icon (replace placeholder adaptive icon)
- [ ] Add ProGuard/R8 rules for release build minification
- [ ] Prepare Play Store listing (screenshots, description)
- [ ] Set up CI/CD pipeline for automated builds
- [ ] Update Compose BOM from `2024.02.00` to latest stable

## Hardware / Physical Product

- [ ] Design a handlebar mount for the Puck.js device
- [ ] Research IP rating of the Puck.js silicone case — determine if it's sufficient for riding in rain or if additional waterproofing is needed

## Documentation

- [ ] Add user guide with setup instructions
- [ ] Document BLE protocol for third-party integrations
- [ ] Add troubleshooting guide (common BLE issues, firmware flashing)
- [ ] Add contributing guide if open-sourcing

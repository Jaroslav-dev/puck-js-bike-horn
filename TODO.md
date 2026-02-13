# Puck.js Bike Horn - TODO

## Pre-Launch (Must Do)

- [ ] Test on real Android phone with Puck.js hardware
- [ ] Replace placeholder sine wave sound files with real audio (bell, horn, alarm, etc.)
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

- [ ] Add unit tests for `PuckJsProtocol` JSON parsing
- [ ] Add unit tests for `MainViewModel` event routing
- [ ] Add unit tests for `PreferencesRepo` persistence
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
- [ ] Support user-uploaded custom sound files (pick from device storage)
- [ ] Add volume control per sound assignment
- [ ] Add sound duration preview indicator

## Crash Detection

- [ ] Improve crash detection by combining gyroscope + accelerometer data (rotation + g-force) to reduce false positives from potholes/bumps
- [ ] Add crash event history log (date, time, location, g-force)
- [ ] Add GPS track logging during rides for crash location context
- [ ] Add option to send crash alert via app notification (not just SMS)
- [ ] Consider adding multiple emergency contacts support
- [ ] Add configurable crash debounce time in settings (currently hardcoded 5s)

## NFC Features

- [ ] Tap-to-pair: use NFC to instantly open the app and connect to the Puck.js (skip BLE scanning)
- [ ] Emergency info tag: after a crash is detected, bystanders can tap the Puck.js with their phone to see emergency contact info, medical details, and crash location

## Firmware

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

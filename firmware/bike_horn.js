// Bike Horn & Crash Detection Firmware for Puck.js
// Communicates via BLE Nordic UART Service (NUS)

// --- Configuration ---
var CRASH_THRESHOLD = 3.0;     // g-force magnitude to trigger crash
var CRASH_DEBOUNCE_MS = 5000;  // ignore crashes for 5s after one
var LONG_PRESS_MS = 2000;      // 2s for long press
var VERY_LONG_PRESS_MS = 4000; // 4s for very long press
var MULTI_TAP_WINDOW_MS = 500; // wait 500ms after short press for more taps
var MULTI_TAP_COUNT = 3;       // 3+ taps for repeated_press
var ACCEL_RATE = 12.5;         // Hz, battery-efficient

// --- State ---
var pressStart = 0;
var shortPressCount = 0;
var multiTapTimer = null;
var lastCrashTime = 0;
var connected = false;

// --- Utility ---
function send(obj) {
  Bluetooth.println(JSON.stringify(obj));
}

// --- Button Pattern Detection ---
function onButtonPress() {
  pressStart = getTime() * 1000; // ms
}

function onButtonRelease() {
  if (pressStart === 0) return;
  var duration = (getTime() * 1000) - pressStart;
  pressStart = 0;

  if (duration >= VERY_LONG_PRESS_MS) {
    clearMultiTap();
    send({ t: "very_long_press" });
  } else if (duration >= LONG_PRESS_MS) {
    clearMultiTap();
    send({ t: "long_press" });
  } else {
    // Short press - accumulate for multi-tap detection
    shortPressCount++;
    if (multiTapTimer) clearTimeout(multiTapTimer);
    multiTapTimer = setTimeout(function() {
      if (shortPressCount >= MULTI_TAP_COUNT) {
        send({ t: "repeated_press" });
      } else {
        send({ t: "short_press" });
      }
      shortPressCount = 0;
      multiTapTimer = null;
    }, MULTI_TAP_WINDOW_MS);
  }
}

function clearMultiTap() {
  if (multiTapTimer) {
    clearTimeout(multiTapTimer);
    multiTapTimer = null;
  }
  shortPressCount = 0;
}

// --- Accelerometer Crash Detection ---
function onAccelData(accel) {
  var mag = Math.sqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z);
  var now = getTime() * 1000;

  if (mag >= CRASH_THRESHOLD && (now - lastCrashTime) > CRASH_DEBOUNCE_MS) {
    lastCrashTime = now;
    send({ t: "crash", d: Math.round(mag * 10) / 10 });
    // Flash red LED briefly to indicate crash detected
    digitalPulse(LED1, 1, 200);
  }
}

// --- Connection Status LEDs ---
NRF.on('connect', function() {
  connected = true;
  // Blink green LED
  digitalPulse(LED2, 1, 300);
});

NRF.on('disconnect', function() {
  connected = false;
  // Blink red LED
  digitalPulse(LED1, 1, 300);
});

// --- Initialization ---
function init() {
  // Set up button watches
  setWatch(onButtonPress, BTN, { repeat: true, edge: "rising", debounce: 50 });
  setWatch(onButtonRelease, BTN, { repeat: true, edge: "falling", debounce: 50 });

  // Enable accelerometer at low rate for battery efficiency
  Puck.accelOn(ACCEL_RATE);
  Puck.on('accel', onAccelData);

  // Flash green LED to indicate firmware ready
  digitalPulse(LED2, 1, 100);
  setTimeout(function() { digitalPulse(LED2, 1, 100); }, 200);
}

// Start on boot
init();

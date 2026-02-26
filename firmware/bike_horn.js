// Bike Horn & Crash Detection Firmware for Puck.js
// Communicates via BLE Nordic UART Service (NUS)

// --- Configuration ---
var CRASH_THRESHOLD = 3.0;     // g-force magnitude to trigger crash
var CRASH_DEBOUNCE_MS = 5000;  // ignore crashes for 5s after one
var LONG_PRESS_MS = 2000;      // 2s hold for long press
var MULTI_TAP_WINDOW_MS = 500; // ms to wait after a tap before deciding count (1, 2, or 3)
var ACCEL_RATE = 10;           // Hz, battery-efficient (valid LIS3DH rates: 1,10,25,50,100,200,400)
// Motion detection: threshold for delta vs rolling average to fire events
var ACCEL_THRESHOLD = 0.3;     // positive delta (g) to detect acceleration
var BRAKE_THRESHOLD = 0.3;     // negative delta (g) to detect braking
var MOTION_DEBOUNCE_MS = 1500; // minimum ms between motion events

// --- State ---
var pressStart = 0;
var shortPressCount = 0;
var multiTapTimer = null;
var lastCrashTime = 0;
var connected = false;
// Motion detection state
var rollingAvg = 1.0;          // starts at 1g (device at rest)
var lastMotionTime = 0;        // timestamp of last acceleration/braking event

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

  if (duration >= LONG_PRESS_MS) {
    // Long hold: fire immediately and cancel any pending tap sequence
    clearMultiTap();
    send({ t: "long_press" });
  } else {
    // Short tap: accumulate count (capped at 3), then wait for more taps
    shortPressCount = Math.min(shortPressCount + 1, 3);
    if (multiTapTimer) clearTimeout(multiTapTimer);
    multiTapTimer = setTimeout(function() {
      if (shortPressCount === 1) send({ t: "single_press" });
      else if (shortPressCount === 2) send({ t: "double_press" });
      else send({ t: "triple_press" });
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

// --- Accelerometer Crash & Motion Detection ---
function onAccelData(accel) {
  var mag = Math.sqrt(accel.x * accel.x + accel.y * accel.y + accel.z * accel.z);
  var now = getTime() * 1000;

  // Crash: large spike in any direction
  if (mag >= CRASH_THRESHOLD && (now - lastCrashTime) > CRASH_DEBOUNCE_MS) {
    lastCrashTime = now;
    send({ t: "crash", d: Math.round(mag * 10) / 10 });
    // Flash red LED briefly to indicate crash detected
    digitalPulse(LED1, 1, 200);
  }

  // Motion detection: compare current magnitude against rolling average.
  // Only fires when outside the crash debounce window and motion debounce window.
  var delta = mag - rollingAvg;
  if ((now - lastCrashTime) > CRASH_DEBOUNCE_MS &&
      (now - lastMotionTime) > MOTION_DEBOUNCE_MS) {
    if (delta > ACCEL_THRESHOLD) {
      lastMotionTime = now;
      send({ t: "acceleration" });
    } else if (delta < -BRAKE_THRESHOLD) {
      lastMotionTime = now;
      send({ t: "braking" });
    }
  }

  // Update rolling average with exponential smoothing (alpha=0.1).
  // Skip updating during a crash spike so the baseline recovers cleanly.
  if (mag < CRASH_THRESHOLD) {
    rollingAvg = rollingAvg * 0.9 + mag * 0.1;
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

  // Enable accelerometer only on Puck.js v2 (v1 has no accelerometer hardware)
  if (typeof Puck.accelOn === 'function') {
    Puck.accelOn(ACCEL_RATE);
    Puck.on('accel', onAccelData);
  }

  // Flash green LED to indicate firmware ready
  digitalPulse(LED2, 1, 100);
  setTimeout(function() { digitalPulse(LED2, 1, 100); }, 200);
}

// Start on boot
init();

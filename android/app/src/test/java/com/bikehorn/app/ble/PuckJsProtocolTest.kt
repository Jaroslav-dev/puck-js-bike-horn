package com.bikehorn.app.ble

// A unit test verifies one small piece of logic in isolation.
// Each @Test function is independent — it sets up its own input, calls the code under
// test, and asserts (checks) that the output matches what we expect.
// Run all tests with: ./gradlew test
// Results appear in app/build/reports/tests/testDebugUnitTest/index.html

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuckJsProtocolTest {

    // -------------------------------------------------------------------------
    // Button press events
    // -------------------------------------------------------------------------

    @Test
    fun `single press is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"single_press"}""")
        assertEquals(PuckEvent.SinglePress, result)
    }

    @Test
    fun `double press is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"double_press"}""")
        assertEquals(PuckEvent.DoublePress, result)
    }

    @Test
    fun `triple press is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"triple_press"}""")
        assertEquals(PuckEvent.TriplePress, result)
    }

    @Test
    fun `long press is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"long_press"}""")
        assertEquals(PuckEvent.LongPress, result)
    }

    // -------------------------------------------------------------------------
    // Crash event — carries a magnitude value
    // -------------------------------------------------------------------------

    @Test
    fun `crash with magnitude is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"crash","d":4.2}""")
        assertEquals(PuckEvent.Crash(4.2f), result)
    }

    @Test
    fun `crash without magnitude field defaults to 0`() {
        // The firmware always sends "d", but we handle the missing case gracefully
        val result = PuckJsProtocol.parse("""{"t":"crash"}""")
        assertEquals(PuckEvent.Crash(0.0f), result)
    }

    @Test
    fun `crash magnitude of zero is preserved`() {
        val result = PuckJsProtocol.parse("""{"t":"crash","d":0}""")
        assertEquals(PuckEvent.Crash(0.0f), result)
    }

    // -------------------------------------------------------------------------
    // Motion events (Puck.js v2 only)
    // -------------------------------------------------------------------------

    @Test
    fun `acceleration is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"acceleration"}""")
        assertEquals(PuckEvent.Acceleration, result)
    }

    @Test
    fun `braking is parsed correctly`() {
        val result = PuckJsProtocol.parse("""{"t":"braking"}""")
        assertEquals(PuckEvent.Braking, result)
    }

    // -------------------------------------------------------------------------
    // Error / edge cases — the parser must never crash the app
    // -------------------------------------------------------------------------

    @Test
    fun `unknown event type returns null`() {
        val result = PuckJsProtocol.parse("""{"t":"unknown_event"}""")
        assertNull(result)
    }

    @Test
    fun `old short_press event type returns null`() {
        // Firmware was updated — old event names should be silently ignored
        val result = PuckJsProtocol.parse("""{"t":"short_press"}""")
        assertNull(result)
    }

    @Test
    fun `missing t field returns null`() {
        val result = PuckJsProtocol.parse("""{"x":"some_value"}""")
        assertNull(result)
    }

    @Test
    fun `malformed JSON returns null`() {
        val result = PuckJsProtocol.parse("not json at all")
        assertNull(result)
    }

    @Test
    fun `empty string returns null`() {
        val result = PuckJsProtocol.parse("")
        assertNull(result)
    }

    @Test
    fun `whitespace around JSON is trimmed before parsing`() {
        // BLE messages sometimes arrive with trailing newline or spaces
        val result = PuckJsProtocol.parse("  {\"t\":\"single_press\"}  ")
        assertEquals(PuckEvent.SinglePress, result)
    }

    @Test
    fun `extra fields in the JSON object are ignored`() {
        // Firmware may add extra fields in future without breaking the app
        val result = PuckJsProtocol.parse("""{"t":"long_press","fw":"2.1"}""")
        assertEquals(PuckEvent.LongPress, result)
    }
}

package com.bikehorn.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ButtonPatternTest {

    // -------------------------------------------------------------------------
    // Known event types must map to the correct enum value
    // -------------------------------------------------------------------------

    @Test
    fun `single_press maps to SINGLE_PRESS`() {
        assertEquals(ButtonPattern.SINGLE_PRESS, ButtonPattern.fromEventType("single_press"))
    }

    @Test
    fun `double_press maps to DOUBLE_PRESS`() {
        assertEquals(ButtonPattern.DOUBLE_PRESS, ButtonPattern.fromEventType("double_press"))
    }

    @Test
    fun `triple_press maps to TRIPLE_PRESS`() {
        assertEquals(ButtonPattern.TRIPLE_PRESS, ButtonPattern.fromEventType("triple_press"))
    }

    @Test
    fun `long_press maps to LONG_PRESS`() {
        assertEquals(ButtonPattern.LONG_PRESS, ButtonPattern.fromEventType("long_press"))
    }

    // -------------------------------------------------------------------------
    // All four patterns have human-readable labels used in the UI
    // -------------------------------------------------------------------------

    @Test
    fun `all patterns have non-blank labels`() {
        ButtonPattern.entries.forEach { pattern ->
            assert(pattern.label.isNotBlank()) {
                "Pattern ${pattern.name} has a blank label"
            }
        }
    }

    // -------------------------------------------------------------------------
    // Unknown / removed event types must return null, not crash
    // -------------------------------------------------------------------------

    @Test
    fun `unknown event type returns null`() {
        assertNull(ButtonPattern.fromEventType("unknown"))
    }

    @Test
    fun `empty string returns null`() {
        assertNull(ButtonPattern.fromEventType(""))
    }

    @Test
    fun `old short_press event type returns null`() {
        // Removed when we switched to tap-count system — must not match anything
        assertNull(ButtonPattern.fromEventType("short_press"))
    }

    @Test
    fun `old very_long_press event type returns null`() {
        assertNull(ButtonPattern.fromEventType("very_long_press"))
    }

    @Test
    fun `old repeated_press event type returns null`() {
        assertNull(ButtonPattern.fromEventType("repeated_press"))
    }
}

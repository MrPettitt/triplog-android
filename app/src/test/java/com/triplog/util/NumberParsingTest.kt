package com.triplog.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParsingTest {

    @Test
    fun decimal_acceptsDotAndComma() {
        assertEquals(12.5, parsePositiveDecimal("12.5")!!, 0.0)
        assertEquals(12.5, parsePositiveDecimal("12,5")!!, 0.0)
        assertEquals(7.0, parsePositiveDecimal(" 7 ")!!, 0.0)
    }

    @Test
    fun decimal_rejectsEmptyZeroNegativeAndGarbage() {
        for (bad in listOf("", "  ", "0", "0.0", "-1", "-0,5", "abc", "1.2.3", "1,234.5", "NaN", "Infinity")) {
            assertNull("'$bad' should be rejected", parsePositiveDecimal(bad))
        }
    }

    @Test
    fun int_acceptsWholeNumbersFromZero() {
        assertEquals(0, parseNonNegativeInt("0"))
        assertEquals(84320, parseNonNegativeInt(" 84320 "))
    }

    @Test
    fun int_rejectsNegativeDecimalAndGarbage() {
        for (bad in listOf("", "-1", "12.5", "12,5", "abc", "99999999999")) {
            assertNull("'$bad' should be rejected", parseNonNegativeInt(bad))
        }
    }
}

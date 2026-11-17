package com.triplog.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormattersTest {

    @Test
    fun formatKm_usesOneDecimalAndGrouping() {
        assertEquals("0.0", formatKm(0.0, Locale.US))
        assertEquals("12.3", formatKm(12.34, Locale.US))
        assertEquals("1,234.6", formatKm(1234.56, Locale.US))
    }

    @Test
    fun formatRands_usesRandPrefixAndTwoDecimals() {
        assertEquals("R 0.00", formatRands(0.0, Locale.US))
        assertEquals("R 812.40", formatRands(812.4, Locale.US))
        assertEquals("R 12,345.00", formatRands(12345.0, Locale.US))
    }

    @Test
    fun formatters_followTheGivenLocale() {
        assertEquals("1.234,6", formatKm(1234.56, Locale.GERMANY))
    }
}

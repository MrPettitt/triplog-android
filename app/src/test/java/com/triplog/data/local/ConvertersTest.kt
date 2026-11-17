package com.triplog.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun localDateTime_roundTrips() {
        val value = LocalDateTime.of(2026, 3, 14, 9, 26, 53)
        val text = converters.toLocalDateTime(value)
        assertEquals("2026-03-14T09:26:53", text)
        assertEquals(value, converters.fromLocalDateTime(text))
    }

    @Test
    fun localDate_roundTrips() {
        val value = LocalDate.of(2026, 12, 1)
        val text = converters.toLocalDate(value)
        assertEquals("2026-12-01", text)
        assertEquals(value, converters.fromLocalDate(text))
    }

    @Test
    fun nulls_stayNull() {
        assertNull(converters.toLocalDateTime(null))
        assertNull(converters.fromLocalDateTime(null))
        assertNull(converters.toLocalDate(null))
        assertNull(converters.fromLocalDate(null))
    }
}

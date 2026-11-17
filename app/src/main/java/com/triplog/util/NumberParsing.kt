package com.triplog.util

/**
 * Parses a decimal typed by the user. Accepts a comma as the decimal separator (common on
 * South African and European keyboards) and rejects anything that is not a finite number
 * greater than zero.
 */
fun parsePositiveDecimal(input: String): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return value.takeIf { it.isFinite() && it > 0.0 }
}

/** Parses a whole number that is zero or greater, such as an odometer reading. */
fun parseNonNegativeInt(input: String): Int? =
    input.trim().toIntOrNull()?.takeIf { it >= 0 }

package com.triplog.util

import java.util.Locale

fun formatKm(km: Double, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "%,.1f", km)

fun formatRands(amount: Double, locale: Locale = Locale.getDefault()): String =
    String.format(locale, "R %,.2f", amount)

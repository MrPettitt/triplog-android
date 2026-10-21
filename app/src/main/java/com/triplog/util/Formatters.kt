package com.triplog.util

import java.util.Locale

fun formatKm(km: Double): String = String.format(Locale.US, "%,.1f", km)

fun formatRands(amount: Double): String = String.format(Locale.US, "R %,.2f", amount)

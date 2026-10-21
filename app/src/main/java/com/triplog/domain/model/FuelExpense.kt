package com.triplog.domain.model

import java.time.LocalDate

data class FuelExpense(
    val id: Long = 0,
    val date: LocalDate,
    val litres: Double,
    val amountRands: Double,
    val odometerKm: Int,
    val fuelStation: String,
    val receiptImagePath: String? = null,
    val isSynced: Boolean = false,
    val remoteId: String? = null,
    val createdAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)

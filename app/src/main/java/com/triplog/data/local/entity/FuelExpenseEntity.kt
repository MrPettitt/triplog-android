package com.triplog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triplog.domain.model.FuelExpense
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "fuel_expenses")
data class FuelExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: LocalDate,
    val litres: Double,
    val amountRands: Double,
    val odometerKm: Int,
    val fuelStation: String,
    val receiptImagePath: String?,
    val createdAt: LocalDateTime
) {
    fun toDomain(): FuelExpense = FuelExpense(
        id = id,
        date = date,
        litres = litres,
        amountRands = amountRands,
        odometerKm = odometerKm,
        fuelStation = fuelStation,
        receiptImagePath = receiptImagePath,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(expense: FuelExpense): FuelExpenseEntity = FuelExpenseEntity(
            id = expense.id,
            date = expense.date,
            litres = expense.litres,
            amountRands = expense.amountRands,
            odometerKm = expense.odometerKm,
            fuelStation = expense.fuelStation,
            receiptImagePath = expense.receiptImagePath,
            createdAt = expense.createdAt
        )
    }
}

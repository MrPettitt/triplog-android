package com.triplog.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.triplog.domain.model.FuelExpense
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "fuel_expenses")
data class FuelExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(defaultValue = "")
    val clientId: String,
    val date: LocalDate,
    val litres: Double,
    val amountRands: Double,
    val odometerKm: Int,
    val fuelStation: String,
    val receiptImagePath: String?,
    val isSynced: Boolean,
    val remoteId: String?,
    val createdAt: LocalDateTime,
    /** Bumped on every local change so a finished upload never marks newer edits as synced. */
    @ColumnInfo(defaultValue = "0")
    val revision: Int = 0,
    /** Tombstone: hidden from the UI, removed locally once the server has deleted it. */
    @ColumnInfo(defaultValue = "0")
    val isDeleted: Boolean = false,
    /** True once the receipt photo has been uploaded for the current file. */
    @ColumnInfo(defaultValue = "0")
    val receiptUploaded: Boolean = false
) {
    fun toDomain(): FuelExpense = FuelExpense(
        id = id,
        clientId = clientId,
        date = date,
        litres = litres,
        amountRands = amountRands,
        odometerKm = odometerKm,
        fuelStation = fuelStation,
        receiptImagePath = receiptImagePath,
        isSynced = isSynced,
        remoteId = remoteId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(expense: FuelExpense): FuelExpenseEntity = FuelExpenseEntity(
            id = expense.id,
            clientId = expense.clientId,
            date = expense.date,
            litres = expense.litres,
            amountRands = expense.amountRands,
            odometerKm = expense.odometerKm,
            fuelStation = expense.fuelStation,
            receiptImagePath = expense.receiptImagePath,
            isSynced = expense.isSynced,
            remoteId = expense.remoteId,
            createdAt = expense.createdAt
        )
    }
}

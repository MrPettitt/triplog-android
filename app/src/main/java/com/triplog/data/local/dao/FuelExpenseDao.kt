package com.triplog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.triplog.data.local.entity.FuelExpenseEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FuelExpenseDao {

    @Query("SELECT * FROM fuel_expenses WHERE isDeleted = 0 ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<FuelExpenseEntity>>

    @Query("SELECT * FROM fuel_expenses WHERE id = :id AND isDeleted = 0")
    suspend fun getExpenseById(id: Long): FuelExpenseEntity?

    /** Everything that still needs to reach the server, including deletions. */
    @Query("SELECT * FROM fuel_expenses WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedExpenses(): List<FuelExpenseEntity>

    @Query("SELECT SUM(amountRands) FROM fuel_expenses WHERE isDeleted = 0")
    fun getTotalExpenseRands(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM fuel_expenses WHERE isSynced = 0")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FuelExpenseEntity): Long

    /** Targeted update so a concurrent sync that just stored remoteId is not overwritten. */
    @Query(
        "UPDATE fuel_expenses SET date = :date, litres = :litres, amountRands = :amountRands, " +
            "odometerKm = :odometerKm, fuelStation = :fuelStation, receiptImagePath = :receiptImagePath, " +
            "receiptUploaded = :receiptUploaded, isSynced = 0, revision = revision + 1 " +
            "WHERE id = :id AND isDeleted = 0"
    )
    suspend fun updateDetails(
        id: Long,
        date: LocalDate,
        litres: Double,
        amountRands: Double,
        odometerKm: Int,
        fuelStation: String,
        receiptImagePath: String?,
        receiptUploaded: Boolean
    )

    @Query("UPDATE fuel_expenses SET isDeleted = 1, isSynced = 0, revision = revision + 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    @Query("DELETE FROM fuel_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE fuel_expenses SET remoteId = :remoteId WHERE id = :localId")
    suspend fun setRemoteId(localId: Long, remoteId: String)

    @Query("UPDATE fuel_expenses SET receiptUploaded = 1 WHERE id = :localId AND revision = :revision")
    suspend fun markReceiptUploaded(localId: Long, revision: Int)

    /** Only succeeds if nothing changed locally since [revision] was read. */
    @Query("UPDATE fuel_expenses SET isSynced = 1, remoteId = :remoteId WHERE id = :localId AND revision = :revision")
    suspend fun markAsSynced(localId: Long, remoteId: String, revision: Int)
}

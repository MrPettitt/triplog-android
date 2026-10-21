package com.triplog.data.local.dao

import androidx.room.*
import com.triplog.data.local.entity.FuelExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelExpenseDao {

    @Query("SELECT * FROM fuel_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<FuelExpenseEntity>>

    @Query("SELECT * FROM fuel_expenses WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedExpenses(): List<FuelExpenseEntity>

    @Query("SELECT SUM(amountRands) FROM fuel_expenses")
    fun getTotalExpenseRands(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM fuel_expenses WHERE isSynced = 0")
    fun getPendingSyncCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FuelExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: FuelExpenseEntity)

    @Query("UPDATE fuel_expenses SET isSynced = 1, remoteId = :remoteId WHERE id = :localId")
    suspend fun markAsSynced(localId: Long, remoteId: String)

    @Delete
    suspend fun deleteExpense(expense: FuelExpenseEntity)
}

package com.triplog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.triplog.data.local.entity.FuelExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelExpenseDao {

    @Query("SELECT * FROM fuel_expenses ORDER BY date DESC, createdAt DESC")
    fun getAllExpenses(): Flow<List<FuelExpenseEntity>>

    @Query("SELECT * FROM fuel_expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): FuelExpenseEntity?

    @Query("SELECT SUM(amountRands) FROM fuel_expenses")
    fun getTotalExpenseRands(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: FuelExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: FuelExpenseEntity)

    @Query("DELETE FROM fuel_expenses WHERE id = :id")
    suspend fun deleteById(id: Long)
}

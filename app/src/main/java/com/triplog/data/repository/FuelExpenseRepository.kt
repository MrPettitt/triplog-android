package com.triplog.data.repository

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.domain.model.FuelExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelExpenseRepository @Inject constructor(
    private val expenseDao: FuelExpenseDao
) {
    val allExpenses: Flow<List<FuelExpense>> = expenseDao.getAllExpenses().map { entities ->
        entities.map { it.toDomain() }
    }

    val totalExpenseRands: Flow<Double?> = expenseDao.getTotalExpenseRands()

    suspend fun getExpense(id: Long): FuelExpense? = expenseDao.getExpenseById(id)?.toDomain()

    suspend fun saveExpense(expense: FuelExpense): Long =
        expenseDao.insertExpense(FuelExpenseEntity.fromDomain(expense))

    /**
     * Replaces the stored expense with the same id. Does nothing if it no longer exists. A receipt
     * photo that was replaced is deleted from storage.
     */
    suspend fun updateExpense(expense: FuelExpense) {
        val existing = expenseDao.getExpenseById(expense.id) ?: return
        expenseDao.updateExpense(FuelExpenseEntity.fromDomain(expense).copy(createdAt = existing.createdAt))
        if (existing.receiptImagePath != null && existing.receiptImagePath != expense.receiptImagePath) {
            deleteReceiptFile(existing.receiptImagePath)
        }
    }

    /** Deletes the expense and its receipt photo. */
    suspend fun deleteExpense(id: Long) {
        val existing = expenseDao.getExpenseById(id) ?: return
        expenseDao.deleteById(id)
        existing.receiptImagePath?.let(::deleteReceiptFile)
    }

    private fun deleteReceiptFile(path: String) {
        runCatching { File(path).delete() }
    }
}

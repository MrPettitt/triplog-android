package com.triplog.data.repository

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateExpenseRequest
import com.triplog.domain.model.FuelExpense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelExpenseRepository @Inject constructor(
    private val expenseDao: FuelExpenseDao,
    private val apiService: ApiService
) {
    val allExpenses: Flow<List<FuelExpense>> = expenseDao.getAllExpenses().map { entities ->
        entities.map { it.toDomain() }
    }

    val totalExpenseRands: Flow<Double?> = expenseDao.getTotalExpenseRands()
    val pendingSyncCount: Flow<Int> = expenseDao.getPendingSyncCount()

    suspend fun saveExpense(expense: FuelExpense): Long {
        val entity = FuelExpenseEntity.fromDomain(expense)
        return expenseDao.insertExpense(entity)
    }

    suspend fun syncPendingExpenses() {
        val unsynced = expenseDao.getUnsyncedExpenses()

        for (entity in unsynced) {
            try {
                val request = CreateExpenseRequest(
                    date = entity.date.toString(),
                    litres = entity.litres,
                    amountRands = entity.amountRands,
                    odometerKm = entity.odometerKm,
                    fuelStation = entity.fuelStation
                )
                val response = apiService.createExpense(request)
                if (response.isSuccessful) {
                    val remoteId = response.body()?.data?.id ?: continue
                    expenseDao.markAsSynced(entity.id, remoteId)

                    // Upload receipt image if present
                    entity.receiptImagePath?.let { path ->
                        uploadReceipt(remoteId, path)
                    }
                }
            } catch (e: Exception) {
                // Network error - will retry on next sync
            }
        }
    }

    private suspend fun uploadReceipt(remoteId: String, imagePath: String) {
        try {
            val file = File(imagePath)
            if (!file.exists()) return

            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("receipt", file.name, requestFile)
            val expenseIdPart = remoteId.toRequestBody("text/plain".toMediaTypeOrNull())

            apiService.uploadReceipt(remoteId, body, expenseIdPart)
        } catch (e: Exception) {
            // Upload failed - receipt will remain local
        }
    }
}

package com.triplog.data.repository

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateExpenseRequest
import com.triplog.domain.model.FuelExpense
import com.triplog.sync.SyncOutcome
import com.triplog.sync.SyncResult
import com.triplog.sync.SyncScheduler
import com.triplog.sync.outcomeForHttpCode
import com.triplog.sync.toResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelExpenseRepository @Inject constructor(
    private val expenseDao: FuelExpenseDao,
    private val apiService: ApiService,
    private val syncScheduler: SyncScheduler
) {
    private val syncMutex = Mutex()

    val allExpenses: Flow<List<FuelExpense>> = expenseDao.getAllExpenses().map { entities ->
        entities.map { it.toDomain() }
    }

    val totalExpenseRands: Flow<Double?> = expenseDao.getTotalExpenseRands()
    val pendingSyncCount: Flow<Int> = expenseDao.getPendingSyncCount()

    suspend fun saveExpense(expense: FuelExpense): Long {
        val id = expenseDao.insertExpense(FuelExpenseEntity.fromDomain(expense))
        syncScheduler.requestSync()
        return id
    }

    /**
     * Uploads every unsynced expense. An expense only counts as synced once both the record and
     * its receipt photo are on the server. If the record was created but the receipt failed, the
     * remote id is kept and only the receipt is retried on the next pass.
     */
    suspend fun syncPendingExpenses(): SyncResult = syncMutex.withLock {
        var result = SyncResult()
        for (entity in expenseDao.getUnsyncedExpenses()) {
            result += syncExpense(entity).toResult()
        }
        result
    }

    private suspend fun syncExpense(entity: FuelExpenseEntity): SyncOutcome {
        return try {
            syncExpenseUnsafe(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SyncOutcome.Retryable
        } catch (e: Exception) {
            SyncOutcome.Rejected
        }
    }

    private suspend fun syncExpenseUnsafe(entity: FuelExpenseEntity): SyncOutcome {
        var remoteId = entity.remoteId
        if (remoteId == null) {
            val response = apiService.createExpense(entity.clientId, entity.toRequest())
            val outcome = outcomeForHttpCode(response.code())
            if (outcome != SyncOutcome.Success) return outcome
            remoteId = response.body()?.data?.id ?: return SyncOutcome.Rejected
            expenseDao.setRemoteId(entity.id, remoteId)
        }

        val receiptOutcome = uploadReceipt(remoteId, entity.receiptImagePath)
        if (receiptOutcome == SyncOutcome.Success) {
            expenseDao.markAsSynced(entity.id, remoteId)
        }
        return receiptOutcome
    }

    /** No receipt, or a receipt file that no longer exists, counts as done: there is nothing to send. */
    private suspend fun uploadReceipt(remoteId: String, imagePath: String?): SyncOutcome {
        if (imagePath == null) return SyncOutcome.Success
        val file = File(imagePath)
        if (!file.exists()) return SyncOutcome.Success

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("receipt", file.name, requestFile)
        val expenseIdPart = remoteId.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = apiService.uploadReceipt(remoteId, body, expenseIdPart)
        return outcomeForHttpCode(response.code())
    }

    private fun FuelExpenseEntity.toRequest() = CreateExpenseRequest(
        clientId = clientId,
        date = date.toString(),
        litres = litres,
        amountRands = amountRands,
        odometerKm = odometerKm,
        fuelStation = fuelStation
    )
}

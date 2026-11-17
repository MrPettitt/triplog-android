package com.triplog.data.repository

import com.triplog.data.local.dao.FuelExpenseDao
import com.triplog.data.local.entity.FuelExpenseEntity
import com.triplog.data.remote.ApiService
import com.triplog.data.remote.dto.CreateExpenseRequest
import com.triplog.domain.model.FuelExpense
import com.triplog.sync.SyncOutcome
import com.triplog.sync.SyncResult
import com.triplog.sync.SyncScheduler
import com.triplog.sync.outcomeForDeleteCode
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

    suspend fun getExpense(id: Long): FuelExpense? = expenseDao.getExpenseById(id)?.toDomain()

    suspend fun saveExpense(expense: FuelExpense): Long {
        val id = expenseDao.insertExpense(FuelExpenseEntity.fromDomain(expense))
        syncScheduler.requestSync()
        return id
    }

    /**
     * Edits an expense. It goes back to Pending until the change is uploaded. A different receipt
     * photo is uploaded again; an unchanged one is not.
     */
    suspend fun updateExpense(expense: FuelExpense) {
        val existing = expenseDao.getExpenseById(expense.id) ?: return
        val receiptChanged = existing.receiptImagePath != expense.receiptImagePath
        expenseDao.updateDetails(
            id = expense.id,
            date = expense.date,
            litres = expense.litres,
            amountRands = expense.amountRands,
            odometerKm = expense.odometerKm,
            fuelStation = expense.fuelStation,
            receiptImagePath = expense.receiptImagePath,
            receiptUploaded = existing.receiptUploaded && !receiptChanged
        )
        syncScheduler.requestSync()
    }

    /** Hides the expense at once; the server copy (if any) is deleted on the next sync. */
    suspend fun deleteExpense(id: Long) {
        expenseDao.markDeleted(id)
        syncScheduler.requestSync()
    }

    /**
     * Pushes every pending create, edit and delete. An expense only counts as synced once both
     * the record and its receipt photo are on the server. If the record was created but the
     * receipt failed, the remote id is kept and only the receipt is retried on the next pass.
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
            if (entity.isDeleted) deleteRemote(entity) else pushExpense(entity)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SyncOutcome.Retryable
        } catch (e: Exception) {
            SyncOutcome.Rejected
        }
    }

    private suspend fun pushExpense(entity: FuelExpenseEntity): SyncOutcome {
        var remoteId = entity.remoteId
        if (remoteId == null) {
            val response = apiService.createExpense(entity.clientId, entity.toRequest())
            val outcome = outcomeForHttpCode(response.code())
            if (outcome != SyncOutcome.Success) return outcome
            remoteId = response.body()?.data?.id ?: return SyncOutcome.Rejected
            // Always remember the server id, even if the user edits the expense meanwhile.
            expenseDao.setRemoteId(entity.id, remoteId)
        } else {
            val response = apiService.updateExpense(remoteId, entity.toRequest())
            val outcome = outcomeForHttpCode(response.code())
            if (outcome != SyncOutcome.Success) return outcome
        }

        val receiptOutcome = uploadReceiptIfNeeded(entity, remoteId)
        if (receiptOutcome == SyncOutcome.Success) {
            expenseDao.markAsSynced(entity.id, remoteId, entity.revision)
        }
        return receiptOutcome
    }

    private suspend fun deleteRemote(entity: FuelExpenseEntity): SyncOutcome {
        val remoteId = entity.remoteId
        if (remoteId == null) {
            // Never reached the server, so there is nothing to delete there.
            expenseDao.deleteById(entity.id)
            return SyncOutcome.Success
        }
        val outcome = outcomeForDeleteCode(apiService.deleteExpense(remoteId).code())
        if (outcome == SyncOutcome.Success) expenseDao.deleteById(entity.id)
        return outcome
    }

    /** No receipt, or a receipt file that no longer exists, counts as done: there is nothing to send. */
    private suspend fun uploadReceiptIfNeeded(entity: FuelExpenseEntity, remoteId: String): SyncOutcome {
        val path = entity.receiptImagePath
        if (path == null || entity.receiptUploaded) return SyncOutcome.Success
        val file = File(path)
        if (!file.exists()) return SyncOutcome.Success

        val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("receipt", file.name, requestFile)
        val expenseIdPart = remoteId.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = apiService.uploadReceipt(remoteId, body, expenseIdPart)
        val outcome = outcomeForHttpCode(response.code())
        if (outcome == SyncOutcome.Success) expenseDao.markReceiptUploaded(entity.id, entity.revision)
        return outcome
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

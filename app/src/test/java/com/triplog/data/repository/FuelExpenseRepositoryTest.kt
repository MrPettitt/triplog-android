package com.triplog.data.repository

import com.triplog.domain.model.FuelExpense
import com.triplog.sync.SyncResult
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeFuelExpenseDao
import com.triplog.testutil.FakeSyncScheduler
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

class FuelExpenseRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dao = FakeFuelExpenseDao()
    private val api = FakeApiService()
    private val scheduler = FakeSyncScheduler()
    private val repository = FuelExpenseRepository(dao, api, scheduler)

    private fun expense(receiptPath: String? = null) = FuelExpense(
        date = LocalDate.of(2026, 1, 1),
        litres = 40.0,
        amountRands = 900.5,
        odometerKm = 84320,
        fuelStation = "Engen Sandton",
        receiptImagePath = receiptPath
    )

    private fun receiptFile() = tempFolder.newFile("receipt.jpg").apply { writeText("jpeg") }

    @Test
    fun saveExpense_storesUnsyncedExpenseAndRequestsSync() = runTest {
        repository.saveExpense(expense())

        assertFalse(dao.expenses.value.single().isSynced)
        assertEquals(1, scheduler.requests)
    }

    @Test
    fun sync_marksExpenseSyncedAndSendsExpectedRequest() = runTest {
        repository.saveExpense(expense())

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(synced = 1), result)
        val stored = dao.expenses.value.single()
        assertTrue(stored.isSynced)
        assertEquals("remote-expense", stored.remoteId)

        val request = api.expenseRequests.single()
        assertEquals(stored.clientId, request.clientId)
        assertEquals(listOf(stored.clientId), api.idempotencyKeys)
        assertEquals("2026-01-01", request.date)
        assertEquals(900.5, request.amountRands, 0.0)
        assertEquals(84320, request.odometerKm)
        assertEquals("Engen Sandton", request.fuelStation)
    }

    @Test
    fun sync_uploadsReceiptWhenFileExists() = runTest {
        repository.saveExpense(expense(receiptPath = receiptFile().absolutePath))

        repository.syncPendingExpenses()

        assertEquals(listOf("remote-expense"), api.receiptUploads)
        assertTrue(dao.expenses.value.single().isSynced)
    }

    @Test
    fun sync_failedReceiptUpload_keepsExpensePendingButRemembersRemoteId() = runTest {
        api.receiptBehaviour = { FakeApiService.serverError() }
        repository.saveExpense(expense(receiptPath = receiptFile().absolutePath))

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(retryable = 1), result)
        val stored = dao.expenses.value.single()
        assertFalse(stored.isSynced)
        assertEquals("remote-expense", stored.remoteId)
    }

    @Test
    fun sync_retriesOnlyTheReceiptAfterAFailedUpload() = runTest {
        api.receiptBehaviour = { throw IOException("dropped") }
        repository.saveExpense(expense(receiptPath = receiptFile().absolutePath))
        repository.syncPendingExpenses()

        api.receiptBehaviour = { id ->
            retrofit2.Response.success(
                com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.expenseDto(id), null)
            )
        }
        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(synced = 1), result)
        assertEquals("Expense must not be created twice", 1, api.expenseRequests.size)
        assertEquals(2, api.receiptUploads.size)
        assertTrue(dao.expenses.value.single().isSynced)
    }

    @Test
    fun sync_skipsReceiptUploadWhenFileIsMissing() = runTest {
        repository.saveExpense(expense(receiptPath = "/does/not/exist.jpg"))

        repository.syncPendingExpenses()

        assertTrue(api.receiptUploads.isEmpty())
        assertTrue(dao.expenses.value.single().isSynced)
    }

    @Test
    fun sync_serverError_leavesExpensePendingWithoutRemoteId() = runTest {
        api.createExpenseBehaviour = { FakeApiService.serverError() }
        repository.saveExpense(expense())

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(retryable = 1), result)
        val stored = dao.expenses.value.single()
        assertFalse(stored.isSynced)
        assertNull(stored.remoteId)
        assertTrue(api.receiptUploads.isEmpty())
    }

    @Test
    fun sync_clientError_isRejectedNotRetried() = runTest {
        api.createExpenseBehaviour = { FakeApiService.httpError(400) }
        repository.saveExpense(expense())

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(rejected = 1), result)
        assertFalse(result.shouldRetry)
    }

    @Test
    fun sync_networkException_isRetryableAndLeavesExpensePending() = runTest {
        api.createExpenseBehaviour = { throw IOException("offline") }
        repository.saveExpense(expense())

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(retryable = 1), result)
        assertFalse(dao.expenses.value.single().isSynced)
    }
}

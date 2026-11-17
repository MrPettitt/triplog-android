package com.triplog.data.repository

import com.triplog.domain.model.FuelExpense
import com.triplog.sync.SyncResult
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeFuelExpenseDao
import com.triplog.testutil.FakeSyncScheduler
import kotlinx.coroutines.flow.first
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

class ExpenseEditDeleteTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dao = FakeFuelExpenseDao()
    private val api = FakeApiService()
    private val scheduler = FakeSyncScheduler()
    private val repository = FuelExpenseRepository(dao, api, scheduler)

    private fun expense(receiptPath: String? = null) = FuelExpense(
        date = LocalDate.of(2026, 1, 1),
        litres = 40.0,
        amountRands = 900.0,
        odometerKm = 1000,
        fuelStation = "Engen",
        receiptImagePath = receiptPath
    )

    private fun receipt(name: String) = tempFolder.newFile(name).apply { writeText("jpeg") }.absolutePath

    private suspend fun savedAndSynced(receiptPath: String? = null): FuelExpense {
        val id = repository.saveExpense(expense(receiptPath))
        repository.syncPendingExpenses()
        return repository.getExpense(id)!!
    }

    // --- edit ---

    @Test
    fun update_changesFieldsAndMarksSyncedExpensePending() = runTest {
        val synced = savedAndSynced()

        repository.updateExpense(synced.copy(litres = 12.5, fuelStation = "Shell"))

        val edited = repository.getExpense(synced.id)!!
        assertEquals(12.5, edited.litres, 0.0)
        assertEquals("Shell", edited.fuelStation)
        assertFalse(edited.isSynced)
        assertEquals("remote-expense", edited.remoteId)
    }

    @Test
    fun sync_sendsEditedExpenseAsUpdateNotCreate() = runTest {
        val synced = savedAndSynced()
        repository.updateExpense(synced.copy(amountRands = 1234.5))

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(synced = 1), result)
        assertEquals(1, api.expenseRequests.size)
        val (remoteId, body) = api.expenseUpdates.single()
        assertEquals("remote-expense", remoteId)
        assertEquals(1234.5, body.amountRands, 0.0)
        assertTrue(repository.getExpense(synced.id)!!.isSynced)
    }

    @Test
    fun edit_withUnchangedReceipt_doesNotUploadItAgain() = runTest {
        val path = receipt("r1.jpg")
        val synced = savedAndSynced(path)
        assertEquals(1, api.receiptUploads.size)

        repository.updateExpense(synced.copy(litres = 11.0))
        repository.syncPendingExpenses()

        assertEquals(1, api.receiptUploads.size)
        assertTrue(repository.getExpense(synced.id)!!.isSynced)
    }

    @Test
    fun edit_withNewReceipt_uploadsTheNewPhoto() = runTest {
        val synced = savedAndSynced(receipt("old.jpg"))

        repository.updateExpense(synced.copy(receiptImagePath = receipt("new.jpg")))
        repository.syncPendingExpenses()

        assertEquals(2, api.receiptUploads.size)
        assertTrue(repository.getExpense(synced.id)!!.isSynced)
    }

    @Test
    fun edit_whileUploadIsInFlight_isNotMarkedSynced() = runTest {
        val id = repository.saveExpense(expense())
        api.createExpenseBehaviour = {
            repository.updateExpense(repository.getExpense(id)!!.copy(fuelStation = "Late edit"))
            retrofit2.Response.success(
                com.triplog.data.remote.dto.ApiResponse(true, FakeApiService.expenseDto("r1"), null)
            )
        }

        repository.syncPendingExpenses()
        assertFalse(repository.getExpense(id)!!.isSynced)

        repository.syncPendingExpenses()

        assertEquals("Late edit", api.expenseUpdates.single().second.fuelStation)
        assertEquals(1, api.expenseRequests.size)
        assertTrue(repository.getExpense(id)!!.isSynced)
    }

    @Test
    fun failedUpdate_staysPendingAndIsRetried() = runTest {
        val synced = savedAndSynced()
        repository.updateExpense(synced.copy(litres = 5.0))
        api.updateExpenseBehaviour = { throw IOException("offline") }

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(retryable = 1), result)
        assertFalse(repository.getExpense(synced.id)!!.isSynced)
    }

    // --- delete ---

    @Test
    fun delete_hidesExpenseFromListsAndTotalsImmediately() = runTest {
        val id = repository.saveExpense(expense())

        repository.deleteExpense(id)

        assertNull(repository.getExpense(id))
        assertTrue(repository.allExpenses.first().isEmpty())
        assertNull(repository.totalExpenseRands.first())
    }

    @Test
    fun delete_ofNeverSyncedExpense_isRemovedWithoutCallingTheServer() = runTest {
        api.createExpenseBehaviour = { throw IOException("offline") }
        val id = repository.saveExpense(expense())
        repository.deleteExpense(id)

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(synced = 1), result)
        assertTrue(dao.expenses.value.isEmpty())
        assertTrue(api.expenseRequests.isEmpty())
        assertTrue(api.expenseDeletes.isEmpty())
    }

    @Test
    fun delete_ofSyncedExpense_callsDeleteThenRemovesLocalRow() = runTest {
        val synced = savedAndSynced()
        repository.deleteExpense(synced.id)

        repository.syncPendingExpenses()

        assertEquals(listOf("remote-expense"), api.expenseDeletes)
        assertTrue(dao.expenses.value.isEmpty())
    }

    @Test
    fun delete_serverError_keepsTombstoneForRetry() = runTest {
        val synced = savedAndSynced()
        repository.deleteExpense(synced.id)
        api.deleteExpenseBehaviour = { FakeApiService.serverError() }

        val result = repository.syncPendingExpenses()

        assertEquals(SyncResult(retryable = 1), result)
        assertEquals(1, dao.expenses.value.size)
        assertNull(repository.getExpense(synced.id))
    }

    @Test
    fun delete_whenServerAlreadyLostIt_countsAsDone() = runTest {
        val synced = savedAndSynced()
        repository.deleteExpense(synced.id)
        api.deleteExpenseBehaviour = { FakeApiService.httpError(410) }

        repository.syncPendingExpenses()

        assertTrue(dao.expenses.value.isEmpty())
    }
}

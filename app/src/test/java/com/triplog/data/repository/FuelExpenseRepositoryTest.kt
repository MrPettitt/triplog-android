package com.triplog.data.repository

import com.triplog.domain.model.FuelExpense
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeFuelExpenseDao
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    private val repository = FuelExpenseRepository(dao, api)

    private fun expense(receiptPath: String? = null) = FuelExpense(
        date = LocalDate.of(2026, 1, 1),
        litres = 40.0,
        amountRands = 900.5,
        odometerKm = 84320,
        fuelStation = "Engen Sandton",
        receiptImagePath = receiptPath
    )

    @Test
    fun saveExpense_storesUnsyncedExpense() = runTest {
        repository.saveExpense(expense())

        assertFalse(dao.expenses.value.single().isSynced)
    }

    @Test
    fun sync_marksExpenseSyncedAndSendsExpectedRequest() = runTest {
        repository.saveExpense(expense())

        repository.syncPendingExpenses()

        val stored = dao.expenses.value.single()
        assertTrue(stored.isSynced)
        assertEquals("remote-expense", stored.remoteId)

        val request = api.expenseRequests.single()
        assertEquals("2026-01-01", request.date)
        assertEquals(900.5, request.amountRands, 0.0)
        assertEquals(84320, request.odometerKm)
        assertEquals("Engen Sandton", request.fuelStation)
    }

    @Test
    fun sync_uploadsReceiptWhenFileExists() = runTest {
        val receipt = tempFolder.newFile("receipt.jpg").apply { writeText("jpeg") }
        repository.saveExpense(expense(receiptPath = receipt.absolutePath))

        repository.syncPendingExpenses()

        assertEquals(listOf("remote-expense"), api.receiptUploads)
    }

    @Test
    fun sync_skipsReceiptUploadWhenFileIsMissing() = runTest {
        repository.saveExpense(expense(receiptPath = "/does/not/exist.jpg"))

        repository.syncPendingExpenses()

        assertTrue(api.receiptUploads.isEmpty())
        assertTrue(dao.expenses.value.single().isSynced)
    }

    @Test
    fun sync_leavesExpensePendingOnServerError() = runTest {
        api.createExpenseBehaviour = { FakeApiService.serverError() }
        repository.saveExpense(expense())

        repository.syncPendingExpenses()

        assertFalse(dao.expenses.value.single().isSynced)
        assertTrue(api.receiptUploads.isEmpty())
    }

    @Test
    fun sync_leavesExpensePendingOnNetworkException() = runTest {
        api.createExpenseBehaviour = { throw IOException("offline") }
        repository.saveExpense(expense())

        repository.syncPendingExpenses()

        assertFalse(dao.expenses.value.single().isSynced)
    }
}

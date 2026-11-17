package com.triplog.data.repository

import com.triplog.domain.model.FuelExpense
import com.triplog.testutil.FakeFuelExpenseDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate
import java.time.LocalDateTime

class FuelExpenseRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val dao = FakeFuelExpenseDao()
    private val repository = FuelExpenseRepository(dao)

    private fun expense(
        station: String = "Engen",
        date: LocalDate = LocalDate.of(2026, 1, 1),
        amount: Double = 900.0,
        receiptPath: String? = null
    ) = FuelExpense(
        date = date,
        litres = 40.0,
        amountRands = amount,
        odometerKm = 1000,
        fuelStation = station,
        receiptImagePath = receiptPath
    )

    private fun receiptFile(name: String) = tempFolder.newFile(name).apply { writeText("jpeg") }

    @Test
    fun saveExpense_storesItAndReturnsTheId() = runTest {
        val id = repository.saveExpense(expense("Shell"))

        assertEquals("Shell", repository.getExpense(id)!!.fuelStation)
    }

    @Test
    fun allExpenses_areNewestFirst() = runTest {
        repository.saveExpense(expense("old", date = LocalDate.of(2026, 1, 1)))
        repository.saveExpense(expense("new", date = LocalDate.of(2026, 3, 1)))
        repository.saveExpense(expense("middle", date = LocalDate.of(2026, 2, 1)))

        assertEquals(listOf("new", "middle", "old"), repository.allExpenses.first().map { it.fuelStation })
    }

    @Test
    fun total_followsTheStoredExpenses() = runTest {
        assertNull(repository.totalExpenseRands.first())

        repository.saveExpense(expense(amount = 100.5))
        repository.saveExpense(expense(amount = 200.0))

        assertEquals(300.5, repository.totalExpenseRands.first()!!, 0.0)
    }

    @Test
    fun updateExpense_changesFieldsButKeepsIdAndCreationTime() = runTest {
        val id = repository.saveExpense(expense("Before"))
        val original = repository.getExpense(id)!!

        repository.updateExpense(original.copy(fuelStation = "After", litres = 12.5, createdAt = LocalDateTime.MAX))

        val edited = repository.getExpense(id)!!
        assertEquals("After", edited.fuelStation)
        assertEquals(12.5, edited.litres, 0.0)
        assertEquals(original.createdAt, edited.createdAt)
        assertEquals(1, dao.expenses.value.size)
    }

    @Test
    fun updateExpense_withANewReceipt_deletesTheOldPhoto() = runTest {
        val old = receiptFile("old.jpg")
        val new = receiptFile("new.jpg")
        val id = repository.saveExpense(expense(receiptPath = old.absolutePath))

        repository.updateExpense(repository.getExpense(id)!!.copy(receiptImagePath = new.absolutePath))

        assertFalse(old.exists())
        assertTrue(new.exists())
    }

    @Test
    fun updateExpense_withTheSameReceipt_keepsThePhoto() = runTest {
        val photo = receiptFile("same.jpg")
        val id = repository.saveExpense(expense(receiptPath = photo.absolutePath))

        repository.updateExpense(repository.getExpense(id)!!.copy(litres = 5.0))

        assertTrue(photo.exists())
    }

    @Test
    fun updateExpense_ofAMissingExpense_doesNothing() = runTest {
        repository.updateExpense(expense().copy(id = 42))

        assertTrue(dao.expenses.value.isEmpty())
    }

    @Test
    fun deleteExpense_removesItAndItsReceiptPhoto() = runTest {
        val photo = receiptFile("receipt.jpg")
        val id = repository.saveExpense(expense(receiptPath = photo.absolutePath))

        repository.deleteExpense(id)

        assertNull(repository.getExpense(id))
        assertTrue(repository.allExpenses.first().isEmpty())
        assertFalse(photo.exists())
    }

    @Test
    fun deleteExpense_whenThePhotoIsAlreadyGone_stillSucceeds() = runTest {
        val id = repository.saveExpense(expense(receiptPath = "/does/not/exist.jpg"))

        repository.deleteExpense(id)

        assertTrue(dao.expenses.value.isEmpty())
    }
}

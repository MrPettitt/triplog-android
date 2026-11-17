package com.triplog.ui

import androidx.lifecycle.SavedStateHandle
import com.triplog.R
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.testutil.FakeFuelExpenseDao
import com.triplog.testutil.MainDispatcherRule
import com.triplog.ui.expenses.FuelExpenseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FuelExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeFuelExpenseDao()
    private val viewModel = FuelExpenseViewModel(
        FuelExpenseRepository(dao),
        SavedStateHandle()
    )

    private fun fillValidForm() {
        viewModel.onLitresChange("38.2")
        viewModel.onAmountChange("812.40")
        viewModel.onOdometerChange("84320")
        viewModel.onFuelStationChange("  Engen Sandton ")
    }

    @Test
    fun saveExpense_withValidForm_persistsExpenseAndSignalsSuccess() {
        fillValidForm()
        viewModel.onReceiptCaptured("/data/receipt.jpg")

        viewModel.saveExpense()

        assertTrue(viewModel.uiState.value.saveSuccess)
        val stored = dao.expenses.value.single()
        assertEquals(38.2, stored.litres, 0.0)
        assertEquals(812.40, stored.amountRands, 0.0)
        assertEquals(84320, stored.odometerKm)
        assertEquals("Engen Sandton", stored.fuelStation)
        assertEquals("/data/receipt.jpg", stored.receiptImagePath)
    }

    @Test
    fun saveExpense_withoutReceipt_isAllowed() {
        fillValidForm()

        viewModel.saveExpense()

        assertNull(dao.expenses.value.single().receiptImagePath)
    }

    @Test
    fun saveExpense_withMissingField_showsErrorAndDoesNotSave() {
        fillValidForm()
        viewModel.onOdometerChange("")

        viewModel.saveExpense()

        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.saveSuccess)
        assertTrue(dao.expenses.value.isEmpty())
    }

    @Test
    fun saveExpense_withBlankStation_showsError() {
        fillValidForm()
        viewModel.onFuelStationChange("   ")

        viewModel.saveExpense()

        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)
        assertTrue(dao.expenses.value.isEmpty())
    }

    @Test
    fun editingAField_clearsTheError() {
        viewModel.saveExpense()
        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)

        viewModel.onLitresChange("10")

        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun resetForm_restoresDefaults() {
        fillValidForm()
        viewModel.onReceiptCaptured("/data/receipt.jpg")

        viewModel.resetForm()

        val state = viewModel.uiState.value
        assertEquals("", state.litres)
        assertNull(state.receiptImagePath)
    }

    @Test
    fun saveExpense_acceptsCommaDecimals() {
        fillValidForm()
        viewModel.onLitresChange("38,2")
        viewModel.onAmountChange("812,40")

        viewModel.saveExpense()

        val stored = dao.expenses.value.single()
        assertEquals(38.2, stored.litres, 0.0)
        assertEquals(812.40, stored.amountRands, 0.0)
    }

    @Test
    fun saveExpense_rejectsZeroNegativeAndNonNumericValues() {
        val badInputs = listOf(
            { viewModel.onLitresChange("0") },
            { viewModel.onLitresChange("-5") },
            { viewModel.onAmountChange("abc") },
            { viewModel.onOdometerChange("12.5") },
            { viewModel.onOdometerChange("-1") }
        )
        for (breakIt in badInputs) {
            viewModel.resetForm()
            fillValidForm()
            breakIt()

            viewModel.saveExpense()

            assertEquals(R.string.error_invalid_expense_numbers, viewModel.uiState.value.errorMessageRes)
        }
        assertTrue(dao.expenses.value.isEmpty())
    }

    @Test
    fun cameraUnavailable_showsError() {
        viewModel.onCameraUnavailable()

        assertEquals(R.string.error_camera_unavailable, viewModel.uiState.value.errorMessageRes)
    }
}

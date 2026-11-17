package com.triplog.ui.expenses

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.R
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.domain.model.FuelExpense
import com.triplog.util.parseNonNegativeInt
import com.triplog.util.parsePositiveDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ExpenseUiState(
    val date: LocalDate = LocalDate.now(),
    val litres: String = "",
    val amountRands: String = "",
    val odometerKm: String = "",
    val fuelStation: String = "",
    val receiptImagePath: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
)

@HiltViewModel
class FuelExpenseViewModel @Inject constructor(
    private val fuelExpenseRepository: FuelExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    val allExpenses = fuelExpenseRepository.allExpenses

    fun onLitresChange(value: String) = _uiState.update { it.copy(litres = value, errorMessageRes = null) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountRands = value, errorMessageRes = null) }
    fun onOdometerChange(value: String) = _uiState.update { it.copy(odometerKm = value, errorMessageRes = null) }
    fun onFuelStationChange(value: String) = _uiState.update { it.copy(fuelStation = value, errorMessageRes = null) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value, errorMessageRes = null) }
    fun onCameraUnavailable() = _uiState.update { it.copy(errorMessageRes = R.string.error_camera_unavailable) }
    fun onReceiptCaptured(path: String) = _uiState.update { it.copy(receiptImagePath = path, errorMessageRes = null) }

    fun saveExpense() {
        val state = _uiState.value
        val litres = parsePositiveDecimal(state.litres)
        val amount = parsePositiveDecimal(state.amountRands)
        val odometer = parseNonNegativeInt(state.odometerKm)

        if (state.litres.isBlank() || state.amountRands.isBlank() ||
            state.odometerKm.isBlank() || state.fuelStation.isBlank()
        ) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_required_fields) }
            return
        }
        if (litres == null || amount == null || odometer == null) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_invalid_expense_numbers) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            val expense = FuelExpense(
                date = state.date,
                litres = litres,
                amountRands = amount,
                odometerKm = odometer,
                fuelStation = state.fuelStation.trim(),
                receiptImagePath = state.receiptImagePath,
                isSynced = false
            )
            fuelExpenseRepository.saveExpense(expense)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun resetForm() {
        _uiState.value = ExpenseUiState()
    }

    fun clearError() = _uiState.update { it.copy(errorMessageRes = null) }
}

package com.triplog.ui.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.domain.model.FuelExpense
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
    val errorMessage: String? = null
)

@HiltViewModel
class FuelExpenseViewModel @Inject constructor(
    private val fuelExpenseRepository: FuelExpenseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseUiState())
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    val allExpenses = fuelExpenseRepository.allExpenses

    fun onLitresChange(value: String) = _uiState.update { it.copy(litres = value) }
    fun onAmountChange(value: String) = _uiState.update { it.copy(amountRands = value) }
    fun onOdometerChange(value: String) = _uiState.update { it.copy(odometerKm = value) }
    fun onFuelStationChange(value: String) = _uiState.update { it.copy(fuelStation = value) }
    fun onDateChange(value: LocalDate) = _uiState.update { it.copy(date = value) }
    fun onReceiptCaptured(path: String) = _uiState.update { it.copy(receiptImagePath = path) }

    fun saveExpense() {
        val state = _uiState.value
        val litres = state.litres.toDoubleOrNull()
        val amount = state.amountRands.toDoubleOrNull()
        val odometer = state.odometerKm.toIntOrNull()

        if (litres == null || amount == null || odometer == null || state.fuelStation.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
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

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

package com.triplog.ui.expenses

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.R
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.domain.model.FuelExpense
import com.triplog.ui.navigation.ARG_EXPENSE_ID
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
    /** Set when an existing expense is being edited; null when adding a new one. */
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    /** True once the expense was saved or deleted and the screen should close. */
    val saveSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
) {
    val isEditing: Boolean get() = editingId != null
}

@HiltViewModel
class FuelExpenseViewModel @Inject constructor(
    private val fuelExpenseRepository: FuelExpenseRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(ARG_EXPENSE_ID)

    private val _uiState = MutableStateFlow(ExpenseUiState(editingId = editingId))
    val uiState: StateFlow<ExpenseUiState> = _uiState.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val expense = fuelExpenseRepository.getExpense(editingId)
                if (expense == null) {
                    // Deleted elsewhere (or a stale link): nothing to edit.
                    _uiState.update { it.copy(saveSuccess = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            date = expense.date,
                            litres = expense.litres.toString(),
                            amountRands = expense.amountRands.toString(),
                            odometerKm = expense.odometerKm.toString(),
                            fuelStation = expense.fuelStation,
                            receiptImagePath = expense.receiptImagePath
                        )
                    }
                }
            }
        }
    }

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
        if (state.date.isAfter(LocalDate.now())) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_expense_in_future) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            val expense = FuelExpense(
                id = state.editingId ?: 0,
                date = state.date,
                litres = litres,
                amountRands = amount,
                odometerKm = odometer,
                fuelStation = state.fuelStation.trim(),
                receiptImagePath = state.receiptImagePath,
            )
            if (state.editingId != null) {
                fuelExpenseRepository.updateExpense(expense)
            } else {
                fuelExpenseRepository.saveExpense(expense)
            }
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun deleteExpense() {
        val id = _uiState.value.editingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            fuelExpenseRepository.deleteExpense(id)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun resetForm() {
        _uiState.value = ExpenseUiState()
    }

    fun clearError() = _uiState.update { it.copy(errorMessageRes = null) }
}

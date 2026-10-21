package com.triplog.ui.logtrip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class LogTripUiState(
    val fromLocation: String = "",
    val toLocation: String = "",
    val distanceKm: String = "",
    val purpose: TripPurpose = TripPurpose.CLIENT_VISIT,
    val notes: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LogTripViewModel @Inject constructor(
    private val tripRepository: TripRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LogTripUiState())
    val uiState: StateFlow<LogTripUiState> = _uiState.asStateFlow()

    fun onFromLocationChange(value: String) = _uiState.update { it.copy(fromLocation = value) }
    fun onToLocationChange(value: String) = _uiState.update { it.copy(toLocation = value) }
    fun onDistanceChange(value: String) = _uiState.update { it.copy(distanceKm = value) }
    fun onPurposeChange(value: TripPurpose) = _uiState.update { it.copy(purpose = value) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onDateTimeChange(value: LocalDateTime) = _uiState.update { it.copy(dateTime = value) }

    fun saveTrip() {
        val state = _uiState.value
        val distance = state.distanceKm.toDoubleOrNull()

        if (state.fromLocation.isBlank() || state.toLocation.isBlank() || distance == null) {
            _uiState.update { it.copy(errorMessage = "Please fill in all required fields.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val trip = Trip(
                fromLocation = state.fromLocation.trim(),
                toLocation = state.toLocation.trim(),
                dateTime = state.dateTime,
                distanceKm = distance,
                purpose = state.purpose,
                notes = state.notes.trim(),
                isSynced = false // SyncWorker uploads it later
            )
            tripRepository.saveTrip(trip)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun resetForm() {
        _uiState.value = LogTripUiState()
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

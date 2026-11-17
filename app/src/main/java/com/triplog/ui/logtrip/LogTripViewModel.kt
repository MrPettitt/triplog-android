package com.triplog.ui.logtrip

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.R
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.ui.navigation.ARG_TRIP_ID
import com.triplog.util.parsePositiveDecimal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class LogTripUiState(
    val fromLocation: String = "",
    val toLocation: String = "",
    val distanceKm: String = "",
    val purpose: TripPurpose = TripPurpose.CLIENT_VISIT,
    val notes: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    /** Set when an existing trip is being edited; null when logging a new one. */
    val editingId: Long? = null,
    val isSaving: Boolean = false,
    /** True once the trip was saved or deleted and the screen should close. */
    val saveSuccess: Boolean = false,
    @StringRes val errorMessageRes: Int? = null
) {
    val isEditing: Boolean get() = editingId != null
}

@HiltViewModel
class LogTripViewModel @Inject constructor(
    private val tripRepository: TripRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editingId: Long? = savedStateHandle.get<Long>(ARG_TRIP_ID)

    private val _uiState = MutableStateFlow(LogTripUiState(editingId = editingId))
    val uiState: StateFlow<LogTripUiState> = _uiState.asStateFlow()

    init {
        if (editingId != null) {
            viewModelScope.launch {
                val trip = tripRepository.getTrip(editingId)
                if (trip == null) {
                    // Deleted elsewhere (or a stale link): nothing to edit.
                    _uiState.update { it.copy(saveSuccess = true) }
                } else {
                    _uiState.update {
                        it.copy(
                            fromLocation = trip.fromLocation,
                            toLocation = trip.toLocation,
                            distanceKm = trip.distanceKm.toString(),
                            purpose = trip.purpose,
                            notes = trip.notes,
                            dateTime = trip.dateTime
                        )
                    }
                }
            }
        }
    }

    fun onFromLocationChange(value: String) = _uiState.update { it.copy(fromLocation = value, errorMessageRes = null) }
    fun onToLocationChange(value: String) = _uiState.update { it.copy(toLocation = value, errorMessageRes = null) }
    fun onDistanceChange(value: String) = _uiState.update { it.copy(distanceKm = value, errorMessageRes = null) }
    fun onPurposeChange(value: TripPurpose) = _uiState.update { it.copy(purpose = value, errorMessageRes = null) }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value, errorMessageRes = null) }

    fun onDateChange(value: LocalDate) = _uiState.update {
        it.copy(dateTime = value.atTime(it.dateTime.toLocalTime()), errorMessageRes = null)
    }

    fun onTimeChange(value: LocalTime) = _uiState.update {
        it.copy(dateTime = it.dateTime.toLocalDate().atTime(value), errorMessageRes = null)
    }

    fun saveTrip() {
        val state = _uiState.value
        val distance = parsePositiveDecimal(state.distanceKm)

        if (state.fromLocation.isBlank() || state.toLocation.isBlank() || state.distanceKm.isBlank()) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_required_fields) }
            return
        }
        if (distance == null) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_invalid_distance) }
            return
        }
        // A minute of slack so a trip logged "now" is never rejected by the clock ticking over.
        if (state.dateTime.isAfter(LocalDateTime.now().plusMinutes(1))) {
            _uiState.update { it.copy(errorMessageRes = R.string.error_trip_in_future) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessageRes = null) }
            val trip = Trip(
                id = state.editingId ?: 0,
                fromLocation = state.fromLocation.trim(),
                toLocation = state.toLocation.trim(),
                dateTime = state.dateTime,
                distanceKm = distance,
                purpose = state.purpose,
                notes = state.notes.trim(),
                isSynced = false // SyncWorker uploads it later
            )
            if (state.editingId != null) {
                tripRepository.updateTrip(trip)
            } else {
                tripRepository.saveTrip(trip)
            }
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun deleteTrip() {
        val id = _uiState.value.editingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            tripRepository.deleteTrip(id)
            _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
        }
    }

    fun resetForm() {
        _uiState.value = LogTripUiState()
    }

    fun clearError() = _uiState.update { it.copy(errorMessageRes = null) }
}

package com.triplog.ui

import androidx.lifecycle.SavedStateHandle
import com.triplog.R
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.TripPurpose
import com.triplog.testutil.FakeTripDao
import com.triplog.testutil.MainDispatcherRule
import com.triplog.ui.logtrip.LogTripViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LogTripViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dao = FakeTripDao()
    private val viewModel = LogTripViewModel(TripRepository(dao), SavedStateHandle())

    private fun fillValidForm() {
        viewModel.onFromLocationChange("  Head Office ")
        viewModel.onToLocationChange("Client Site")
        viewModel.onDistanceChange("42.5")
        viewModel.onPurposeChange(TripPurpose.DELIVERY)
        viewModel.onNotesChange(" parcels ")
    }

    @Test
    fun saveTrip_withValidForm_persistsTrimmedTripAndSignalsSuccess() {
        fillValidForm()

        viewModel.saveTrip()

        val state = viewModel.uiState.value
        assertTrue(state.saveSuccess)
        assertFalse(state.isSaving)
        assertNull(state.errorMessageRes)

        val stored = dao.trips.value.single()
        assertEquals("Head Office", stored.fromLocation)
        assertEquals("Client Site", stored.toLocation)
        assertEquals(42.5, stored.distanceKm, 0.0)
        assertEquals("DELIVERY", stored.purpose)
        assertEquals("parcels", stored.notes)
    }

    @Test
    fun saveTrip_withBlankLocation_showsErrorAndDoesNotSave() {
        fillValidForm()
        viewModel.onFromLocationChange("   ")

        viewModel.saveTrip()

        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)
        assertFalse(viewModel.uiState.value.saveSuccess)
        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun saveTrip_withNonNumericDistance_showsInvalidNumberError() {
        fillValidForm()
        viewModel.onDistanceChange("abc")

        viewModel.saveTrip()

        assertEquals(R.string.error_invalid_distance, viewModel.uiState.value.errorMessageRes)
        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun editingAField_clearsTheError() {
        viewModel.saveTrip()
        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)

        viewModel.onFromLocationChange("A")

        assertNull(viewModel.uiState.value.errorMessageRes)
    }

    @Test
    fun resetForm_restoresDefaults() {
        fillValidForm()
        viewModel.saveTrip()

        viewModel.resetForm()

        val state = viewModel.uiState.value
        assertEquals("", state.fromLocation)
        assertEquals("", state.distanceKm)
        assertEquals(TripPurpose.CLIENT_VISIT, state.purpose)
        assertFalse(state.saveSuccess)
    }

    @Test
    fun saveTrip_acceptsCommaAsDecimalSeparator() {
        fillValidForm()
        viewModel.onDistanceChange("12,5")

        viewModel.saveTrip()

        assertEquals(12.5, dao.trips.value.single().distanceKm, 0.0)
    }

    @Test
    fun saveTrip_rejectsZeroAndNegativeDistance() {
        fillValidForm()

        for (bad in listOf("0", "-4", "0,0")) {
            viewModel.onDistanceChange(bad)
            viewModel.saveTrip()
            assertEquals(R.string.error_invalid_distance, viewModel.uiState.value.errorMessageRes)
        }
        assertTrue(dao.trips.value.isEmpty())
    }

    @Test
    fun saveTrip_withBlankDistance_asksForRequiredFields() {
        fillValidForm()
        viewModel.onDistanceChange("")

        viewModel.saveTrip()

        assertEquals(R.string.error_required_fields, viewModel.uiState.value.errorMessageRes)
    }
}

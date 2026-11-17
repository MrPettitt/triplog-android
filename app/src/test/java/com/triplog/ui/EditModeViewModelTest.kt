package com.triplog.ui

import androidx.lifecycle.SavedStateHandle
import com.triplog.R
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.testutil.FakeApiService
import com.triplog.testutil.FakeFuelExpenseDao
import com.triplog.testutil.FakeSyncScheduler
import com.triplog.testutil.FakeTripDao
import com.triplog.testutil.MainDispatcherRule
import com.triplog.ui.expenses.FuelExpenseViewModel
import com.triplog.ui.logtrip.LogTripViewModel
import com.triplog.ui.navigation.ARG_EXPENSE_ID
import com.triplog.ui.navigation.ARG_TRIP_ID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class EditModeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tripDao = FakeTripDao()
    private val tripRepository = TripRepository(tripDao, FakeApiService(), FakeSyncScheduler())
    private val expenseDao = FakeFuelExpenseDao()
    private val expenseRepository = FuelExpenseRepository(expenseDao, FakeApiService(), FakeSyncScheduler())

    private fun savedTrip(): Long = runBlocking {
        tripRepository.saveTrip(
            Trip(
                fromLocation = "Head Office",
                toLocation = "Client Site",
                dateTime = LocalDateTime.of(2026, 3, 2, 9, 30),
                distanceKm = 42.5,
                purpose = TripPurpose.SITE_INSPECTION,
                notes = "bring badge"
            )
        )
    }

    private fun savedExpense(): Long = runBlocking {
        expenseRepository.saveExpense(
            FuelExpense(
                date = LocalDate.of(2026, 3, 2),
                litres = 38.2,
                amountRands = 812.4,
                odometerKm = 84320,
                fuelStation = "Engen",
                receiptImagePath = "/data/r.jpg"
            )
        )
    }

    private fun tripViewModel(id: Long? = null) = LogTripViewModel(
        tripRepository,
        SavedStateHandle(if (id == null) emptyMap() else mapOf(ARG_TRIP_ID to id))
    )

    private fun expenseViewModel(id: Long? = null) = FuelExpenseViewModel(
        expenseRepository,
        SavedStateHandle(if (id == null) emptyMap() else mapOf(ARG_EXPENSE_ID to id))
    )

    // --- trips ---

    @Test
    fun editTrip_loadsTheExistingValues() {
        val viewModel = tripViewModel(savedTrip())

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals("Head Office", state.fromLocation)
        assertEquals("Client Site", state.toLocation)
        assertEquals("42.5", state.distanceKm)
        assertEquals(TripPurpose.SITE_INSPECTION, state.purpose)
        assertEquals("bring badge", state.notes)
        assertEquals(LocalDateTime.of(2026, 3, 2, 9, 30), state.dateTime)
    }

    @Test
    fun editTrip_saveUpdatesTheSameRowInsteadOfAddingOne() {
        val id = savedTrip()
        val viewModel = tripViewModel(id)

        viewModel.onToLocationChange("Warehouse")
        viewModel.onDistanceChange("50,5")
        viewModel.saveTrip()

        assertTrue(viewModel.uiState.value.saveSuccess)
        val stored = tripDao.trips.value.single()
        assertEquals(id, stored.id)
        assertEquals("Warehouse", stored.toLocation)
        assertEquals(50.5, stored.distanceKm, 0.0)
        assertFalse(stored.isSynced)
    }

    @Test
    fun editTrip_deleteHidesTheTripAndClosesTheScreen() {
        val id = savedTrip()
        val viewModel = tripViewModel(id)

        viewModel.deleteTrip()

        assertTrue(viewModel.uiState.value.saveSuccess)
        runBlocking { assertNull(tripRepository.getTrip(id)) }
    }

    @Test
    fun editTrip_forAMissingTrip_closesTheScreen() {
        val viewModel = tripViewModel(999)

        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun newTrip_isNotInEditMode_andDeleteDoesNothing() {
        val viewModel = tripViewModel()

        viewModel.deleteTrip()

        assertFalse(viewModel.uiState.value.isEditing)
        assertFalse(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun tripDateAndTime_areChangedIndependently() {
        val viewModel = tripViewModel(savedTrip())

        viewModel.onDateChange(LocalDate.of(2026, 2, 1))
        assertEquals(LocalDateTime.of(2026, 2, 1, 9, 30), viewModel.uiState.value.dateTime)

        viewModel.onTimeChange(LocalTime.of(16, 45))
        assertEquals(LocalDateTime.of(2026, 2, 1, 16, 45), viewModel.uiState.value.dateTime)
    }

    @Test
    fun trip_inTheFuture_isRejected() {
        val viewModel = tripViewModel()
        viewModel.onFromLocationChange("A")
        viewModel.onToLocationChange("B")
        viewModel.onDistanceChange("5")
        viewModel.onDateChange(LocalDate.now().plusDays(2))

        viewModel.saveTrip()

        assertEquals(R.string.error_trip_in_future, viewModel.uiState.value.errorMessageRes)
        assertTrue(tripDao.trips.value.isEmpty())
    }

    // --- expenses ---

    @Test
    fun editExpense_loadsTheExistingValues() {
        val viewModel = expenseViewModel(savedExpense())

        val state = viewModel.uiState.value
        assertTrue(state.isEditing)
        assertEquals(LocalDate.of(2026, 3, 2), state.date)
        assertEquals("38.2", state.litres)
        assertEquals("812.4", state.amountRands)
        assertEquals("84320", state.odometerKm)
        assertEquals("Engen", state.fuelStation)
        assertEquals("/data/r.jpg", state.receiptImagePath)
    }

    @Test
    fun editExpense_saveUpdatesTheSameRow() {
        val id = savedExpense()
        val viewModel = expenseViewModel(id)

        viewModel.onFuelStationChange("Shell")
        viewModel.onDateChange(LocalDate.of(2026, 3, 1))
        viewModel.saveExpense()

        assertTrue(viewModel.uiState.value.saveSuccess)
        val stored = expenseDao.expenses.value.single()
        assertEquals(id, stored.id)
        assertEquals("Shell", stored.fuelStation)
        assertEquals(LocalDate.of(2026, 3, 1), stored.date)
        assertFalse(stored.isSynced)
    }

    @Test
    fun editExpense_deleteHidesTheExpenseAndClosesTheScreen() {
        val id = savedExpense()
        val viewModel = expenseViewModel(id)

        viewModel.deleteExpense()

        assertTrue(viewModel.uiState.value.saveSuccess)
        runBlocking { assertNull(expenseRepository.getExpense(id)) }
    }

    @Test
    fun expense_inTheFuture_isRejected() {
        val viewModel = expenseViewModel()
        viewModel.onLitresChange("10")
        viewModel.onAmountChange("200")
        viewModel.onOdometerChange("1000")
        viewModel.onFuelStationChange("Engen")
        viewModel.onDateChange(LocalDate.now().plusDays(1))

        viewModel.saveExpense()

        assertEquals(R.string.error_expense_in_future, viewModel.uiState.value.errorMessageRes)
        assertTrue(expenseDao.expenses.value.isEmpty())
    }
}

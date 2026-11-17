package com.triplog.ui

import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.FuelExpense
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.testutil.FakeFuelExpenseDao
import com.triplog.testutil.FakeTripDao
import com.triplog.testutil.MainDispatcherRule
import com.triplog.ui.expenses.FuelListViewModel
import com.triplog.ui.trips.AllTripsViewModel
import com.triplog.util.DateRange
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class ListViewModelsTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tripRepository = TripRepository(FakeTripDao())
    private val expenseRepository = FuelExpenseRepository(FakeFuelExpenseDao())

    private fun addTrip(from: String, purpose: TripPurpose = TripPurpose.MEETING, daysAgo: Long = 0) = runBlocking {
        tripRepository.saveTrip(
            Trip(
                fromLocation = from,
                toLocation = "Somewhere",
                dateTime = LocalDate.now().minusDays(daysAgo).atTime(8, 0),
                distanceKm = 10.0,
                purpose = purpose
            )
        )
    }

    private fun addExpense(station: String, amount: Double, daysAgo: Long = 0) = runBlocking {
        expenseRepository.saveExpense(
            FuelExpense(
                date = LocalDate.now().minusDays(daysAgo),
                litres = 10.0,
                amountRands = amount,
                odometerKm = 1000,
                fuelStation = station
            )
        )
    }

    @Test
    fun trips_filterAsTheUserTypesAndCanBeCleared() = runTest {
        addTrip("Rosebank")
        addTrip("Sandton")
        val viewModel = AllTripsViewModel(tripRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(2, viewModel.uiState.value.trips.size)
        assertFalse(viewModel.uiState.value.filter.isActive)

        viewModel.onQueryChange("rose")
        assertEquals(listOf("Rosebank"), viewModel.uiState.value.trips.map { it.fromLocation })
        assertEquals("the unfiltered count is kept", 2, viewModel.uiState.value.totalCount)
        assertTrue(viewModel.uiState.value.filter.isActive)

        viewModel.clearFilters()
        assertEquals(2, viewModel.uiState.value.trips.size)
        assertFalse(viewModel.uiState.value.filter.isActive)
    }

    @Test
    fun trips_combinePurposeAndRange() = runTest {
        addTrip("recent delivery", TripPurpose.DELIVERY, daysAgo = 2)
        addTrip("old delivery", TripPurpose.DELIVERY, daysAgo = 90)
        addTrip("recent meeting", TripPurpose.MEETING, daysAgo = 2)
        val viewModel = AllTripsViewModel(tripRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onPurposeChange(TripPurpose.DELIVERY)
        viewModel.onRangeChange(DateRange.LAST_30_DAYS)

        assertEquals(listOf("recent delivery"), viewModel.uiState.value.trips.map { it.fromLocation })
    }

    @Test
    fun trips_noMatchesStillReportsTheTotal() = runTest {
        addTrip("Rosebank")
        val viewModel = AllTripsViewModel(tripRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onQueryChange("zzz")

        assertTrue(viewModel.uiState.value.trips.isEmpty())
        assertEquals(1, viewModel.uiState.value.totalCount)
    }

    @Test
    fun fuel_totalFollowsTheFilteredList() = runTest {
        addExpense("Engen Sandton", 100.0)
        addExpense("Shell Rosebank", 250.5)
        addExpense("Engen Midrand", 49.5)
        val viewModel = FuelListViewModel(expenseRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        assertEquals(400.0, viewModel.uiState.value.totalRands, 0.0)

        viewModel.onQueryChange("engen")

        assertEquals(2, viewModel.uiState.value.expenses.size)
        assertEquals(149.5, viewModel.uiState.value.totalRands, 0.0)
        assertEquals(3, viewModel.uiState.value.totalCount)
    }

    @Test
    fun fuel_rangeAndClear() = runTest {
        addExpense("recent", 10.0, daysAgo = 3)
        addExpense("old", 20.0, daysAgo = 120)
        val viewModel = FuelListViewModel(expenseRepository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.onRangeChange(DateRange.LAST_30_DAYS)
        assertEquals(listOf("recent"), viewModel.uiState.value.expenses.map { it.fuelStation })

        viewModel.clearFilters()
        assertEquals(2, viewModel.uiState.value.expenses.size)
    }
}

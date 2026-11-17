package com.triplog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val totalTrips: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalFuelSpentRands: Double = 0.0,
    val recentTrips: List<Trip> = emptyList()
)

private const val RECENT_TRIP_LIMIT = 5

@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    fuelExpenseRepository: FuelExpenseRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        tripRepository.totalTripCount,
        tripRepository.totalDistanceKm,
        fuelExpenseRepository.totalExpenseRands,
        tripRepository.allTrips
    ) { tripCount, distanceKm, fuelSpent, trips ->
        HomeUiState(
            totalTrips = tripCount,
            totalDistanceKm = distanceKm ?: 0.0,
            totalFuelSpentRands = fuelSpent ?: 0.0,
            recentTrips = trips.take(RECENT_TRIP_LIMIT)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

package com.triplog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.util.ConnectivityObserver
import com.triplog.util.NetworkStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class HomeUiState(
    val totalTrips: Int = 0,
    val totalDistanceKm: Double = 0.0,
    val totalFuelSpentRands: Double = 0.0,
    val pendingSyncCount: Int = 0,
    val recentTrips: List<Trip> = emptyList(),
    val networkStatus: NetworkStatus = NetworkStatus.Available
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    fuelExpenseRepository: FuelExpenseRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        tripRepository.totalTripCount,
        tripRepository.totalDistanceKm,
        fuelExpenseRepository.totalExpenseRands,
        tripRepository.pendingSyncCount,
        tripRepository.allTrips,
        connectivityObserver.networkStatus
    ) { values ->
        val tripCount = values[0] as Int
        val distanceKm = (values[1] as? Double) ?: 0.0
        val fuelSpent = (values[2] as? Double) ?: 0.0
        val pending = values[3] as Int
        @Suppress("UNCHECKED_CAST")
        val trips = values[4] as List<Trip>
        val network = values[5] as NetworkStatus

        HomeUiState(
            totalTrips = tripCount,
            totalDistanceKm = distanceKm,
            totalFuelSpentRands = fuelSpent,
            pendingSyncCount = pending,
            recentTrips = trips.take(5),
            networkStatus = network
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )
}

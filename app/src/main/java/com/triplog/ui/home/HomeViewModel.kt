package com.triplog.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.sync.SyncConfig
import com.triplog.util.ConnectivityObserver
import com.triplog.util.NetworkStatus
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
    /** Trips and fuel expenses that have not finished syncing. */
    val pendingSyncCount: Int = 0,
    val recentTrips: List<Trip> = emptyList(),
    val networkStatus: NetworkStatus = NetworkStatus.Available,
    val syncConfigured: Boolean = true
)

private const val RECENT_TRIP_LIMIT = 5

@HiltViewModel
class HomeViewModel @Inject constructor(
    tripRepository: TripRepository,
    fuelExpenseRepository: FuelExpenseRepository,
    connectivityObserver: ConnectivityObserver,
    syncConfig: SyncConfig
) : ViewModel() {

    private data class Totals(val trips: Int, val distanceKm: Double, val fuelSpentRands: Double)

    private val totals = combine(
        tripRepository.totalTripCount,
        tripRepository.totalDistanceKm,
        fuelExpenseRepository.totalExpenseRands
    ) { trips, distanceKm, fuelSpent ->
        Totals(trips, distanceKm ?: 0.0, fuelSpent ?: 0.0)
    }

    private val pending = combine(
        tripRepository.pendingSyncCount,
        fuelExpenseRepository.pendingSyncCount
    ) { tripsPending, expensesPending -> tripsPending + expensesPending }

    val uiState: StateFlow<HomeUiState> = combine(
        totals,
        pending,
        tripRepository.allTrips,
        connectivityObserver.networkStatus
    ) { totals, pending, trips, network ->
        HomeUiState(
            totalTrips = totals.trips,
            totalDistanceKm = totals.distanceKm,
            totalFuelSpentRands = totals.fuelSpentRands,
            pendingSyncCount = pending,
            recentTrips = trips.take(RECENT_TRIP_LIMIT),
            networkStatus = network,
            syncConfigured = syncConfig.isConfigured
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(syncConfigured = syncConfig.isConfigured)
    )
}

package com.triplog.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.ui.TripCard
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AllTripsViewModel @Inject constructor(tripRepository: TripRepository) : ViewModel() {
    val trips: StateFlow<List<Trip>> = tripRepository.allTrips.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
}

@Composable
fun AllTripsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AllTripsViewModel = hiltViewModel()
) {
    val trips by viewModel.trips.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
    ) {
        TripLogTopBar(
            title = "All trips",
            subtitle = if (trips.isEmpty()) null else "${trips.size} trip${if (trips.size != 1) "s" else ""} logged",
            onBack = onNavigateBack
        )

        if (trips.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No trips logged yet.", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripCard(trip = trip)
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

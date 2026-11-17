package com.triplog.ui.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.triplog.R
import com.triplog.data.repository.TripRepository
import com.triplog.domain.model.Trip
import com.triplog.domain.model.TripPurpose
import com.triplog.ui.ChipRow
import com.triplog.ui.ExportCsvButton
import com.triplog.ui.SearchField
import com.triplog.ui.TripCard
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.dateRangeLabel
import com.triplog.ui.purposeChipLabel
import com.triplog.ui.theme.Brand400
import com.triplog.ui.theme.Surface900
import com.triplog.ui.theme.TextSecondary
import com.triplog.util.CsvExport
import com.triplog.util.DateRange
import com.triplog.util.TripFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import javax.inject.Inject

data class AllTripsUiState(
    /** Trips after the search and filters are applied. */
    val trips: List<Trip> = emptyList(),
    /** Every stored trip, so the screen can tell "no trips yet" from "nothing matches". */
    val totalCount: Int = 0,
    val filter: TripFilter = TripFilter()
)

@HiltViewModel
class AllTripsViewModel @Inject constructor(tripRepository: TripRepository) : ViewModel() {

    private val filter = MutableStateFlow(TripFilter())

    val uiState: StateFlow<AllTripsUiState> = combine(tripRepository.allTrips, filter) { trips, filter ->
        AllTripsUiState(trips = filter.apply(trips), totalCount = trips.size, filter = filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllTripsUiState())

    fun onQueryChange(query: String) = filter.update { it.copy(query = query) }
    fun onPurposeChange(purpose: TripPurpose?) = filter.update { it.copy(purpose = purpose) }
    fun onRangeChange(range: DateRange) = filter.update { it.copy(range = range) }
    fun clearFilters() {
        filter.value = TripFilter()
    }
}

@Composable
fun AllTripsScreen(
    onTripClick: (Long) -> Unit,
    viewModel: AllTripsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val trips = state.trips

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .imePadding()
    ) {
        TripLogTopBar(
            title = stringResource(R.string.all_trips_title),
            subtitle = when {
                state.totalCount == 0 -> null
                state.filter.isActive -> resources.getString(R.string.filtered_trips_subtitle, trips.size, state.totalCount)
                else -> resources.getQuantityString(R.plurals.all_trips_subtitle, trips.size, trips.size)
            },
            actions = {
                // Exports what is on screen, so a filtered list gives a filtered file.
                ExportCsvButton(
                    enabled = trips.isNotEmpty(),
                    fileName = "triplog-trips-${LocalDate.now()}.csv",
                    contentDescription = stringResource(R.string.export_trips),
                    buildCsv = {
                        CsvExport.trips(trips) { purpose -> resources.getString(purpose.labelRes) }
                    }
                )
            }
        )

        if (state.totalCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.all_trips_empty), color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
            }
            return@Column
        }

        SearchField(
            query = state.filter.query,
            onQueryChange = viewModel::onQueryChange,
            hint = stringResource(R.string.search_trips_hint),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(10.dp))
        ChipRow(
            options = DateRange.entries,
            selected = state.filter.range,
            label = { dateRangeLabel(it) },
            onSelect = viewModel::onRangeChange
        )
        ChipRow(
            options = listOf<TripPurpose?>(null) + TripPurpose.entries,
            selected = state.filter.purpose,
            label = { purposeChipLabel(it) },
            onSelect = viewModel::onPurposeChange
        )
        Spacer(Modifier.height(6.dp))

        if (trips.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.filter_no_results),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
                TextButton(onClick = viewModel::clearFilters) {
                    Text(stringResource(R.string.filter_clear_all), color = Brand400, style = MaterialTheme.typography.labelLarge)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

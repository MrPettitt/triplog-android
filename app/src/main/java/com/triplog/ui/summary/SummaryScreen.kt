package com.triplog.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.ui.ExportCsvButton
import com.triplog.ui.SurfaceCard
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.Brand400
import com.triplog.ui.theme.Surface900
import com.triplog.ui.theme.TextPrimary
import com.triplog.ui.theme.TextSecondary
import com.triplog.util.CsvExport
import com.triplog.util.MonthSummary
import com.triplog.util.formatKm
import com.triplog.util.formatRands
import com.triplog.util.summarizeByMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    tripRepository: TripRepository,
    fuelExpenseRepository: FuelExpenseRepository
) : ViewModel() {

    val months: StateFlow<List<MonthSummary>> = combine(
        tripRepository.allTrips,
        fuelExpenseRepository.allExpenses
    ) { trips, expenses ->
        summarizeByMonth(trips, expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

private val MonthTitleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
    ) {
        TripLogTopBar(
            title = stringResource(R.string.summary_title),
            subtitle = if (months.isEmpty()) {
                null
            } else {
                resources.getQuantityString(R.plurals.summary_subtitle, months.size, months.size)
            },
            actions = {
                ExportCsvButton(
                    enabled = months.isNotEmpty(),
                    fileName = "triplog-monthly-summary-${LocalDate.now()}.csv",
                    contentDescription = stringResource(R.string.export_summary),
                    buildCsv = { CsvExport.monthlySummary(months) }
                )
            }
        )

        if (months.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.summary_empty),
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(months, key = { it.month.toString() }) { MonthCard(it) }
            }
        }
    }
}

@Composable
private fun MonthCard(summary: MonthSummary) {
    val resources = LocalContext.current.resources
    val title = summary.month.format(MonthTitleFormat).replaceFirstChar { it.titlecase(Locale.getDefault()) }

    SurfaceCard {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Metric(
                label = stringResource(R.string.home_metric_trips),
                value = summary.tripCount.toString(),
                modifier = Modifier.weight(1f)
            )
            Metric(
                label = stringResource(R.string.summary_distance),
                value = "${formatKm(summary.distanceKm)} ${stringResource(R.string.unit_km)}",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            Metric(
                label = stringResource(R.string.home_metric_fuel_spend),
                value = formatRands(summary.fuelSpendRands),
                modifier = Modifier.weight(1f)
            )
            Metric(
                label = stringResource(R.string.summary_litres),
                value = String.format(Locale.getDefault(), "%.1f L", summary.litres),
                modifier = Modifier.weight(1f)
            )
        }
        if (summary.fillUpCount > 0) {
            Spacer(Modifier.height(12.dp))
            Text(
                resources.getQuantityString(R.plurals.summary_fill_ups, summary.fillUpCount, summary.fillUpCount),
                style = MaterialTheme.typography.bodySmall,
                color = Brand400
            )
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

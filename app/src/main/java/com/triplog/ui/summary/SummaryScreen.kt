package com.triplog.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.triplog.R
import com.triplog.data.repository.FuelExpenseRepository
import com.triplog.data.repository.TripRepository
import com.triplog.data.settings.BudgetSettings
import com.triplog.ui.ExportCsvButton
import com.triplog.ui.SurfaceCard
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.Amber400
import com.triplog.ui.theme.Brand400
import com.triplog.ui.theme.GreenSync
import com.triplog.ui.theme.RedError
import com.triplog.ui.theme.Surface600
import com.triplog.ui.theme.Surface900
import com.triplog.ui.theme.TextPrimary
import com.triplog.ui.theme.TextSecondary
import com.triplog.util.BudgetStatus
import com.triplog.util.CsvExport
import com.triplog.util.MonthSummary
import com.triplog.util.budgetStatus
import com.triplog.util.formatKm
import com.triplog.util.formatRands
import com.triplog.util.parsePositiveDecimal
import com.triplog.util.summarizeByMonth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    tripRepository: TripRepository,
    fuelExpenseRepository: FuelExpenseRepository,
    private val budgetSettings: BudgetSettings
) : ViewModel() {

    val months: StateFlow<List<MonthSummary>> = combine(
        tripRepository.allTrips,
        fuelExpenseRepository.allExpenses
    ) { trips, expenses ->
        summarizeByMonth(trips, expenses)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budget: StateFlow<Double?> = budgetSettings.monthlyBudget

    /** Pass null to remove the budget. */
    fun setBudget(rands: Double?) = budgetSettings.setMonthlyBudget(rands)
}

private val MonthTitleFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("LLLL yyyy")

@Composable
fun SummaryScreen(viewModel: SummaryViewModel = hiltViewModel()) {
    val months by viewModel.months.collectAsStateWithLifecycle()
    val budget by viewModel.budget.collectAsStateWithLifecycle()
    var editingBudget by remember { mutableStateOf(false) }
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

        val spentThisMonth = months.firstOrNull { it.month == YearMonth.now() }?.fuelSpendRands ?: 0.0

        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "budget") {
                BudgetCard(
                    status = budgetStatus(budget, spentThisMonth),
                    onEdit = { editingBudget = true }
                )
            }
            if (months.isEmpty()) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.summary_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(months, key = { it.month.toString() }) { MonthCard(it, budget) }
            }
        }
    }

    if (editingBudget) {
        BudgetDialog(
            current = budget,
            onSave = {
                viewModel.setBudget(it)
                editingBudget = false
            },
            onRemove = {
                viewModel.setBudget(null)
                editingBudget = false
            },
            onDismiss = { editingBudget = false }
        )
    }
}

@Composable
private fun BudgetCard(status: BudgetStatus?, onEdit: () -> Unit) {
    SurfaceCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.budget_title),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Text(
                stringResource(if (status == null) R.string.budget_set else R.string.budget_edit),
                style = MaterialTheme.typography.labelLarge,
                color = Brand400
            )
        }
        Spacer(Modifier.height(10.dp))
        if (status == null) {
            Text(
                stringResource(R.string.budget_none),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            val color = when {
                status.isOver -> RedError
                status.isNearLimit -> Amber400
                else -> GreenSync
            }
            Text(
                stringResource(R.string.budget_spent_of, formatRands(status.spentRands), formatRands(status.budgetRands)),
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Surface600)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(status.fractionUsed)
                        .clip(RoundedCornerShape(5.dp))
                        .background(color)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                budgetMessage(status),
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
private fun budgetMessage(status: BudgetStatus): String = when {
    status.isOver -> stringResource(R.string.budget_over, formatRands(-status.remainingRands))
    status.isNearLimit -> stringResource(R.string.budget_near, formatRands(status.remainingRands))
    else -> stringResource(R.string.budget_remaining, formatRands(status.remainingRands))
}

@Composable
private fun BudgetDialog(
    current: Double?,
    onSave: (Double) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(current?.let { String.format(Locale.US, "%.2f", it) } ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = {
                    text = it
                    showError = false
                },
                label = { Text(stringResource(R.string.budget_field_label)) },
                isError = showError,
                supportingText = if (showError) {
                    { Text(stringResource(R.string.budget_error)) }
                } else {
                    null
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        },
        confirmButton = {
            TextButton(onClick = {
                val value = parsePositiveDecimal(text)
                if (value == null) showError = true else onSave(value)
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (current != null) {
                    TextButton(onClick = onRemove) {
                        Text(stringResource(R.string.budget_remove), color = RedError)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

@Composable
private fun MonthCard(summary: MonthSummary, budget: Double?) {
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
        if (budget != null && summary.fuelSpendRands > budget) {
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.budget_over, formatRands(summary.fuelSpendRands - budget)),
                style = MaterialTheme.typography.bodySmall,
                color = RedError
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

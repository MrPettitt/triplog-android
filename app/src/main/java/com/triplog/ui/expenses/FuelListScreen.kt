package com.triplog.ui.expenses

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import com.triplog.domain.model.FuelExpense
import com.triplog.ui.SurfaceCard
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.Brand500
import com.triplog.ui.theme.Surface900
import com.triplog.ui.theme.TextPrimary
import com.triplog.ui.theme.TextSecondary
import com.triplog.util.formatRands
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class FuelListUiState(
    val expenses: List<FuelExpense> = emptyList(),
    val totalRands: Double = 0.0
)

@HiltViewModel
class FuelListViewModel @Inject constructor(repository: FuelExpenseRepository) : ViewModel() {
    val uiState: StateFlow<FuelListUiState> = combine(
        repository.allExpenses,
        repository.totalExpenseRands
    ) { expenses, total ->
        FuelListUiState(expenses = expenses, totalRands = total ?: 0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelListUiState())
}

private val ExpenseDateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@Composable
fun FuelListScreen(
    onAddExpense: () -> Unit,
    onExpenseClick: (Long) -> Unit,
    viewModel: FuelListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            TripLogTopBar(
                title = stringResource(R.string.fuel_list_title),
                subtitle = if (state.expenses.isEmpty()) {
                    null
                } else {
                    resources.getQuantityString(
                        R.plurals.fuel_list_subtitle,
                        state.expenses.size,
                        state.expenses.size,
                        formatRands(state.totalRands)
                    )
                }
            )

            if (state.expenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.fuel_list_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.expenses, key = { it.id }) { expense ->
                        ExpenseCard(expense = expense, onClick = { onExpenseClick(expense.id) })
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onAddExpense,
            containerColor = Brand500,
            contentColor = androidx.compose.ui.graphics.Color.White,
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.fuel_list_add), style = MaterialTheme.typography.labelLarge) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
        )
    }
}

@Composable
private fun ExpenseCard(expense: FuelExpense, onClick: () -> Unit) {
    SurfaceCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                expense.fuelStation,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            expense.date.format(ExpenseDateFormat),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(
                        R.string.fuel_card_litres_odometer,
                        String.format(java.util.Locale.getDefault(), "%.2f", expense.litres),
                        expense.odometerKm.toString()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (expense.receiptImagePath != null) {
                    Text(
                        stringResource(R.string.receipt_attached),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            Text(
                formatRands(expense.amountRands),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
    }
}

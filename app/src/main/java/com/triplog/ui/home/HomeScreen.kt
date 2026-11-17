package com.triplog.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocationAlt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.triplog.R
import com.triplog.ui.TripCard
import com.triplog.ui.theme.*
import com.triplog.util.NetworkStatus
import com.triplog.util.formatKm
import com.triplog.util.formatRands
import java.time.LocalTime

@Composable
private fun greeting(): String = stringResource(when (LocalTime.now().hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    else -> R.string.greeting_evening
})

@Composable
fun HomeScreen(
    onNavigateToLogTrip: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToAllTrips: () -> Unit,
    onTripClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline = state.networkStatus == NetworkStatus.Available

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(greeting(), style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
            }
        }

        item {
            SummaryCard(
                distanceKm = state.totalDistanceKm,
                trips = state.totalTrips,
                fuelSpent = state.totalFuelSpentRands,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            SyncBanner(
                isOnline = isOnline,
                pending = state.pendingSyncCount,
                syncConfigured = state.syncConfigured,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(20.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ActionTile(stringResource(R.string.home_action_log_trip), Icons.Default.AddLocationAlt, onNavigateToLogTrip, Modifier.weight(1f))
                ActionTile(stringResource(R.string.home_action_log_fuel), Icons.Default.LocalGasStation, onNavigateToExpenses, Modifier.weight(1f))
            }
            Spacer(Modifier.height(28.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.home_recent_trips), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                TextButton(onClick = onNavigateToAllTrips) {
                    Text(stringResource(R.string.home_see_all), color = Brand400, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.height(4.dp))
        }

        if (state.recentTrips.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.home_empty),
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            items(state.recentTrips, key = { it.id }) { trip ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    TripCard(trip = trip, onClick = { onTripClick(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(distanceKm: Double, trips: Int, fuelSpent: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(BrandGradient)
            .padding(22.dp)
    ) {
        Text(stringResource(R.string.home_total_distance), style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.85f))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                formatKm(distanceKm),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.unit_km),
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.18f))
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Metric(stringResource(R.string.home_metric_trips), trips.toString())
            Metric(stringResource(R.string.home_metric_fuel_spend), formatRands(fuelSpent))
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Text(value, style = MaterialTheme.typography.titleLarge, color = Color.White)
    }
}

@Composable
private fun SyncBanner(
    isOnline: Boolean,
    pending: Int,
    syncConfigured: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isOnline && syncConfigured) GreenSync else Amber400
    val resources = LocalContext.current.resources
    val text = when {
        !syncConfigured -> stringResource(R.string.sync_not_configured)
        !isOnline -> resources.getQuantityString(R.plurals.sync_offline, pending, pending)
        pending == 0 -> stringResource(R.string.sync_all_synced)
        else -> resources.getQuantityString(R.plurals.sync_syncing, pending, pending)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(12.dp))
        Text(text, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ActionTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Surface700)
            .border(1.dp, Surface600, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brand400.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Brand400)
        }
        Spacer(Modifier.height(14.dp))
        Text(label, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
    }
}

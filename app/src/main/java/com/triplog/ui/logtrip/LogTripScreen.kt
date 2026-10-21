package com.triplog.ui.logtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.triplog.domain.model.TripPurpose
import com.triplog.ui.FieldLabel
import com.triplog.ui.PrimaryButton
import com.triplog.ui.fieldColors
import com.triplog.ui.TripLogTextField
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogTripScreen(
    onNavigateBack: () -> Unit,
    viewModel: LogTripViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetForm()
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface900)
            .verticalScroll(rememberScrollState())
    ) {
        TripLogTopBar(title = "Log trip", subtitle = "Saved on this device, synced when online", onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TripLogTextField(
                value = state.fromLocation,
                onValueChange = viewModel::onFromLocationChange,
                label = "Start location",
                placeholder = "e.g. Head Office, 15 Main St"
            )

            TripLogTextField(
                value = state.toLocation,
                onValueChange = viewModel::onToLocationChange,
                label = "End location",
                placeholder = "e.g. Client Site, 84 Park Ave"
            )

            TripLogTextField(
                value = state.distanceKm,
                onValueChange = viewModel::onDistanceChange,
                label = "Distance (km)",
                placeholder = "0.0",
                keyboardType = KeyboardType.Decimal
            )

            // Purpose dropdown
            Column {
                FieldLabel("Purpose")
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = state.purpose.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Surface600)
                    ) {
                        TripPurpose.entries.forEach { purpose ->
                            DropdownMenuItem(
                                text = { Text(purpose.label, color = TextPrimary, style = MaterialTheme.typography.bodyLarge) },
                                onClick = {
                                    viewModel.onPurposeChange(purpose)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            TripLogTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                label = "Notes (optional)",
                placeholder = "Anything worth remembering",
                singleLine = false,
                maxLines = 4
            )

            state.errorMessage?.let { error ->
                Text(error, color = RedError, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = "Save trip",
                onClick = viewModel::saveTrip,
                isLoading = state.isSaving
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

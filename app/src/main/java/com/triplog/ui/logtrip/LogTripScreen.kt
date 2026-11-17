package com.triplog.ui.logtrip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.triplog.R
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
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
        TripLogTopBar(title = stringResource(R.string.log_trip_title), subtitle = stringResource(R.string.log_trip_subtitle), onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            TripLogTextField(
                value = state.fromLocation,
                onValueChange = viewModel::onFromLocationChange,
                label = stringResource(R.string.field_start_location),
                placeholder = stringResource(R.string.field_start_location_hint)
            )

            TripLogTextField(
                value = state.toLocation,
                onValueChange = viewModel::onToLocationChange,
                label = stringResource(R.string.field_end_location),
                placeholder = stringResource(R.string.field_end_location_hint)
            )

            TripLogTextField(
                value = state.distanceKm,
                onValueChange = viewModel::onDistanceChange,
                label = stringResource(R.string.field_distance),
                placeholder = stringResource(R.string.field_distance_hint),
                keyboardType = KeyboardType.Decimal
            )

            // Purpose dropdown
            Column {
                FieldLabel(stringResource(R.string.field_purpose))
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = stringResource(state.purpose.labelRes),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                                text = { Text(stringResource(purpose.labelRes), color = TextPrimary, style = MaterialTheme.typography.bodyLarge) },
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
                label = stringResource(R.string.field_notes),
                placeholder = stringResource(R.string.field_notes_hint),
                singleLine = false,
                maxLines = 4
            )

            state.errorMessageRes?.let { errorRes ->
                Text(stringResource(errorRes), color = RedError, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = stringResource(R.string.action_save_trip),
                onClick = viewModel::saveTrip,
                isLoading = state.isSaving
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

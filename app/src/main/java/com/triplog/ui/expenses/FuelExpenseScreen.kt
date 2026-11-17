package com.triplog.ui.expenses

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.triplog.util.ReceiptImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.triplog.R
import com.triplog.ui.FieldLabel
import com.triplog.ui.PrimaryButton
import com.triplog.ui.TripLogTextField
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FuelExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: FuelExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    // Saved so a rotation while the camera app is open does not lose the capture target.
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            if (success) {
                scope.launch {
                    // Phone photos are several MB; shrink before storing and uploading.
                    withContext(Dispatchers.IO) { ReceiptImage.downscale(File(path)) }
                    viewModel.onReceiptCaptured(path)
                }
            } else {
                // Cancelled: do not leave an empty file behind.
                File(path).delete()
            }
        }
    }

    fun launchCamera() {
        val imageFile = createImageFile(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        pendingPhotoPath = imageFile.absolutePath
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPhotoPath = null
            imageFile.delete()
            viewModel.onCameraUnavailable()
        }
    }

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
        TripLogTopBar(title = stringResource(R.string.fuel_title), subtitle = stringResource(R.string.fuel_subtitle), onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TripLogTextField(
                    value = state.litres,
                    onValueChange = viewModel::onLitresChange,
                    label = stringResource(R.string.field_litres),
                    placeholder = stringResource(R.string.field_amount_hint),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                TripLogTextField(
                    value = state.amountRands,
                    onValueChange = viewModel::onAmountChange,
                    label = stringResource(R.string.field_amount),
                    placeholder = stringResource(R.string.field_amount_hint),
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            TripLogTextField(
                value = state.odometerKm,
                onValueChange = viewModel::onOdometerChange,
                label = stringResource(R.string.field_odometer),
                placeholder = stringResource(R.string.field_odometer_hint),
                keyboardType = KeyboardType.Number
            )

            TripLogTextField(
                value = state.fuelStation,
                onValueChange = viewModel::onFuelStationChange,
                label = stringResource(R.string.field_fuel_station),
                placeholder = stringResource(R.string.field_fuel_station_hint)
            )

            // Receipt capture zone
            Column {
                FieldLabel(stringResource(R.string.field_receipt))

                if (state.receiptImagePath != null) {
                    // Show captured receipt thumbnail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(0.5.dp, GreenSync.copy(0.3f), RoundedCornerShape(16.dp))
                    ) {
                        AsyncImage(
                            model = File(state.receiptImagePath!!),
                            contentDescription = stringResource(R.string.receipt_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(GreenSync)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Surface900,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.receipt_captured), color = Surface900, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                } else {
                    // Camera capture button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Surface700)
                            .border(
                                0.5.dp,
                                Brand400.copy(alpha = 0.45f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                launchCamera()
                            }
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = Brand400,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(stringResource(R.string.receipt_tap_to_photograph), color = Brand400, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.receipt_opens_camera), color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            state.errorMessageRes?.let { errorRes ->
                Text(stringResource(errorRes), color = RedError, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = stringResource(R.string.action_save_expense),
                onClick = viewModel::saveExpense,
                isLoading = state.isSaving
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun createImageFile(context: Context): File {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
    return File.createTempFile("RECEIPT_${timestamp}_", ".jpg", storageDir)
}

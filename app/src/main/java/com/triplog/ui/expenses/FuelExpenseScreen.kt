package com.triplog.ui.expenses

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.triplog.ui.FieldLabel
import com.triplog.ui.PrimaryButton
import com.triplog.ui.TripLogTextField
import com.triplog.ui.TripLogTopBar
import com.triplog.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FuelExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: FuelExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val cameraPermission = rememberPermissionState(android.Manifest.permission.CAMERA)
    var tempImageFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempImageFile?.let { file -> viewModel.onReceiptCaptured(file.absolutePath) }
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
        TripLogTopBar(title = "Fuel expense", subtitle = "Add the receipt photo for your records", onBack = onNavigateBack)

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
                    label = "Litres",
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
                TripLogTextField(
                    value = state.amountRands,
                    onValueChange = viewModel::onAmountChange,
                    label = "Amount (R)",
                    placeholder = "0.00",
                    keyboardType = KeyboardType.Decimal,
                    modifier = Modifier.weight(1f)
                )
            }

            TripLogTextField(
                value = state.odometerKm,
                onValueChange = viewModel::onOdometerChange,
                label = "Odometer (km)",
                placeholder = "e.g. 84320",
                keyboardType = KeyboardType.Number
            )

            TripLogTextField(
                value = state.fuelStation,
                onValueChange = viewModel::onFuelStationChange,
                label = "Fuel station",
                placeholder = "e.g. Engen Sandton"
            )

            // Receipt capture zone
            Column {
                FieldLabel("Receipt photo")

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
                            contentDescription = "Receipt",
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
                                Text("Captured", color = Surface900, style = MaterialTheme.typography.labelSmall)
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
                                if (cameraPermission.status.isGranted) {
                                    val imageFile = createImageFile(context)
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        imageFile
                                    )
                                    tempImageFile = imageFile
                                    cameraLauncher.launch(uri)
                                } else {
                                    cameraPermission.launchPermissionRequest()
                                }
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
                            Text("Tap to photograph receipt", color = Brand400, style = MaterialTheme.typography.titleSmall)
                            Text("Opens your camera", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            state.errorMessage?.let { error ->
                Text(error, color = RedError, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(4.dp))

            PrimaryButton(
                text = "Save expense",
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

package com.triplog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.triplog.domain.model.Trip
import com.triplog.ui.theme.*
import com.triplog.util.formatKm
import java.time.format.DateTimeFormatter

private val CardShape = RoundedCornerShape(20.dp)
private val FieldShape = RoundedCornerShape(14.dp)
private val TripDateFormat = DateTimeFormatter.ofPattern("d MMM, HH:mm")

@Composable
fun TripLogTopBar(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Surface700)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Spacer(Modifier.width(14.dp))
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        }
    }
}

@Composable
fun SyncStatusChip(isSynced: Boolean) {
    val color = if (isSynced) GreenSync else Amber400
    val label = if (isSynced) "Synced" else "Pending"
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(Surface700)
            .border(1.dp, Surface600, CardShape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun TripCard(trip: Trip) {
    SurfaceCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = trip.purpose.label,
                style = MaterialTheme.typography.labelMedium,
                color = Brand400,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brand400.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
            SyncStatusChip(isSynced = trip.isSynced)
        }
        Spacer(Modifier.height(12.dp))
        RoutePoint(trip.fromLocation, filled = false)
        Box(
            Modifier
                .padding(start = 4.dp)
                .width(2.dp)
                .height(12.dp)
                .background(Surface500)
        )
        RoutePoint(trip.toLocation, filled = true)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                trip.dateTime.format(TripDateFormat),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                "${formatKm(trip.distanceKm)} km",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun RoutePoint(label: String, filled: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(
                    if (filled) Modifier.background(Brand400)
                    else Modifier.border(2.dp, TextTertiary, CircleShape)
                )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand400,
    unfocusedBorderColor = Surface500,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = Brand400,
    focusedContainerColor = Surface700,
    unfocusedContainerColor = Surface700,
    focusedPlaceholderColor = TextTertiary,
    unfocusedPlaceholderColor = TextTertiary
)

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = TextSecondary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun TripLogTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    maxLines: Int = 1
) {
    Column(modifier = modifier) {
        FieldLabel(label)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyLarge) },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            modifier = Modifier.fillMaxWidth(),
            shape = FieldShape,
            colors = fieldColors()
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Brand500,
            contentColor = Color.White,
            disabledContainerColor = Surface600,
            disabledContentColor = TextTertiary
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge)
        }
    }
}

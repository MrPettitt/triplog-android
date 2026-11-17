package com.triplog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.triplog.R
import com.triplog.domain.model.TripPurpose
import com.triplog.ui.theme.Brand400
import com.triplog.ui.theme.Surface500
import com.triplog.ui.theme.Surface700
import com.triplog.ui.theme.TextPrimary
import com.triplog.ui.theme.TextSecondary
import com.triplog.util.DateRange

/** Search box with a clear button. Filtering happens as you type, so no search button is needed. */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(hint, style = MaterialTheme.typography.bodyLarge) },
        textStyle = MaterialTheme.typography.bodyLarge,
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = TextSecondary
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = fieldColors()
    )
}

/** A horizontally scrolling row of single-select chips. */
@Composable
fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(options) { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option), style = MaterialTheme.typography.labelLarge) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Surface700,
                    labelColor = TextPrimary,
                    selectedContainerColor = Brand400.copy(alpha = 0.22f),
                    selectedLabelColor = Brand400
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = option == selected,
                    borderColor = Surface500,
                    selectedBorderColor = Brand400
                )
            )
        }
    }
}

@Composable
fun dateRangeLabel(range: DateRange): String = stringResource(
    when (range) {
        DateRange.ALL -> R.string.range_all
        DateRange.THIS_MONTH -> R.string.range_this_month
        DateRange.LAST_30_DAYS -> R.string.range_last_30_days
        DateRange.THIS_YEAR -> R.string.range_this_year
    }
)

/** Label for the purpose chips, where null means "any purpose". */
@Composable
fun purposeChipLabel(purpose: TripPurpose?): String =
    if (purpose == null) stringResource(R.string.filter_any_purpose) else stringResource(purpose.labelRes)

package com.triplog.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.triplog.R
import com.triplog.ui.theme.Surface700
import com.triplog.ui.theme.TextPrimary
import com.triplog.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Round share button for the top bar. [buildCsv] runs when it is tapped. */
@Composable
fun ExportCsvButton(
    enabled: Boolean,
    fileName: String,
    contentDescription: String,
    buildCsv: () -> String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val chooserTitle = stringResource(R.string.export_chooser_title)
    val failedMessage = stringResource(R.string.export_failed)

    IconButton(
        onClick = {
            scope.launch {
                if (!shareCsv(context, fileName, buildCsv(), chooserTitle)) {
                    Toast.makeText(context, failedMessage, Toast.LENGTH_SHORT).show()
                }
            }
        },
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(Surface700)
    ) {
        Icon(
            Icons.Default.Share,
            contentDescription = contentDescription,
            tint = if (enabled) TextPrimary else TextTertiary
        )
    }
}

/**
 * Writes [content] to a CSV file in the app cache and opens the system share sheet for it. The
 * user decides where it goes; the app itself never sends anything anywhere. Returns false if the
 * file could not be written or no app can handle it.
 */
suspend fun shareCsv(context: Context, fileName: String, content: String, chooserTitle: String): Boolean {
    val uri = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, "exports").apply { mkdirs() }
            // Keep only the latest export in the cache.
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, fileName)
            file.writeText(content, Charsets.UTF_8)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrNull()
    } ?: return false

    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return try {
        context.startActivity(Intent.createChooser(send, chooserTitle))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

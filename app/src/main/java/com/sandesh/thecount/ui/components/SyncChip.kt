package com.sandesh.thecount.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material.icons.filled.WifiOff
import com.sandesh.thecount.data.sync.SyncState

/**
 * Small status chip for the Home screen. Shows one of four states:
 * - Synced (green): all rows are on the server.
 * - Pending (orange): N rows still need to be pushed.
 * - Error (red): at least one row hit a permanent Postgres error.
 * - Offline (grey): no network right now.
 */
@Composable
fun SyncChip(
    state: SyncState,
    modifier: Modifier = Modifier
) {
    // Triple avoids the broken nested `to` destructuring (A to B to C is Pair<Pair,C>, not 3 values).
    val (label, icon, color) = when (state) {
        is SyncState.Synced  -> Triple<String, ImageVector, androidx.compose.ui.graphics.Color>(
            "Synced", Icons.Default.CloudDone, MaterialTheme.colorScheme.tertiary)
        is SyncState.Pending -> Triple<String, ImageVector, androidx.compose.ui.graphics.Color>(
            "${state.count} pending", Icons.Default.CloudQueue, MaterialTheme.colorScheme.secondary)
        is SyncState.Error   -> Triple<String, ImageVector, androidx.compose.ui.graphics.Color>(
            "Sync error", Icons.Default.SyncProblem, MaterialTheme.colorScheme.error)
        is SyncState.Offline -> Triple<String, ImageVector, androidx.compose.ui.graphics.Color>(
            "Offline", Icons.Default.WifiOff, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    AssistChip(
        onClick = { },
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.12f),
            labelColor = color
        ),
        modifier = modifier
    )
}

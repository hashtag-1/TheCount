package com.sandesh.thecount.ui.screens

import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.graphics.graphicsLayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.prefs.DisplayPreferences
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.BackupManager
import com.sandesh.thecount.data.repository.BackupMode
import com.sandesh.thecount.data.repository.ValidatedBackup
import com.sandesh.thecount.data.sync.SyncScheduler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: com.sandesh.thecount.ui.viewmodel.HomeViewModel? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val session = SupabaseProvider.client.auth.currentSessionOrNull()
    val userId = session?.user?.id
    val email = session?.user?.email

    val backupManager = remember {
        BackupManager(
            context.applicationContext,
            TheCountDatabase.getInstance(context.applicationContext)
        )
    }

    var showSignOutDialog by remember { mutableStateOf(false) }
    var backupToImport by remember { mutableStateOf<ValidatedBackup?>(null) }
    var backupMode by remember { mutableStateOf(BackupMode.MERGE) }
    var replaceText by remember { mutableStateOf("") }
    var showAccountWarning by remember { mutableStateOf(false) }
    var accountMismatchConfirmed by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    val prefs = remember { DisplayPreferences.getInstance(context) }
    val hide by prefs.hideCurrency.collectAsState()
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val workingDays = uiState?.allStrokes?.count { it.strokeCount > 0 } ?: 0

    // The Sync Now icon spins while a sync is running, and for at least 1.2 s
    // so even a very fast sync is visible.
    val syncWorkInfos by WorkManager.getInstance(context)
        .getWorkInfosForUniqueWorkFlow("thecount-sync-now")
        .collectAsState(initial = emptyList())
    var minSpin by remember { mutableStateOf(false) }
    val syncing = minSpin || syncWorkInfos.any { it.state == WorkInfo.State.RUNNING }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && userId != null) {
            scope.launch {
                busy = true
                try {
                    backupManager.exportTo(uri, userId)
                    statusMessage = "Backup exported successfully."
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Backup export failed."
                } finally {
                    busy = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                backupToImport = backupManager.validateBackup(uri)
                backupMode = BackupMode.MERGE
                replaceText = ""
                accountMismatchConfirmed = false
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = e.message ?: "Backup validation failed."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (session != null && !email.isNullOrBlank() && userId != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Signed in as", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(email, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Backup and Recovery", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            IconButton(onClick = { expanded = !expanded }) {
                                Icon(
                                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = if (expanded) "Collapse" else "Expand"
                                )
                            }
                        }
                        if (expanded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            TextButton(
                                onClick = { exportLauncher.launch("TheCount-backup.json") },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Export Backup", style = MaterialTheme.typography.bodyLarge) }
                            TextButton(
                                onClick = {
                                    importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream"))
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) { Text("Import Backup", style = MaterialTheme.typography.bodyLarge) }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Working Days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { contentDescription = "Working days, $workingDays" })
                        Text(workingDays.toString(), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {  },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(16.dp)
                            .semantics { stateDescription = if (hide) "On" else "Off" }
                            .toggleable(value = hide, role = Role.Switch, onValueChange = { prefs.setHideCurrency(it) }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Home Screen Privacy", style = MaterialTheme.typography.titleMedium)
                            Text("Show XXXXX instead of amounts and cumulative strokes on Home", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = hide, onCheckedChange = null)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilledTonalButton(
                        onClick = {
                            SyncScheduler.syncNow(context)
                            scope.launch {
                                minSpin = true
                                delay(1200)
                                minSpin = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        SyncIcon(spinning = syncing)
                        Text("Sync Now", maxLines = 2)
                    }
                    OutlinedButton(
                        onClick = { showSignOutDialog = true },
                        enabled = !busy,
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) {
                        Text("Sign Out", maxLines = 2, color = MaterialTheme.colorScheme.error)
                    }
                }

                if (busy) {
                    Text("Please wait…")
                }
                statusMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Text("Sign in to sync your data across devices.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = onLoginClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Sign in with Google")
                }
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = { Text("Are you sure you want to sign out?") },
            confirmButton = {
                TextButton(onClick = { showSignOutDialog = false; onSignOut() }) { Text("Sign out") }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Basic backup import confirmation dialog.
    backupToImport?.let { backup ->
        AlertDialog(
            onDismissRequest = { backupToImport = null; accountMismatchConfirmed = false },
            title = { Text("Review backup") },
            text = { Text("Import?") },
            confirmButton = {
                TextButton(onClick = {
                    if (userId == null) { errorMessage = "Sign in before importing."; backupToImport = null }
                    else {
                        scope.launch {
                            busy = true
                            try {
                                backupManager.importBackup(backup, userId, backupMode, accountMismatchConfirmed, replaceText)
                                SyncScheduler.syncNow(context)
                                statusMessage = "Import complete. Sync has been requested."
                                backupToImport = null
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Backup import failed."
                            } finally { busy = false }
                        }
                    }
                }) { Text("Import") }
            },
            dismissButton = { TextButton(onClick = { backupToImport = null; accountMismatchConfirmed = false }) { Text("Cancel") } }
        )
    }
}

// Refresh icon that rotates while a sync is running. The angle is calculated from the
// frame clock, so it keeps spinning even when the phone's animation setting is turned off.
@Composable
private fun SyncIcon(spinning: Boolean) {
    if (spinning) {
        val angle = remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            val start = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    val elapsedMs = (now - start) / 1_000_000f
                    angle.value = (elapsedMs % 900f) / 900f * 360f
                }
            }
        }
        Icon(
            Icons.Filled.Refresh,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 4.dp)
                .graphicsLayer { rotationZ = angle.value }
        )
    } else {
        Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
    }
}
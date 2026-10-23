
package com.sandesh.thecount.ui.screens

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.BackupManager
import com.sandesh.thecount.data.repository.BackupMode
import com.sandesh.thecount.data.repository.ValidatedBackup
import com.sandesh.thecount.data.sync.SyncScheduler
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit,
    onSignOut: () -> Unit
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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (session != null && !email.isNullOrBlank() && userId != null) {
                Text("Signed in as", style = MaterialTheme.typography.bodyMedium)
                Text(email, style = MaterialTheme.typography.bodyLarge)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "Backup and recovery",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Save your strokes and payments to a JSON file. " +
                                "Keep a copy outside your phone."
                        )

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            onClick = {
                                exportLauncher.launch("TheCount-backup.json")
                            }
                        ) {
                            Text("Export backup")
                        }

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                            onClick = {
                                importLauncher.launch(
                                    arrayOf(
                                        "application/json",
                                        "text/json",
                                        "application/octet-stream"
                                    )
                                )
                            }
                        ) {
                            Text("Import backup")
                        }
                    }
                }

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    onClick = { SyncScheduler.syncNow(context) }
                ) {
                    Text("Sync now")
                }

                TextButton(
                    enabled = !busy,
                    onClick = { showSignOutDialog = true }
                ) {
                    Text("Sign out", fontSize = 16.sp)
                }
            } else {
                TextButton(onClick = onLoginClick) {
                    Text("Sign in with Google")
                }
            }

            if (busy) {
                Text("Please wait…")
            }

            statusMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
            }

            errorMessage?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text = {
                Text("Are you sure you want to sign out?")
            },
            confirmButton = {
                TextButton(onClick = {
                    showSignOutDialog = false
                    onSignOut()
                }) {
                    Text("Sign out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    backupToImport?.let { backup ->
        AlertDialog(
            onDismissRequest = {
                backupToImport = null
                accountMismatchConfirmed = false
            },
            title = { Text("Review backup") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("The file passed validation.")
                    Text("Strokes: ${backup.summary.strokeCount}")
                    Text("Payments: ${backup.summary.paymentCount}")
                    Text(
                        "Date range: " +
                            "${backup.summary.firstDate ?: "None"} to " +
                            "${backup.summary.lastDate ?: "None"}"
                    )

                    Text(
                        "Backup account: ${backup.summary.sourceAccountId}"
                    )

                    Text("Choose import mode:")
                    Column {
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = backupMode == BackupMode.MERGE,
                                onClick = { backupMode = BackupMode.MERGE }
                            )
                            Text("MERGE — add missing records; keep existing")
                        }
                        androidx.compose.foundation.layout.Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = backupMode == BackupMode.REPLACE,
                                onClick = { backupMode = BackupMode.REPLACE }
                            )
                            Text("REPLACE — replace this account's local data")
                        }
                    }

                    if (backupMode == BackupMode.REPLACE) {
                        Text(
                            "A safety copy will be created before replacement. " +
                                "Type REPLACE to confirm."
                        )
                        OutlinedTextField(
                            value = replaceText,
                            onValueChange = { replaceText = it },
                            label = { Text("Type REPLACE") },
                            singleLine = true
                        )
                    }

                    if (backup.summary.sourceAccountId != userId) {
                        Text(
                            "WARNING: This backup belongs to another account. " +
                                "Imported rows will belong to your currently " +
                                "signed-in account.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !busy &&
                        (backupMode != BackupMode.REPLACE ||
                            replaceText == "REPLACE"),
                    onClick = {
                        if (userId == null) {
                            errorMessage = "Sign in before importing."
                            backupToImport = null
                        } else if (
                            backup.summary.sourceAccountId != userId &&
                            !accountMismatchConfirmed
                        ) {
                            showAccountWarning = true
                        } else {
                            scope.launch {
                                busy = true
                                try {
                                    backupManager.importBackup(
                                        backup = backup,
                                        currentUserId = userId,
                                        mode = backupMode,
                                        confirmedAccountMismatch =
                                            accountMismatchConfirmed,
                                        replaceConfirmation = replaceText
                                    )
                                    SyncScheduler.syncNow(context)
                                    statusMessage =
                                        "Import complete. Sync has been requested."
                                    backupToImport = null
                                } catch (e: Exception) {
                                    errorMessage =
                                        e.message ?: "Backup import failed."
                                } finally {
                                    busy = false
                                }
                            }
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    backupToImport = null
                    accountMismatchConfirmed = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAccountWarning) {
        AlertDialog(
            onDismissRequest = { showAccountWarning = false },
            title = { Text("Different account") },
            text = {
                Text(
                    "This backup was exported from another account. " +
                        "Continue only if you want its records imported " +
                        "under the currently signed-in account."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    accountMismatchConfirmed = true
                    showAccountWarning = false
                }) {
                    Text("I understand")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountWarning = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

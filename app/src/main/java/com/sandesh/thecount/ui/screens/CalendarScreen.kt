package com.sandesh.thecount.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sandesh.thecount.domain.Validators
import com.sandesh.thecount.domain.WorkConfig
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlinx.coroutines.delay

private val countFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    // Kept so the navigation code does not have to change. Rows no longer open another screen.
    onDateClick: (LocalDate) -> Unit = {},
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf<LocalDate?>(null) }

    // Only ONE row at a time turns into an editor. All other rows are plain text, which is
    // what keeps scrolling smooth.
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }

    // The day that was just saved (shows a green check for a moment). One timer for the screen.
    var savedFlashDate by remember { mutableStateOf<LocalDate?>(null) }
    LaunchedEffect(savedFlashDate) {
        if (savedFlashDate != null) {
            delay(1200)
            savedFlashDate = null
        }
    }

    // Do not show an old error from another screen.
    LaunchedEffect(Unit) { viewModel.clearError() }

    // Back closes the open editor first (without saving), then leaves the screen.
    BackHandler(enabled = editingDate != null) { editingDate = null }

    val today = LocalDate.now()
    val dates = remember(today) {
        generateSequence(WorkConfig.START_DATE) { it.plusDays(1) }
            .takeWhile { !it.isAfter(today) }
            .toList()
            .reversed()
    }
    val countByDate = remember(uiState.allStrokes) {
        uiState.allStrokes.associate { it.workDate to it.strokeCount }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("History & Edit") },
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
                .fillMaxSize()
                .padding(padding)
                .imePadding()   // keeps the row you are typing in above the keyboard
        ) {
            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = dates,
                    key = { it.toEpochDay() },
                    contentType = { if (it == editingDate) "edit" else "day" }
                ) { date ->
                    if (date == editingDate) {
                        EditRow(
                            date = date,
                            savedCount = countByDate[date],
                            onSave = { text -> viewModel.saveForDate(date, text) },
                            onSaved = {
                                editingDate = null
                                savedFlashDate = date
                            },
                            onDelete = { showDeleteConfirm = date }
                        )
                    } else {
                        DayRow(
                            date = date,
                            savedCount = countByDate[date],
                            justSaved = savedFlashDate == date,
                            onClick = {
                                viewModel.clearError()
                                editingDate = date
                            }
                        )
                    }
                }
            }
        }

        showDeleteConfirm?.let { dateToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("Delete entry") },
                text = { Text("Delete entry for $dateToDelete?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.delete(dateToDelete)
                        showDeleteConfirm = null
                        editingDate = null
                    }) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteConfirm = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

// A normal row: just the date and the saved count. Tap it to edit.
@Composable
private fun DayRow(
    date: LocalDate,
    savedCount: Long?,
    justSaved: Boolean,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (justSaved) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Saved",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = savedCount?.let { countFormat.format(it) } ?: "No entry",
                style = MaterialTheme.typography.bodyLarge,
                color = if (savedCount == null) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// The one open editor: [date] [stroke input] [Update] [delete]
// Saving replaces that day's count with the typed number (it never adds to it).
// The saved number is shown in grey inside the empty field. Update works once a number is typed.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun EditRow(
    date: LocalDate,
    savedCount: Long?,
    onSave: (String) -> Unit,
    onSaved: () -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Focus the field and open the keyboard as soon as the row opens.
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    val submit = {
        if (text.isNotBlank()) {
            val valid = Validators.validateStrokeCount(text).isSuccess
            onSave(text)
            if (valid) {          // on an invalid number the row stays open so you can fix it
                focusManager.clearFocus()
                onSaved()
            }
        }
    }

    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(8.dp)

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = date.toString(),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(76.dp)
            )

            BasicTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .focusRequester(focusRequester),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .border(1.dp, borderColor, shape)
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text(
                                text = savedCount?.toString() ?: "No entry",
                                style = MaterialTheme.typography.bodyMedium,
                                color = hintColor
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Button(
                onClick = submit,
                enabled = text.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 10.dp)
            ) {
                Text("Update", style = MaterialTheme.typography.labelMedium)
            }

            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                if (savedCount != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                    }
                }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}

package com.sandesh.thecount.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sandesh.thecount.ui.components.AppCard
import com.sandesh.thecount.ui.components.AppScaffold
import com.sandesh.thecount.ui.components.ErrorBanner
import com.sandesh.thecount.ui.theme.Spacing
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel,
    date: LocalDate = LocalDate.now()
) {
    val uiState by viewModel.uiState.collectAsState()
    val error by viewModel.error.collectAsState()
    var countText by remember { mutableStateOf("") }
    val count = if (date == LocalDate.now()) uiState.todayCount else uiState.selectedDateCount
    val isToday = date == LocalDate.now()

    AppScaffold(title = if (isToday) "Daily Entry - Today" else "Daily Entry - $date", onBack = onBack) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text = date.toString(), style = MaterialTheme.typography.titleMedium)
                    Text(text = "Saved count for this date: ${count ?: "No entry"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = countText,
                onValueChange = { countText = it },
                label = { Text("Stroke count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = MaterialTheme.typography.headlineSmall,
                supportingText = { if (error != null) Text(text = error ?: "", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (error != null) ErrorBanner(message = error ?: "")
            Button(onClick = { viewModel.saveForDate(date, countText); countText = "" }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (count == null) "Save" else "Update")
            }
        }
    }
}

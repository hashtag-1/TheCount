package com.sandesh.thecount.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isToday) "Daily Entry - Today" else "Daily Entry - ${date}") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = countText,
                onValueChange = { countText = it },
                label = { Text("Stroke count") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(text = error ?: "", color = androidx.compose.material3.MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = {
                    viewModel.saveForDate(date, countText)
                    countText = ""
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (count == null) "Save" else "Update")
            }
            Text("Saved count for this date: ${count ?: "No entry"}")
        }
    }
}

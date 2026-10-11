package com.sandesh.thecount.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel? = null
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val payments = uiState?.payments ?: emptyList()
    var showDelete by remember { mutableStateOf<UUID?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payments") },
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
                .padding(16.dp)
        ) {
            Text(
                text = "Total received: ${MoneyFormat.format(uiState?.paymentsTotalUpToSelected ?: java.math.BigDecimal.ZERO)}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(payments) { payment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "${payment.paymentDate}: ${MoneyFormat.format(payment.amount)}")
                                if (!payment.note.isNullOrBlank()) {
                                    Text(text = payment.note ?: "", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            OutlinedButton(onClick = { showDelete = payment.id }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }

        showDelete?.let { id ->
            AlertDialog(
                onDismissRequest = { showDelete = null },
                title = { Text("Delete payment") },
                text = { Text("Delete this payment?") },
                confirmButton = {
                    Button(onClick = {
                        viewModel?.deletePayment(id)
                        showDelete = null
                    }) { Text("Delete") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}

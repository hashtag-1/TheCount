package com.sandesh.thecount.ui.screens

import io.github.jan.supabase.auth.auth
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onEntryClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onEarningsClick: () -> Unit,
    onPaymentsClick: () -> Unit,
    onAddPaymentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val earnings = uiState.earnings
    val outstanding = earnings?.outstanding ?: BigDecimal.ZERO
    val session = SupabaseProvider.client.auth.currentSessionOrNull()
    val accountEmail = session?.user?.email ?: "Not signed in"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TheCount") },
                actions = {
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = LocalDate.now().toString(), style = MaterialTheme.typography.bodyMedium)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = accountEmail, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Synced", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEntryClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "1. Daily Entry (Today)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = uiState.todayCount?.toString() ?: "No entry", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCalendarClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "2. History & Edit", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${uiState.selectedDate}: ${uiState.selectedDateCount?.toString() ?: "No entry"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "3. Cumulative Strokes", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = numberFormat.format(uiState.cumulativeStrokes), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEarningsClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "4. Earnings", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = earnings?.net?.let { MoneyFormat.format(it) } ?: "-",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPaymentsClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "5. Payments Received", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = MoneyFormat.format(uiState.paymentsTotalUpToSelected), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "+ Add payment", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddPaymentClick() },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "6. Outstanding Balance", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        val status = if (outstanding.signum() == 0) "Fully settled" else if (outstanding.signum() < 0) "Overpaid / credit" else "Still owed"
                        Text(text = "${MoneyFormat.format(outstanding)} - $status", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
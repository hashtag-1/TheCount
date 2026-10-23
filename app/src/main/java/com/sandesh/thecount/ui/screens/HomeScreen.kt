package com.sandesh.thecount.ui.screens

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
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.SyncRepository
import com.sandesh.thecount.data.sync.SyncState
import com.sandesh.thecount.ui.components.SyncChip
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import io.github.jan.supabase.auth.auth
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
    viewModel: HomeViewModel,
    syncRepository: SyncRepository? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    val earnings = uiState.earnings
    val outstanding = earnings?.outstanding ?: BigDecimal.ZERO
    val session = SupabaseProvider.client.auth.currentSessionOrNull()
    val accountEmail = session?.user?.email ?: "Not signed in"

    val syncState: SyncState = if (syncRepository == null) {
        SyncState.Synced
    } else {
        syncRepository.syncState().collectAsStateWithLifecycle(initialValue = SyncState.Synced).value
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TheCount", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
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
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(text = LocalDate.now().toString(), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        SyncChip(state = syncState)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = accountEmail, style = MaterialTheme.typography.bodySmall)
                        Text(text = "Synced", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item { StyledCard(onClick = onEntryClick) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Daily Entry (Today)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = uiState.todayCount?.toString() ?: "No entry", style = MaterialTheme.typography.bodyMedium)
                }
            }}

            item { StyledCard(onClick = onCalendarClick) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "History & Edit", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${uiState.selectedDate}: ${uiState.selectedDateCount?.toString() ?: "No entry"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }}

            item { StyledCard() {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Cumulative Strokes", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = numberFormat.format(uiState.cumulativeStrokes), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                }
            }}

            item { StyledCard(onClick = onEarningsClick) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Earnings", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = earnings?.net?.let { MoneyFormat.format(it) } ?: "-",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }}

            item { StyledCard(onClick = onPaymentsClick) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Payments Received", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = MoneyFormat.format(uiState.paymentsTotalUpToSelected), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(onClick = onAddPaymentClick) {
                        Text("+ Add payment", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }}

            item { StyledCard() {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Outstanding Balance", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    val text: String
                    val color: Color
                    when {
                        outstanding.signum() == 0 -> { text = MoneyFormat.format(outstanding) + " - Fully settled"; color = MaterialTheme.colorScheme.tertiary }
                        outstanding.signum() < 0  -> { text = MoneyFormat.format(outstanding) + " - Overpaid / credit"; color = MaterialTheme.colorScheme.onSecondaryContainer }
                        else                       -> { text = MoneyFormat.format(outstanding); color = MaterialTheme.colorScheme.onSurface }
                    }
                    Text(
                        text = text,
                        style = MaterialTheme.typography.headlineSmall,
                        color = color
                    )
                }
            }}
        }
    }
}

@Composable
private fun StyledCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        content()
    }
}

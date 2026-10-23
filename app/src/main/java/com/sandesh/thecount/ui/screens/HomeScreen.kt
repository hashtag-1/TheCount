package com.sandesh.thecount.ui.screens
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.flowOf
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.SyncRepository
import com.sandesh.thecount.data.sync.SyncState
import com.sandesh.thecount.ui.components.AppCard
import com.sandesh.thecount.ui.components.InfoChip
import com.sandesh.thecount.ui.components.LabelValue
import com.sandesh.thecount.ui.theme.Spacing
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import com.sandesh.thecount.util.MoneyMask
import com.sandesh.thecount.data.prefs.DisplayPreferences
import io.github.jan.supabase.auth.auth
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

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
    val context = LocalContext.current
    val prefs = remember { DisplayPreferences.getInstance(context) }
    val hide by prefs.hideCurrency.collectAsState()
    val outstanding = earnings?.outstanding ?: BigDecimal.ZERO
    val session = SupabaseProvider.client.auth.currentSessionOrNull()
    val accountEmail = session?.user?.email ?: "Not signed in"
    val notSignedIn = session?.user?.email == null
    val syncFlow = remember(syncRepository) {
        syncRepository?.syncState() ?: flowOf<SyncState>(SyncState.Synced)
    }
    val syncState by syncFlow.collectAsStateWithLifecycle(initialValue = SyncState.Synced)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TheCount", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary) },
                navigationIcon = { IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, contentDescription = "Settings") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = LocalDate.now().toString(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(horizontalAlignment = Alignment.End) {
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                            InfoChip(text = if (notSignedIn) "Not signed in" else accountEmail, tone = if (notSignedIn) com.sandesh.thecount.ui.components.ChipTone.Warning else com.sandesh.thecount.ui.components.ChipTone.Neutral)
                            val state = syncState
                            InfoChip(
                                text = when (state) {
                                    is SyncState.Synced -> "Synced"
                                    is SyncState.Pending -> "${state.count} pending"
                                    is SyncState.Error -> "Sync error"
                                    is SyncState.Offline -> "Offline"
                                },
                                tone = when (state) {
                                    is SyncState.Synced -> com.sandesh.thecount.ui.components.ChipTone.Positive
                                    is SyncState.Pending -> com.sandesh.thecount.ui.components.ChipTone.Warning
                                    is SyncState.Error -> com.sandesh.thecount.ui.components.ChipTone.Error
                                    is SyncState.Offline -> com.sandesh.thecount.ui.components.ChipTone.Neutral
                                }
                            )
                        }
                    }
                }
            }
            item {
                Card(onClick = onEarningsClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                    Column(modifier = Modifier.padding(Spacing.lg)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Earnings", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(text = MoneyMask.display(hide) { earnings?.net?.let { MoneyFormat.format(it) } ?: "-" }, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.semantics { if (hide) contentDescription = "Amount hidden" })
                    }
                }
            }
            item {
                val fontScale = LocalDensity.current.fontScale
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val shouldStack = maxWidth < 360.dp || fontScale > 1.15f
                    if (shouldStack) {
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            AppCard(onClick = onPaymentsClick, modifier = Modifier.fillMaxWidth()) { LabelValue(label = "Payments Received", value = MoneyMask.display(hide) { MoneyFormat.format(uiState.paymentsTotalUpToSelected) }, valueStyle = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { if (hide) contentDescription = "Amount hidden" }) }
                            AppCard(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    Text(text = "Outstanding Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    when {
                                        hide -> { Text(text = MoneyMask.HIDDEN, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { contentDescription = "Amount hidden" }) }
                                        else -> { Text(text = MoneyMask.display(hide) { MoneyFormat.format(outstanding) }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { if (hide) contentDescription = "Amount hidden" }) }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            AppCard(onClick = onPaymentsClick, modifier = Modifier.weight(1f).fillMaxHeight()) { LabelValue(label = "Payments Received", value = MoneyMask.display(hide) { MoneyFormat.format(uiState.paymentsTotalUpToSelected) }, valueStyle = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { if (hide) contentDescription = "Amount hidden" }) }
                            AppCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                Column {
                                    Text(text = "Outstanding Balance", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(Spacing.xs))
                                    when {
                                        hide -> { Text(text = MoneyMask.HIDDEN, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { contentDescription = "Amount hidden" }) }
                                        else -> { Text(text = MoneyMask.display(hide) { MoneyFormat.format(outstanding) }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.semantics { if (hide) contentDescription = "Amount hidden" }) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onAddPaymentClick, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("+ Add payment") }
            }
            item {
                AppCard(modifier = Modifier.fillMaxWidth()) { LabelValue(label = "Cumulative Strokes", value = numberFormat.format(uiState.cumulativeStrokes), valueStyle = MaterialTheme.typography.headlineMedium, valueColor = MaterialTheme.colorScheme.primary) }
            }
            item {
                AppCard(onClick = onEntryClick, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Daily Entry (Today)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(text = uiState.todayCount?.toString() ?: "No entry", style = if (uiState.todayCount != null) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge, color = if (uiState.todayCount != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
            item {
                AppCard(onClick = onCalendarClick, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "History & Edit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(text = "${uiState.selectedDate}: ${uiState.selectedDateCount?.toString() ?: "No entry"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null)
                    }
                }
            }
        }
    }
}


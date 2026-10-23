package com.sandesh.thecount.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.sandesh.thecount.data.prefs.DisplayPreferences
import com.sandesh.thecount.ui.components.AppCard
import com.sandesh.thecount.ui.theme.Spacing
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import com.sandesh.thecount.util.MoneyMask

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel? = null
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val earnings = uiState?.earnings
    val context = LocalContext.current
    val prefs = remember { DisplayPreferences.getInstance(context) }
    val hide by prefs.hideCurrency.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings") },
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
                .padding(Spacing.lg)
        ) {
            AppCard(modifier = Modifier.fillMaxWidth()) {
                EarningsRow(
                    label = "Total Earning",
                    value = MoneyMask.display(hide) { earnings?.gross?.let { MoneyFormat.format(it) } ?: "-" },
                    hidden = hide
                )
                EarningsRow(
                    label = "Tax (1%)",
                    value = MoneyMask.display(hide) { earnings?.tax?.let { MoneyFormat.format(it) } ?: "-" },
                    hidden = hide
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.xs)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                EarningsRow(
                    label = "Receivable Income",
                    value = MoneyMask.display(hide) { earnings?.net?.let { MoneyFormat.format(it) } ?: "-" },
                    hidden = hide,
                    emphasized = true
                )
            }
        }
    }
}

@Composable
private fun EarningsRow(
    label: String,
    value: String,
    hidden: Boolean,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emphasized) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.maskedIf(hidden),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { if (hidden) contentDescription = "Amount hidden" }
            )
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.maskedIf(hidden),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { if (hidden) contentDescription = "Amount hidden" }
            )
        }
    }
}

// When privacy mode is on, the XXXXX placeholder is drawn in a smaller font than the real number.
private fun TextStyle.maskedIf(hide: Boolean): TextStyle =
    if (hide && fontSize.isSpecified) copy(fontSize = fontSize * 0.75f) else this
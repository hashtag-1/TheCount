package com.sandesh.thecount.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import com.sandesh.thecount.util.MoneyFormat
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    onBack: () -> Unit,
    viewModel: HomeViewModel? = null
) {
    val uiState = viewModel?.uiState?.collectAsState()?.value
    val earnings = uiState?.earnings

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "Selected date: ${uiState?.selectedDate ?: "-"}")
            Text(text = "4.1 Total Earning: ${earnings?.gross?.let { MoneyFormat.format(it) } ?: "-"}")
            Text(text = "4.2 Tax = 1%: ${earnings?.tax?.let { MoneyFormat.format(it) } ?: "-"}")
            Text(text = "4.3 Receivable Income: ${earnings?.net?.let { MoneyFormat.format(it) } ?: "-"} (payments not included yet)")
        }
    }
}

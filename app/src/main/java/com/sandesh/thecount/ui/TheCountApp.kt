package com.sandesh.thecount.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandesh.thecount.ui.navigation.Routes
import com.sandesh.thecount.ui.screens.*
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import java.time.LocalDate

@Composable
fun TheCountApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factory = ViewModelFactory(context.applicationContext as android.app.Application)
    val viewModel: HomeViewModel = viewModel(factory = factory)
    var selectedDateForEntry by remember { mutableStateOf(LocalDate.now()) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavHost(navController = navController, viewModel = viewModel) { date ->
            selectedDateForEntry = date
            navController.navigate(Routes.ENTRY)
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: HomeViewModel,
    onDateSelectedForEntry: (LocalDate) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onEntryClick = {
                    onDateSelectedForEntry(LocalDate.now())
                },
                onCalendarClick = { navController.navigate(Routes.CALENDAR) },
                onEarningsClick = { navController.navigate(Routes.EARNINGS) },
                onPaymentsClick = { navController.navigate(Routes.PAYMENTS) },
                onAddPaymentClick = { navController.navigate(Routes.ADD_PAYMENT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                viewModel = viewModel
            )
        }
        composable(Routes.ENTRY) {
            EntryScreen(
                onBack = { navController.popBackStack() },
                viewModel = viewModel,
                date = java.time.LocalDate.now() // Will be updated via param
            )
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(
                onBack = { navController.popBackStack() },
                onDateClick = { date ->
                    viewModel.setSelectedDate(date)
                    navController.navigate(Routes.ENTRY)
                },
                viewModel = viewModel
            )
        }
        composable(Routes.EARNINGS) {
            EarningsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PAYMENTS) {
            PaymentsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ADD_PAYMENT) {
            AddPaymentScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(Routes.LOGIN) }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ACCESS_DENIED) {
            AccessDeniedScreen(onBack = { navController.popBackStack() })
        }
    }
}

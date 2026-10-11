package com.sandesh.thecount.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandesh.thecount.ui.navigation.Routes
import com.sandesh.thecount.ui.screens.*

@Composable
fun TheCountApp() {
    val navController = rememberNavController()
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        AppNavHost(navController = navController)
    }
}

@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onEntryClick = { navController.navigate(Routes.ENTRY) },
                onCalendarClick = { navController.navigate(Routes.CALENDAR) },
                onEarningsClick = { navController.navigate(Routes.EARNINGS) },
                onPaymentsClick = { navController.navigate(Routes.PAYMENTS) },
                onAddPaymentClick = { navController.navigate(Routes.ADD_PAYMENT) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) }
            )
        }
        composable(Routes.ENTRY) {
            EntryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CALENDAR) {
            CalendarScreen(onBack = { navController.popBackStack() })
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

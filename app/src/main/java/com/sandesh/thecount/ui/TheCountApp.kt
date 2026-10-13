package com.sandesh.thecount.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.AuthRepository
import com.sandesh.thecount.ui.navigation.Routes
import com.sandesh.thecount.ui.screens.*
import com.sandesh.thecount.ui.screens.auth.AccessDeniedScreen
import com.sandesh.thecount.ui.viewmodel.HomeViewModel
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun TheCountApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val factory = ViewModelFactory(context.applicationContext as android.app.Application)
    val viewModel: HomeViewModel = viewModel(factory = factory)
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()

    var sessionChecked by remember { mutableStateOf(false) }
    var hasValidSession by remember { mutableStateOf(false) }

    if (!sessionChecked) {
        scope.launch {
            val session = authRepository.currentSession()
            hasValidSession = session != null
            sessionChecked = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (!sessionChecked) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            AppNavHost(
                navController = navController,
                viewModel = viewModel,
                authRepository = authRepository,
                initialRoute = if (hasValidSession) Routes.HOME else Routes.LOGIN
            )
        }
    }
}

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: HomeViewModel,
    authRepository: AuthRepository,
    initialRoute: String
) {
    val scope = rememberCoroutineScope()

    NavHost(
        navController = navController,
        startDestination = initialRoute
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onEntryClick = { navController.navigate(Routes.ENTRY) },
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
                date = LocalDate.now()
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
            EarningsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable(Routes.PAYMENTS) {
            PaymentsScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable(Routes.ADD_PAYMENT) {
            AddPaymentScreen(onBack = { navController.popBackStack() }, viewModel = viewModel)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onSignOut = {
                    scope.launch {
                        authRepository.signOut()
                        navController.popBackStack(Routes.LOGIN, inclusive = true)
                    }
                }
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                authRepository = authRepository,
                onSignInSuccess = {
                    navController.popBackStack(Routes.HOME, inclusive = false)
                },
                onAccessDenied = {
                    navController.navigate(Routes.ACCESS_DENIED)
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.ACCESS_DENIED) {
            AccessDeniedScreen(onBack = { navController.popBackStack() })
        }
    }
}
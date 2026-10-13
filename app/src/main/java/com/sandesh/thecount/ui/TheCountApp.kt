package com.sandesh.thecount.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.repository.AuthRepository
import com.sandesh.thecount.data.repository.AttachManager
import com.sandesh.thecount.data.repository.AttachResult
import com.sandesh.thecount.data.repository.CurrentUserProvider
import com.sandesh.thecount.data.repository.SessionUserProvider
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.data.repository.PaymentRepository
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
    val authRepository = remember { AuthRepository() }
    val scope = rememberCoroutineScope()
    val userProvider = remember { SessionUserProvider(context.applicationContext as android.app.Application) }
    val factory = ViewModelFactory(context.applicationContext as android.app.Application, userProvider)
    val strokeRepository = remember { StrokeRepository(TheCountDatabase.getInstance(context.applicationContext as android.app.Application).dailyStrokeDao(), userProvider) }
    val paymentRepository = remember { PaymentRepository(TheCountDatabase.getInstance(context.applicationContext as android.app.Application).paymentDao(), userProvider) }
    val attachManager = remember { AttachManager(strokeRepository, paymentRepository) }

    // Observe userId flow to trigger attach prompt and key ViewModel
    val currentUserId by userProvider.currentUserIdFlow.collectAsState()

    var sessionChecked by remember { mutableStateOf(false) }
    var hasValidSession by remember { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var attachResult by remember { mutableStateOf<AttachResult?>(null) }
    var pendingUserId by remember { mutableStateOf<String?>(null) }
    var pendingStrokeCount by remember { mutableStateOf(0) }
    var pendingPaymentCount by remember { mutableStateOf(0) }

    // Session check - runs when currentUserId changes
    LaunchedEffect(currentUserId) {
        val session = authRepository.currentSession()
        val userId = session?.user?.id
        hasValidSession = session != null

        if (userId != null && !userProvider.wasPromptedForAccount(userId)) {
            val localStrokes = strokeRepository.countLocalStrokes()
            val localPayments = paymentRepository.countLocalPayments()
            if (localStrokes > 0 || localPayments > 0) {
                pendingUserId = userId
                pendingStrokeCount = localStrokes
                pendingPaymentCount = localPayments
                showAttachDialog = true
            } else {
                userProvider.markAccountPrompted(userId)
            }
        }

        sessionChecked = true
    }

    // HomeViewModel keyed by userId so a new user gets a fresh instance
    val viewModel: HomeViewModel = viewModel(
        key = currentUserId,
        factory = factory
    )

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

        // Attach dialog overlay
        if (showAttachDialog) {
            val strokes = pendingStrokeCount
            val payments = pendingPaymentCount
            val userId = pendingUserId ?: ""

            AlertDialog(
                onDismissRequest = { },
                title = { Text("Attach local data?") },
                text = {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "You have $strokes stroke entries and $payments payments stored locally on this device.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Attach them to this account ($userId)?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        scope.launch {
                            val result = attachManager.tryAttach(userId)
                            attachResult = result
                            showAttachDialog = false

                            when (result) {
                                is AttachResult.Attached -> {
                                    userProvider.markAccountPrompted(userId)
                                }
                                is AttachResult.Clash -> {
                                    // The clash dialog below shows the dates.
                                }
                                is AttachResult.NoLocalData -> {
                                    userProvider.markAccountPrompted(userId)
                                }
                                is AttachResult.AlreadyAttached -> {
                                    userProvider.markAccountPrompted(userId)
                                }
                            }
                        }
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAttachDialog = false
                        userProvider.markAccountPrompted(userId)
                    }) {
                        Text("No")
                    }
                }
            )
        }

        // Clash dialog overlay
        attachResult?.let { result ->
            if (result is AttachResult.Clash) {
                AlertDialog(
                    onDismissRequest = { attachResult = null },
                    title = { Text("Cannot attach - date conflict") },
                    text = {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "The following dates already have entries for this account:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = result.clashingDates.joinToString(", ") { it.toString() },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No data was attached. Local entries remain as 'local'.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            attachResult = null
                            pendingUserId?.let { userProvider.markAccountPrompted(it) }
                        }) {
                            Text("OK")
                        }
                    }
                )
            }
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
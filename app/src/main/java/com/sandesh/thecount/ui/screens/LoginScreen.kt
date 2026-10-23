package com.sandesh.thecount.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.sandesh.thecount.data.repository.AuthRepository
import com.sandesh.thecount.data.repository.AuthResult
import java.security.MessageDigest
import java.security.SecureRandom
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Random raw nonce (64 hex chars). This is what Supabase receives.
private fun generateRawNonce(): String {
    val bytes = ByteArray(32)
    SecureRandom().nextBytes(bytes)
    return bytes.joinToString("") { "%02x".format(it) }
}

// SHA-256 as lowercase hex. This is what Google receives.
private fun sha256Hex(input: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authRepository: AuthRepository,
    onSignInSuccess: () -> Unit,
    onAccessDenied: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Login") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                // Fresh raw nonce for every attempt.
                                val rawNonce = generateRawNonce()
                                val hashedNonce = sha256Hex(rawNonce)

                                val credentialManager = CredentialManager.create(context)
                                val googleIdOption = GetGoogleIdOption.Builder()
                                    .setFilterByAuthorizedAccounts(false)
                                    .setServerClientId(authRepository.getGoogleWebClientId())
                                    .setNonce(hashedNonce) // Google gets the HASHED nonce
                                    .build()
                                val request = GetCredentialRequest.Builder()
                                    .addCredentialOption(googleIdOption)
                                    .build()
                                val result = credentialManager.getCredential(context, request)
                                val googleIdTokenCredential =
                                    GoogleIdTokenCredential.createFrom(result.credential.data)
                                val idToken = googleIdTokenCredential.idToken

                                // Supabase gets the RAW nonce
                                when (val res = authRepository.signInWithGoogle(context, idToken, rawNonce)) {
                                    AuthResult.Success -> onSignInSuccess()
                                    AuthResult.AccessDenied -> onAccessDenied()
                                    AuthResult.NetworkError -> snackbarHostState.showSnackbar("No internet connection")
                                    AuthResult.Cancelled -> snackbarHostState.showSnackbar("Sign-in cancelled")
                                    is AuthResult.NoGoogleAccount -> snackbarHostState.showSnackbar(res.message)
                                    is AuthResult.Unknown -> snackbarHostState.showSnackbar(res.message)
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: GetCredentialCancellationException) {
                                Log.e("TheCountAuth", "${e.javaClass.simpleName}: ${e.message}")
                                snackbarHostState.showSnackbar("Sign-in cancelled")
                            } catch (e: NoCredentialException) {
                                Log.e("TheCountAuth", "${e.javaClass.simpleName}: ${e.message}")
                                snackbarHostState.showSnackbar("No Google account is available on this phone. Add one in Settings and try again.")
                            } catch (e: Exception) {
                                Log.e("TheCountAuth", "${e.javaClass.simpleName}: ${e.message}")
                                snackbarHostState.showSnackbar("Something went wrong. Please try again.")
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                ) {
                    Text("Sign in with Google")
                }
            }
        }
    }
}

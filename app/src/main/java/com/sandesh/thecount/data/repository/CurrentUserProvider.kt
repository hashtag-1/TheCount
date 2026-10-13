package com.sandesh.thecount.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sandesh.thecount.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

interface CurrentUserProvider {
    val currentUserId: String
    val currentUserIdFlow: StateFlow<String>

    fun wasPromptedForAccount(userId: String): Boolean
    fun markAccountPrompted(userId: String)
    fun clearPromptedAccount()
}

class SessionUserProvider(context: Context) : CurrentUserProvider {

    companion object {
        private const val PREFS_NAME = "TheCount_UserPrefs"
        private const val KEY_PROMPTED_ACCOUNT = "prompted_account_id"

        // Matches no real user's rows, so nothing is shown while nobody is signed in.
        private const val NO_USER = "anonymous"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow(NO_USER)
    override val currentUserIdFlow: StateFlow<String> = _currentUserId.asStateFlow()

    override val currentUserId: String
        get() = _currentUserId.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        observeSession()
    }

    private fun observeSession() {
        scope.launch {
            // sessionStatus reports sign-in and sign-out.
            SupabaseProvider.client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> updateUserId(status.session.user?.id)
                    is SessionStatus.NotAuthenticated -> updateUserId(null)
                    else -> {
                        // Initializing or a temporary refresh problem (for example offline):
                        // keep the current user, so the data does not vanish.
                    }
                }
            }
        }
    }

    private fun updateUserId(userId: String?) {
        val newId = userId ?: NO_USER
        if (_currentUserId.value != newId) {
            _currentUserId.value = newId
        }
    }

    override fun wasPromptedForAccount(userId: String): Boolean {
        return prefs.getString(KEY_PROMPTED_ACCOUNT, null) == userId
    }

    override fun markAccountPrompted(userId: String) {
        prefs.edit().putString(KEY_PROMPTED_ACCOUNT, userId).apply()
    }

    override fun clearPromptedAccount() {
        prefs.edit().remove(KEY_PROMPTED_ACCOUNT).apply()
    }

    fun shutdown() {
        scope.cancel()
    }
}
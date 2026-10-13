package com.sandesh.thecount.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableStateFlow
import androidx.lifecycle.StateFlow
import androidx.lifecycle.asStateFlow
import com.sandesh.thecount.data.remote.SupabaseProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

interface CurrentUserProvider {
    val currentUserId: String
    val currentUserIdFlow: StateFlow<String>

    fun wasPromptedForAccount(userId: String): Boolean
    fun markAccountPrompted(userId: String)
    fun clearPromptedAccount()
}

class SessionUserProvider(private val context: Context) : CurrentUserProvider {

    companion object {
        private const val PREFS_NAME = "TheCount_UserPrefs"
        private const val KEY_PROMPTED_ACCOUNT = "prompted_account_id"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentUserId = MutableStateFlow<String>("anonymous")
    override val currentUserIdFlow: StateFlow<String> = _currentUserId.asStateFlow()

    override val currentUserId: String
        get() = _currentUserId.value

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        observeSession()
    }

    private fun observeSession() {
        scope.launch {
            val auth = SupabaseProvider.client.auth
            // Get initial session
            val session = auth.currentSessionOrNull()
            updateUserId(session?.user?.id)

            // Observe auth state changes
            auth.sessionChanges.collect { event ->
                val userId = event?.session?.user?.id
                updateUserId(userId)
            }
        }
    }

    private fun updateUserId(userId: String?) {
        val newId = userId ?: "anonymous"
        if (_currentUserId.value != newId) {
            _currentUserId.value = newId
        }
    }

    fun wasPromptedForAccount(userId: String): Boolean {
        val promptedId = prefs.getString(KEY_PROMPTED_ACCOUNT, null)
        return promptedId == userId
    }

    fun markAccountPrompted(userId: String) {
        prefs.edit().putString(KEY_PROMPTED_ACCOUNT, userId).apply()
    }

    fun clearPromptedAccount() {
        prefs.edit().remove(KEY_PROMPTED_ACCOUNT).apply()
    }

    fun shutdown() {
        scope.cancel()
    }
}
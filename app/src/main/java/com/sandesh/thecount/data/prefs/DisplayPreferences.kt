package com.sandesh.thecount.data.prefs

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DisplayPreferences private constructor(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("thecount_display_prefs", Context.MODE_PRIVATE)

    private val _hideCurrency = MutableStateFlow(prefs.getBoolean("hide_currency", false))
    val hideCurrency: StateFlow<Boolean> = _hideCurrency.asStateFlow()

    fun setHideCurrency(value: Boolean) {
        _hideCurrency.value = value
        prefs.edit().putBoolean("hide_currency", value).apply()
    }

    companion object {
        @Volatile
        private var instance: DisplayPreferences? = null

        fun getInstance(context: Context): DisplayPreferences {
            return instance ?: synchronized(this) {
                instance ?: DisplayPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}

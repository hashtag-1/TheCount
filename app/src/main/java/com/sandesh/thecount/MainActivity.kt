package com.sandesh.thecount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.ui.TheCountApp
import com.sandesh.thecount.ui.theme.TheCountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.initialize(application)
        setContent {
            TheCountTheme {
                TheCountApp()
            }
        }
    }
}
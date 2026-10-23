package com.sandesh.thecount

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sandesh.thecount.data.remote.SupabaseProvider
import com.sandesh.thecount.data.sync.SyncScheduler
import com.sandesh.thecount.ui.TheCountApp
import com.sandesh.thecount.ui.theme.TheCountTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.initialize(application)
        SyncScheduler.schedule(application)
        setContent {
            TheCountTheme {
                TheCountApp()
            }
        }
    }

    // Runs every time the app comes to the foreground, so edits made in the
    // database show up as soon as you reopen the app.
    override fun onStart() {
        super.onStart()
        SyncScheduler.syncNow(application)
    }
}
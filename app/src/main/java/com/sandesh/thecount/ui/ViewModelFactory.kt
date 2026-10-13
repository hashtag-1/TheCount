package com.sandesh.thecount.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.repository.CurrentUserProvider
import com.sandesh.thecount.data.repository.LocalUserProvider
import com.sandesh.thecount.data.repository.PaymentRepository
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.ui.viewmodel.HomeViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    private val userProvider: CurrentUserProvider = LocalUserProvider()

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = TheCountDatabase.getInstance(application)
            val repo = StrokeRepository(db.dailyStrokeDao(), userProvider)
            val payRepo = PaymentRepository(db.paymentDao(), userProvider)
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo, payRepo, userProvider) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.sandesh.thecount.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sandesh.thecount.data.db.TheCountDatabase
import com.sandesh.thecount.data.repository.PaymentRepository
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.ui.viewmodel.HomeViewModel

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            val db = TheCountDatabase.getInstance(application)
            val repo = StrokeRepository(db.dailyStrokeDao())
            val payRepo = PaymentRepository(db.paymentDao())
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repo, payRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

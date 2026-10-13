package com.sandesh.thecount.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandesh.thecount.data.repository.CurrentUserProvider
import com.sandesh.thecount.data.repository.PaymentRepository
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.domain.EarningsCalculator
import com.sandesh.thecount.domain.EarningsResult
import com.sandesh.thecount.domain.PaymentValidators
import com.sandesh.thecount.domain.Validators
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayCount: Long? = null,
    val selectedDateCount: Long? = null,
    val cumulativeStrokes: Long = 0,
    val allStrokes: List<com.sandesh.thecount.data.db.DailyStrokeEntity> = emptyList(),
    val earnings: EarningsResult? = null,
    val payments: List<com.sandesh.thecount.data.db.PaymentEntity> = emptyList(),
    val paymentsTotalUpToSelected: BigDecimal = BigDecimal.ZERO
)

class HomeViewModel(
    private val strokeRepository: StrokeRepository,
    private val paymentRepository: PaymentRepository? = null,
    private val userProvider: CurrentUserProvider
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // The signed-in user's id. A StateFlow already ignores repeated values.
    private val userIdFlow: StateFlow<String> = userProvider.currentUserIdFlow

    init {
        // When the signed-in user changes, reset per-user screen state.
        viewModelScope.launch {
            userIdFlow.collect {
                _selectedDate.value = LocalDate.now()
                _error.value = null
            }
        }
    }

    // Every time the user id changes, the old queries are cancelled and new ones start,
    // so a second account never keeps showing the first account's data.
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = userIdFlow
        .flatMapLatest { userId ->
            if (userId == "anonymous") {
                // Nobody signed in: show an empty state.
                flowOf(HomeUiState())
            } else {
                combine(
                    _selectedDate,
                    strokeRepository.observeAll(),
                    paymentRepository?.observeAll() ?: flowOf(emptyList())
                ) { selectedDate, allStrokes, payments ->
                    val liveStrokes = allStrokes.filter { it.deletedAt == null }
                    val livePayments = payments.filter { it.deletedAt == null }

                    val todayCount = liveStrokes.find { it.workDate == LocalDate.now() }?.strokeCount
                    val selectedCount = liveStrokes.find { it.workDate == selectedDate }?.strokeCount

                    val cumulativeUpToSelected = liveStrokes
                        .filter { !it.workDate.isAfter(selectedDate) }
                        .sumOf { it.strokeCount }
                    val paymentsUpToSelected = livePayments
                        .filter { !it.paymentDate.isAfter(selectedDate) }
                        .sumOf { it.amount }

                    val earnings = EarningsCalculator.calculate(cumulativeUpToSelected, paymentsUpToSelected)

                    HomeUiState(
                        selectedDate = selectedDate,
                        todayCount = todayCount,
                        selectedDateCount = selectedCount,
                        cumulativeStrokes = cumulativeUpToSelected,
                        allStrokes = liveStrokes,
                        earnings = earnings,
                        payments = livePayments,
                        paymentsTotalUpToSelected = paymentsUpToSelected
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState()
        )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun saveToday(countStr: String) {
        saveForDate(LocalDate.now(), countStr)
    }

    fun saveForDate(date: LocalDate, countStr: String) {
        val dateResult = Validators.validateWorkDate(date)
        if (dateResult.isFailure) {
            _error.value = dateResult.exceptionOrNull()?.message
            return
        }
        val countResult = Validators.validateStrokeCount(countStr)
        if (countResult.isFailure) {
            _error.value = countResult.exceptionOrNull()?.message
            return
        }
        viewModelScope.launch {
            try {
                strokeRepository.saveOrUpdate(date, countResult.getOrThrow())
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun delete(date: LocalDate) {
        val dateResult = Validators.validateWorkDate(date)
        if (dateResult.isFailure) {
            _error.value = dateResult.exceptionOrNull()?.message
            return
        }
        viewModelScope.launch {
            try {
                strokeRepository.softDelete(date)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun addPayment(amountStr: String, date: LocalDate, note: String?) {
        val dateResult = PaymentValidators.validateDate(date)
        if (dateResult.isFailure) {
            _error.value = dateResult.exceptionOrNull()?.message
            return
        }
        val amountResult = PaymentValidators.validateAmount(amountStr)
        if (amountResult.isFailure) {
            _error.value = amountResult.exceptionOrNull()?.message
            return
        }
        if (paymentRepository == null) {
            _error.value = "PaymentRepository not initialized"
            return
        }
        viewModelScope.launch {
            try {
                paymentRepository.add(date, amountResult.getOrThrow(), note)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun deletePayment(id: UUID) {
        if (paymentRepository == null) {
            _error.value = "PaymentRepository not initialized"
            return
        }
        viewModelScope.launch {
            try {
                paymentRepository.softDelete(id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}
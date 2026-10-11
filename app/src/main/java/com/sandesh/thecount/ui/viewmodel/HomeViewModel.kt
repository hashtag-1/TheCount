package com.sandesh.thecount.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandesh.thecount.data.repository.PaymentRepository
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.domain.EarningsCalculator
import com.sandesh.thecount.domain.EarningsResult
import com.sandesh.thecount.domain.PaymentValidators
import com.sandesh.thecount.domain.Validators
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val paymentRepository: PaymentRepository? = null
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val userId = "local"
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        strokeRepository.observeAll(userId),
        strokeRepository.sumStrokesUntil(userId, LocalDate.now()),
        paymentRepository?.observeAll(userId) ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        paymentRepository?.sumPaymentsUntil(userId, LocalDate.now()) ?: kotlinx.coroutines.flow.flowOf(BigDecimal.ZERO)
    ) { selectedDate, allStrokes, cumulative, payments, paymentsTotal ->
        val todayCount = allStrokes.find { it.workDate == LocalDate.now() && it.deletedAt == null }?.strokeCount
        val selectedCount = allStrokes.find { it.workDate == selectedDate && it.deletedAt == null }?.strokeCount
        val cumulativeUpToSelected = allStrokes.filter { it.deletedAt == null && !it.workDate.isAfter(selectedDate) }
            .sumOf { it.strokeCount }
        val paymentsUpToSelected = payments.filter { it.deletedAt == null && !it.paymentDate.isAfter(selectedDate) }
            .sumOf { it.amount }
        val earnings = EarningsCalculator.calculate(cumulativeUpToSelected, paymentsUpToSelected)
        HomeUiState(
            selectedDate = selectedDate,
            todayCount = todayCount,
            selectedDateCount = selectedCount,
            cumulativeStrokes = cumulative,
            allStrokes = allStrokes.filter { it.deletedAt == null },
            earnings = earnings,
            payments = payments.filter { it.deletedAt == null },
            paymentsTotalUpToSelected = paymentsUpToSelected
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
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
                strokeRepository.saveOrUpdate(userId, date, countResult.getOrThrow())
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
                strokeRepository.softDelete(userId, date)
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
                paymentRepository.add(userId, date, amountResult.getOrThrow(), note)
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
                paymentRepository.softDelete(userId, id)
                _error.value = null
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }
}

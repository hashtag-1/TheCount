package com.sandesh.thecount.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandesh.thecount.data.repository.StrokeRepository
import com.sandesh.thecount.domain.Validators
import java.time.LocalDate
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
    val allStrokes: List<com.sandesh.thecount.data.db.DailyStrokeEntity> = emptyList()
)

class HomeViewModel(private val strokeRepository: StrokeRepository) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val userId = "local"
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        _selectedDate,
        strokeRepository.observeAll(userId),
        strokeRepository.sumStrokesUntil(userId, LocalDate.now())
    ) { selectedDate, allStrokes, cumulative ->
        val todayCount = allStrokes.find { it.workDate == LocalDate.now() && it.deletedAt == null }?.strokeCount
        val selectedCount = allStrokes.find { it.workDate == selectedDate && it.deletedAt == null }?.strokeCount
        HomeUiState(
            selectedDate = selectedDate,
            todayCount = todayCount,
            selectedDateCount = selectedCount,
            cumulativeStrokes = cumulative,
            allStrokes = allStrokes.filter { it.deletedAt == null }
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
}

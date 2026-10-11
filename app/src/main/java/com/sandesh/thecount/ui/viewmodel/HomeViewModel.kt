package com.sandesh.thecount.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sandesh.thecount.data.repository.StrokeRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val todayCount: Long? = null,
    val selectedDateCount: Long? = null,
    val cumulativeStrokes: Long = 0
)

class HomeViewModel(private val strokeRepository: StrokeRepository) : ViewModel() {
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val userId = "local"

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
            cumulativeStrokes = cumulative
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }
}

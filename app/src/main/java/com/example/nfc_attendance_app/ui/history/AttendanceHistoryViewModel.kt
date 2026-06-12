package com.example.nfc_attendance_app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfc_attendance_app.data.AttendanceHistoryRepository
import com.example.nfc_attendance_app.data.LocalUserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AttendanceHistoryViewModel(
    private val repository: AttendanceHistoryRepository,
    private val preferences: LocalUserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AttendanceHistoryUiState>(AttendanceHistoryUiState.Loading)
    val uiState: StateFlow<AttendanceHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val userNumber = preferences.getUserNumber()
        if (userNumber == null) {
            _uiState.value = AttendanceHistoryUiState.Error("사용자 번호 로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AttendanceHistoryUiState.Loading
            try {
                val records = repository.getAttendanceHistory(userNumber)
                if (records.isEmpty()) {
                    _uiState.value = AttendanceHistoryUiState.Empty
                } else {
                    _uiState.value = AttendanceHistoryUiState.Success(records)
                }
            } catch (e: Exception) {
                _uiState.value = AttendanceHistoryUiState.Error("기록을 불러오지 못했습니다.")
            }
        }
    }

    fun refresh() {
        loadHistory()
    }
}

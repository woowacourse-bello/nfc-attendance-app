package com.example.nfc_attendance_app.ui.history

import com.example.nfc_attendance_app.data.model.AttendanceRecord

sealed interface AttendanceHistoryUiState {
    data object Loading : AttendanceHistoryUiState

    data class Success(
        val records: List<AttendanceRecord>,
    ) : AttendanceHistoryUiState

    data object Empty : AttendanceHistoryUiState

    data class Error(
        val message: String,
    ) : AttendanceHistoryUiState
}

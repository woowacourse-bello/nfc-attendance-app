package com.example.nfc_attendance_app.ui.nfc

import com.example.nfc_attendance_app.data.model.AttendanceStatus
import com.example.nfc_attendance_app.data.model.AttendanceType

sealed interface NfcAttendanceUiState {
    data object Waiting : NfcAttendanceUiState
    data object Loading : NfcAttendanceUiState
    data class Success(
        val message: String,
        val type: AttendanceType,
        val status: AttendanceStatus?,
        val checkedAt: Long,
    ) : NfcAttendanceUiState

    data class Error(
        val message: String
    ) : NfcAttendanceUiState
}

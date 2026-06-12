package com.example.nfc_attendance_app.ui.nfc

import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceStatus
import com.example.nfc_attendance_app.data.model.AttendanceType

data class NfcAttendanceUiState(
    val userName: String = "",
    val todayRecords: List<AttendanceRecord> = emptyList(),
    val actionState: ActionState = ActionState.Waiting
)

sealed interface ActionState {
    data object Waiting : ActionState
    data object Loading : ActionState
    
    data class ConfirmEarlyLeave(
        val userNumber: String,
        val userName: String,
        val tagId: String,
        val checkedAt: Long,
    ) : ActionState

    data class Success(
        val message: String,
        val type: AttendanceType,
        val status: AttendanceStatus?,
        val checkedAt: Long,
    ) : ActionState

    data class Error(
        val message: String
    ) : ActionState
}

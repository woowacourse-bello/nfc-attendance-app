package com.example.nfc_attendance_app.data.model

sealed interface AttendanceActionResult {
    data class Saved(
        val result: AttendanceResult,
    ) : AttendanceActionResult

    data class PendingEarlyLeave(
        val userNumber: String,
        val userName: String,
        val tagId: String,
        val checkedAt: Long,
    ) : AttendanceActionResult
}

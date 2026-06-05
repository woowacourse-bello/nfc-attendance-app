package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceActionResult
import com.example.nfc_attendance_app.data.model.AttendanceResult

interface AttendanceRepository {
    suspend fun processAttendance(
        userNumber: String,
        userName: String,
        tagId: String,
    ): AttendanceActionResult

    suspend fun confirmEarlyLeave(
        userNumber: String,
        userName: String,
        tagId: String,
        checkedAt: Long,
    ): AttendanceResult
}

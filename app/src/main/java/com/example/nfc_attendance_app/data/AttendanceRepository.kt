package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceActionResult
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceResult

interface AttendanceRepository {
    suspend fun processAttendance(
        userNumber: String,
        userName: String,
        tagId: String = "MANUAL",
    ): AttendanceActionResult

    suspend fun confirmEarlyLeave(
        userNumber: String,
        userName: String,
        tagId: String = "MANUAL",
        checkedAt: Long,
    ): AttendanceResult

    suspend fun getTodayRecords(userNumber: String): List<AttendanceRecord>
}

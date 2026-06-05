package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceRecord

interface AttendanceHistoryRepository {
    suspend fun getAttendanceHistory(
        userNumber: String,
    ): List<AttendanceRecord>
}

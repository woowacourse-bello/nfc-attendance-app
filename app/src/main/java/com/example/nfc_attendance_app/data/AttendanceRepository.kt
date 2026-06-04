package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceResult

interface AttendanceRepository {

    suspend fun recordAttendance(
        userNumber: String,
        userName: String,
        tagId: String
    ): AttendanceResult
}

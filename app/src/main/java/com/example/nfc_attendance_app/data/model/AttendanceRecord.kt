package com.example.nfc_attendance_app.data.model

data class AttendanceRecord(
    val userNumber: String = "",
    val userName: String = "",
    val tagId: String = "",
    val type: String = AttendanceType.CHECK_IN.name,
    val checkedAt: Long = System.currentTimeMillis()
)

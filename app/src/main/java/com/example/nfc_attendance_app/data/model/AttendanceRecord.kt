package com.example.nfc_attendance_app.data.model

data class AttendanceRecord(
    val userNumber: String = "",
    val userName: String = "",
    val tagId: String = "",
    val type: String = "",
    val status: String? = null,
    val checkedAt: Long = 0L,
)

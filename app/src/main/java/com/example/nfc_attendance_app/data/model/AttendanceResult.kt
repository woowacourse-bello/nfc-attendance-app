package com.example.nfc_attendance_app.data.model

data class AttendanceResult(
    val message: String,
    val type: AttendanceType?,
    val checkedAt: Long
)

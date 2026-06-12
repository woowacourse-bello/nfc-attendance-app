package com.example.nfc_attendance_app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateTimeFormatter {
    fun formatCheckedAt(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.KOREA)
        return formatter.format(Date(timestamp))
    }

    fun formatToTime(timestamp: Long): String {
        val formatter = SimpleDateFormat("HH:mm", Locale.KOREA)
        return formatter.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        return formatter.format(Date(timestamp))
    }
}

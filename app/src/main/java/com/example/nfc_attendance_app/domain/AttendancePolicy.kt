package com.example.nfc_attendance_app.domain

import com.example.nfc_attendance_app.data.model.AttendanceStatus
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

class AttendancePolicy {

    private val zoneId = ZoneId.of("Asia/Seoul")

    fun getCheckInStatus(nowMillis: Long): AttendanceStatus {
        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalTime()

        return when {
            !nowTime.isAfter(LocalTime.of(10, 0)) -> AttendanceStatus.PRESENT
            !nowTime.isAfter(LocalTime.of(10, 30)) -> AttendanceStatus.LATE
            else -> AttendanceStatus.ABSENT
        }
    }

    fun getCheckOutStatus(nowMillis: Long): AttendanceStatus? {
        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalTime()

        return if (nowTime.isBefore(LocalTime.of(18, 0))) {
            AttendanceStatus.EARLY_LEAVE
        } else {
            null
        }
    }

    fun isBeforeCheckInStart(nowMillis: Long): Boolean {
        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalTime()
        return nowTime.isBefore(LocalTime.of(8, 0))
    }

    fun isRecordRestricted(nowMillis: Long): Boolean {
        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalTime()
        // 23:01 이후면 true (23:01 포함)
        return !nowTime.isBefore(LocalTime.of(23, 1))
    }
}

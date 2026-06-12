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
        return nowTime.isBefore(LocalTime.of(6, 0))
    }

    fun isRecordRestricted(nowMillis: Long): Boolean {
        val nowTime = Instant.ofEpochMilli(nowMillis)
            .atZone(zoneId)
            .toLocalTime()
        // 23:30 이후면 true (23:30 포함)
        return !nowTime.isBefore(LocalTime.of(23, 30))
    }

    /**
     * 출근 기록과 퇴근 기록을 종합하여 최종 당일 상태를 결정합니다.
     */
    fun calculateDailyStatus(checkInStatus: AttendanceStatus?, checkOutStatus: AttendanceStatus?): AttendanceStatus? {
        if (checkInStatus == null) return null
        
        // 출근이 결석 상태라면 퇴근과 상관없이 결석
        if (checkInStatus == AttendanceStatus.ABSENT) return AttendanceStatus.ABSENT
        
        // 퇴근 기록이 없으면 현재 출근 상태를 유지
        if (checkOutStatus == null) return checkInStatus
        
        // 조퇴라면 출근 상태(정상/지각)보다 조퇴가 우선
        if (checkOutStatus == AttendanceStatus.EARLY_LEAVE) return AttendanceStatus.EARLY_LEAVE
        
        // 그 외(정상 퇴근)의 경우 출근 상태를 최종 상태로 반환
        return checkInStatus
    }
}

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
        // 21:01 이후면 true (21:01:00 포함)
        return !nowTime.isBefore(LocalTime.of(21, 1))
    }

    /**
     * 출근 기록과 퇴근 기록을 종합하여 최종 당일 상태를 결정합니다.
     * @param isCheckOutMissing 하교 기록 자체가 없는지 여부
     * @param isToday 오늘 날짜인지 여부 (오늘 하교 기록이 없는 것은 아직 하교 전일 수 있으므로 21:01 전까지는 정상 유지)
     */
    fun calculateDailyStatus(
        checkInStatus: AttendanceStatus?,
        checkOutStatus: AttendanceStatus?,
        isCheckOutMissing: Boolean = false,
        isToday: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): AttendanceStatus? {
        if (checkInStatus == null) return null
        
        // 출근이 결석 상태라면 퇴근과 상관없이 결석 (벌점이 더 높으므로 조퇴로 덮어쓰지 않음)
        if (checkInStatus == AttendanceStatus.ABSENT) return AttendanceStatus.ABSENT
        
        // 조퇴 기록이 있는 경우 (하교를 찍었으나 시간상 조퇴)
        if (checkOutStatus == AttendanceStatus.EARLY_LEAVE) return AttendanceStatus.EARLY_LEAVE
        
        // 하교 기록이 없는 경우
        if (isCheckOutMissing) {
            // 오늘이 아니면 무조건 '조퇴'
            if (!isToday) return AttendanceStatus.EARLY_LEAVE
            
            // 오늘인데 21:01이 넘었다면 '조퇴'
            val nowTime = Instant.ofEpochMilli(nowMillis)
                .atZone(zoneId)
                .toLocalTime()
            if (!nowTime.isBefore(LocalTime.of(21, 1))) {
                return AttendanceStatus.EARLY_LEAVE
            }
        }
        
        // 하교 기록이 있거나(정상 하교), 하교 기록이 없는데 아직 21:01 전인 오늘인 경우 출근 상태(정상/지각) 유지
        return checkInStatus
    }
}

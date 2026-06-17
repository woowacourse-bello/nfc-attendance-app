package com.example.nfc_attendance_app.domain

import com.example.nfc_attendance_app.data.model.AttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class AttendancePolicyTest {

    private val policy = AttendancePolicy()
    private val zoneId = ZoneId.of("Asia/Seoul")

    @Test
    fun `calculateDailyStatus should return EARLY_LEAVE when check-out is missing for past days`() {
        val result = policy.calculateDailyStatus(
            checkInStatus = AttendanceStatus.PRESENT,
            checkOutStatus = null,
            isCheckOutMissing = true,
            isToday = false
        )
        assertEquals(AttendanceStatus.EARLY_LEAVE, result)
    }

    @Test
    fun `calculateDailyStatus should return PRESENT when check-out is missing for today before 21 01`() {
        val now = ZonedDateTime.now(zoneId).with(LocalTime.of(20, 0)).toInstant().toEpochMilli()
        val result = policy.calculateDailyStatus(
            checkInStatus = AttendanceStatus.PRESENT,
            checkOutStatus = null,
            isCheckOutMissing = true,
            isToday = true,
            nowMillis = now
        )
        assertEquals(AttendanceStatus.PRESENT, result)
    }

    @Test
    fun `calculateDailyStatus should return EARLY_LEAVE when check-out is missing for today after 21 01`() {
        val now = ZonedDateTime.now(zoneId).with(LocalTime.of(21, 5)).toInstant().toEpochMilli()
        val result = policy.calculateDailyStatus(
            checkInStatus = AttendanceStatus.PRESENT,
            checkOutStatus = null,
            isCheckOutMissing = true,
            isToday = true,
            nowMillis = now
        )
        assertEquals(AttendanceStatus.EARLY_LEAVE, result)
    }

    @Test
    fun `calculateDailyStatus should return PRESENT when check-out is completed normally`() {
        val result = policy.calculateDailyStatus(
            checkInStatus = AttendanceStatus.PRESENT,
            checkOutStatus = null, // Normal checkout status is null
            isCheckOutMissing = false, // But record exists
            isToday = false
        )
        assertEquals(AttendanceStatus.PRESENT, result)
    }

    @Test
    fun `isRecordRestricted should return true after 21 01`() {
        val now = ZonedDateTime.now(zoneId).with(LocalTime.of(21, 2)).toInstant().toEpochMilli()
        assertEquals(true, policy.isRecordRestricted(now))
    }

    @Test
    fun `isRecordRestricted should return false before 21 01`() {
        val now = ZonedDateTime.now(zoneId).with(LocalTime.of(21, 0)).toInstant().toEpochMilli()
        assertEquals(false, policy.isRecordRestricted(now))
    }
}

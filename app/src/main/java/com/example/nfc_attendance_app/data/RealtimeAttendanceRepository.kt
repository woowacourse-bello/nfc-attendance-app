package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceActionResult
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceResult
import com.example.nfc_attendance_app.data.model.AttendanceStatus
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.data.model.NfcTagInfo
import com.example.nfc_attendance_app.domain.AttendancePolicy
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RealtimeAttendanceRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val policy: AttendancePolicy = AttendancePolicy()
) : AttendanceRepository {

    override suspend fun processAttendance(
        userNumber: String,
        userName: String,
        tagId: String
    ): AttendanceActionResult {
        try {
            // 1. NFC 태그 유효성 검사
            val tagSnapshot = database.reference
                .child("nfcTags")
                .child(tagId)
                .get()
                .await()

            if (!tagSnapshot.exists()) {
                throw Exception("등록되지 않은 NFC 태그입니다.")
            }

            val tagInfo = tagSnapshot.getValue(NfcTagInfo::class.java)
                ?: throw Exception("등교/하교 처리에 실패했습니다.")

            if (!tagInfo.isActive) {
                throw Exception("비활성화된 NFC 태그입니다.")
            }

            // 2. 현재 시각 생성 및 정책 확인
            val currentTime = System.currentTimeMillis()
            
            if (policy.isRecordRestricted(currentTime)) {
                throw Exception("오늘 등교/하교 기록 가능 시간이 지났습니다.")
            }

            // 3. 사용자의 오늘 기록 조회 및 타입 자동 판단
            val recordsSnapshot = database.reference
                .child("attendanceRecords")
                .orderByChild("userNumber")
                .equalTo(userNumber)
                .get()
                .await()

            val todayRecords = recordsSnapshot.children.mapNotNull {
                it.getValue(AttendanceRecord::class.java)
            }.filter { isSameDate(it.checkedAt, currentTime) }

            val hasCheckIn = todayRecords.any { it.type == AttendanceType.CHECK_IN.name }
            val hasCheckOut = todayRecords.any { it.type == AttendanceType.CHECK_OUT.name }

            return when {
                !hasCheckIn -> {
                    if (policy.isBeforeCheckInStart(currentTime)) {
                        throw Exception("등교 가능 시간이 아닙니다.")
                    }
                    val status = policy.getCheckInStatus(currentTime)
                    val result = saveRecord(userNumber, userName, tagId, AttendanceType.CHECK_IN, status, currentTime, "등교가 완료되었습니다.")
                    AttendanceActionResult.Saved(result)
                }
                !hasCheckOut -> {
                    val status = policy.getCheckOutStatus(currentTime)
                    if (status == AttendanceStatus.EARLY_LEAVE) {
                        // 18:00 이전 하교이면 보류 상태 반환
                        AttendanceActionResult.PendingEarlyLeave(userNumber, userName, tagId, currentTime)
                    } else {
                        val result = saveRecord(userNumber, userName, tagId, AttendanceType.CHECK_OUT, null, currentTime, "하교가 완료되었습니다.")
                        AttendanceActionResult.Saved(result)
                    }
                }
                else -> throw Exception("오늘 등교와 하교가 이미 완료되었습니다.")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun confirmEarlyLeave(
        userNumber: String,
        userName: String,
        tagId: String,
        checkedAt: Long
    ): AttendanceResult {
        try {
            // 중복 저장 방어: 오늘 이미 퇴실 기록이 있는지 다시 확인
            val recordsSnapshot = database.reference
                .child("attendanceRecords")
                .orderByChild("userNumber")
                .equalTo(userNumber)
                .get()
                .await()

            val alreadyHasCheckOut = recordsSnapshot.children.mapNotNull {
                it.getValue(AttendanceRecord::class.java)
            }.filter { isSameDate(it.checkedAt, checkedAt) }
             .any { it.type == AttendanceType.CHECK_OUT.name }

            if (alreadyHasCheckOut) {
                throw Exception("오늘 등교와 하교가 이미 완료되었습니다.")
            }

            return saveRecord(
                userNumber, userName, tagId, 
                AttendanceType.CHECK_OUT, 
                AttendanceStatus.EARLY_LEAVE, 
                checkedAt, 
                "조퇴 처리되었습니다."
            )
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getTodayRecords(userNumber: String): List<AttendanceRecord> {
        val currentTime = System.currentTimeMillis()
        val recordsSnapshot = database.reference
            .child("attendanceRecords")
            .orderByChild("userNumber")
            .equalTo(userNumber)
            .get()
            .await()

        return recordsSnapshot.children.mapNotNull {
            it.getValue(AttendanceRecord::class.java)
        }.filter { isSameDate(it.checkedAt, currentTime) }
            .sortedBy { it.checkedAt }
    }

    private suspend fun saveRecord(
        userNumber: String,
        userName: String,
        tagId: String,
        type: AttendanceType,
        status: AttendanceStatus?,
        checkedAt: Long,
        successMessage: String
    ): AttendanceResult {
        val newRecord = AttendanceRecord(
            userNumber = userNumber,
            userName = userName,
            tagId = tagId,
            type = type.name,
            status = status?.name,
            checkedAt = checkedAt
        )

        database.reference
            .child("attendanceRecords")
            .push()
            .setValue(newRecord)
            .await()

        return AttendanceResult(
            message = successMessage,
            type = type,
            status = status,
            checkedAt = checkedAt
        )
    }

    private fun isSameDate(timestamp1: Long, timestamp2: Long): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val date1 = formatter.format(Date(timestamp1))
        val date2 = formatter.format(Date(timestamp2))
        return date1 == date2
    }
}

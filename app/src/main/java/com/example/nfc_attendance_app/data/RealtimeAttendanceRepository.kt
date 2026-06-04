package com.example.nfc_attendance_app.data

import android.util.Log
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceResult
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.data.model.NfcTagInfo
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RealtimeAttendanceRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : AttendanceRepository {

    override suspend fun recordAttendance(
        userNumber: String,
        userName: String,
        tagId: String
    ): AttendanceResult {
        try {
            // 1. NFC 태그 유효성 검사
            val tagSnapshot = database.reference
                .child("nfcTags")
                .child(tagId)
                .get()
                .await()

            Log.d("TAGSNAPSHOT", tagSnapshot.toString())

            if (!tagSnapshot.exists()) {
                throw Exception("등록되지 않은 NFC 태그입니다.")
            }

            val tagInfo = tagSnapshot.getValue(NfcTagInfo::class.java)
                ?: throw Exception("출석 처리에 실패했습니다.")

            if (!tagInfo.isActive) {
                throw Exception("비활성화된 NFC 태그입니다.")
            }

            // 2. 사용자의 오늘 기록 조회 및 타입 자동 판단
            val recordsSnapshot = database.reference
                .child("attendanceRecords")
                .orderByChild("userNumber")
                .equalTo(userNumber)
                .get()
                .await()

            val currentTime = System.currentTimeMillis()
            val todayRecords = recordsSnapshot.children.mapNotNull {
                it.getValue(AttendanceRecord::class.java)
            }.filter { isSameDate(it.checkedAt, currentTime) }

            val hasCheckIn = todayRecords.any { it.type == AttendanceType.CHECK_IN.name }
            val hasCheckOut = todayRecords.any { it.type == AttendanceType.CHECK_OUT.name }

            val typeToRecord = when {
                !hasCheckIn -> AttendanceType.CHECK_IN
                !hasCheckOut -> AttendanceType.CHECK_OUT
                else -> throw Exception("오늘 출석과 퇴실이 이미 완료되었습니다.")
            }

            // 3. 기록 저장
            val newRecord = AttendanceRecord(
                userNumber = userNumber,
                userName = userName,
                tagId = tagId,
                type = typeToRecord.name,
                checkedAt = currentTime
            )

            database.reference
                .child("attendanceRecords")
                .push()
                .setValue(newRecord)
                .await()

            val successMessage = if (typeToRecord == AttendanceType.CHECK_IN) {
                "출석이 완료되었습니다."
            } else {
                "퇴실이 완료되었습니다."
            }

            return AttendanceResult(
                message = successMessage,
                type = typeToRecord,
                checkedAt = newRecord.checkedAt
            )

        } catch (e: Exception) {
            // 상세한 에러 메시지 전달을 위해 catch 후 throw 또는 별도 처리
            throw e
        }
    }

    private fun isSameDate(timestamp1: Long, timestamp2: Long): Boolean {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
        val date1 = formatter.format(Date(timestamp1))
        val date2 = formatter.format(Date(timestamp2))
        return date1 == date2
    }
}

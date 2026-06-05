package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RealtimeAttendanceHistoryRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : AttendanceHistoryRepository {

    override suspend fun getAttendanceHistory(userNumber: String): List<AttendanceRecord> {
        return try {
            val snapshot = database.reference
                .child("attendanceRecords")
                .orderByChild("userNumber")
                .equalTo(userNumber)
                .get()
                .await()

            snapshot.children.mapNotNull {
                it.getValue(AttendanceRecord::class.java)
            }.sortedByDescending { it.checkedAt }
        } catch (e: Exception) {
            throw e
        }
    }
}

package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.UserInfo
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class RealtimeLoginRepository(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()
) : LoginRepository {

    override suspend fun loginWithUserNumber(userNumber: String): UserInfo {
        try {
            val snapshot = database.reference
                .child("users")
                .child(userNumber)
                .get()
                .await()

            if (!snapshot.exists()) {
                throw Exception("등록되지 않은 사용자 번호입니다.")
            }

            val userInfo = snapshot.getValue(UserInfo::class.java)
                ?: throw Exception("로그인 처리에 실패했습니다.")

            if (!userInfo.isActive) {
                throw Exception("비활성화된 사용자입니다.")
            }

            return userInfo
        } catch (e: Exception) {
            throw e
        }
    }
}

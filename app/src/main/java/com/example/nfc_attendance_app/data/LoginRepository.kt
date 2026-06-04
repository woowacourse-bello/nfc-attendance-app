package com.example.nfc_attendance_app.data

import com.example.nfc_attendance_app.data.model.UserInfo

interface LoginRepository {
    suspend fun loginWithUserNumber(userNumber: String): UserInfo
}

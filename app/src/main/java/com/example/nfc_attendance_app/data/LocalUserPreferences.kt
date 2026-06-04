package com.example.nfc_attendance_app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class LocalUserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    fun saveUser(userNumber: String, userName: String) {
        prefs.edit().apply {
            putString("user_number", userNumber)
            putString("user_name", userName)
            apply()
        }
    }

    fun getUserNumber(): String? = prefs.getString("user_number", null)

    fun getUserName(): String? = prefs.getString("user_name", null)

    fun isLoggedIn(): Boolean = getUserNumber() != null

    fun clearUser() {
        prefs.edit { clear() }
    }
}

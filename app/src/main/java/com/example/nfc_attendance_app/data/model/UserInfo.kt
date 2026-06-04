package com.example.nfc_attendance_app.data.model

import com.google.firebase.database.PropertyName

data class UserInfo(
    val userNumber: String = "",
    val name: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false
)

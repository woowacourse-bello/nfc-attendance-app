package com.example.nfc_attendance_app.data.model

import com.google.firebase.database.PropertyName

data class NfcTagInfo(
    val tagId: String = "",
    @get:PropertyName("isActive")
    @set:PropertyName("isActive")
    var isActive: Boolean = false
)

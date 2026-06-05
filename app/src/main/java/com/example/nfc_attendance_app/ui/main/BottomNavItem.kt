package com.example.nfc_attendance_app.ui.main

sealed class BottomNavItem(
    val route: String,
    val label: String,
) {
    data object Attendance : BottomNavItem(
        route = "attendance_tab",
        label = "출석",
    )

    data object History : BottomNavItem(
        route = "history_tab",
        label = "히스토리",
    )
}

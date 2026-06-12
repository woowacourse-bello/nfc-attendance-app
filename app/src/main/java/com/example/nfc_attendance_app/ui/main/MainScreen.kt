package com.example.nfc_attendance_app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.nfc_attendance_app.data.AttendanceHistoryRepository
import com.example.nfc_attendance_app.data.AttendanceRepository
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.ui.history.AttendanceHistoryRoute
import com.example.nfc_attendance_app.ui.history.AttendanceHistoryViewModel
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceRoute
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceViewModel

@Composable
fun MainScreen(
    attendanceRepository: AttendanceRepository,
    historyRepository: AttendanceHistoryRepository,
    preferences: LocalUserPreferences,
    onNfcViewModelCreated: (NfcAttendanceViewModel) -> Unit
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Attendance,
        BottomNavItem.History
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (item == BottomNavItem.Attendance) Icons.Default.Nfc else Icons.Default.History,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        val historyViewModel: AttendanceHistoryViewModel = viewModel {
            AttendanceHistoryViewModel(historyRepository, preferences)
        }
        
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Attendance.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Attendance.route) {
                val nfcViewModel: NfcAttendanceViewModel = viewModel {
                    NfcAttendanceViewModel(attendanceRepository, preferences)
                }
                onNfcViewModelCreated(nfcViewModel)
                NfcAttendanceRoute(viewModel = nfcViewModel)
            }
            composable(BottomNavItem.History.route) {
                AttendanceHistoryRoute(viewModel = historyViewModel)
            }
        }
    }
}

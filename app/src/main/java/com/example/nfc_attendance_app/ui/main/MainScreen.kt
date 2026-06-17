package com.example.nfc_attendance_app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.nfc_attendance_app.ui.home.HomeRoute
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceRoute
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceViewModel

@Composable
fun MainScreen(
    attendanceRepository: AttendanceRepository,
    historyRepository: AttendanceHistoryRepository,
    preferences: LocalUserPreferences,
    onNfcViewModelCreated: (NfcAttendanceViewModel) -> Unit,
    externalTabRequest: BottomNavItem? = null,
    onTabRequestConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Attendance,
        BottomNavItem.History
    )

    // 외부(NFC 인식 등)에서 탭 전환 요청이 있을 경우 처리
    LaunchedEffect(externalTabRequest) {
        externalTabRequest?.let { item ->
            navController.navigate(item.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
            onTabRequestConsumed()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { item ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = when (item) {
                                    BottomNavItem.Home -> Icons.Default.Home
                                    BottomNavItem.Attendance -> Icons.Default.Nfc
                                    BottomNavItem.History -> Icons.Default.History
                                },
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
        // ViewModel들을 NavHost 밖에서 생성하여 탭 전환 시에도 상태 유지 및 공유
        val historyViewModel: AttendanceHistoryViewModel = viewModel {
            AttendanceHistoryViewModel(historyRepository, preferences)
        }
        
        val nfcViewModel: NfcAttendanceViewModel = viewModel {
            NfcAttendanceViewModel(attendanceRepository, preferences)
        }
        onNfcViewModelCreated(nfcViewModel)

        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) {
                HomeRoute(viewModel = nfcViewModel)
            }
            composable(BottomNavItem.Attendance.route) {
                NfcAttendanceRoute(viewModel = nfcViewModel)
            }
            composable(BottomNavItem.History.route) {
                AttendanceHistoryRoute(viewModel = historyViewModel)
            }
        }
    }
}

@Composable
fun AttendancePolicyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "출석 정책 가이드",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "등교 규정",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                PolicyItem("등교 가능", "08:00 ~", Color(0xFF4CAF50))
                PolicyItem("정상 등교", "~ 10:00", Color(0xFF4CAF50))
                PolicyItem("지각 처리", "10:01 ~ 10:30", Color(0xFFFF9800))
                PolicyItem("결석 처리", "10:31 ~", Color(0xFFF44336))
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "하교 및 제한",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                PolicyItem("하교 가능", "18:00 ~", MaterialTheme.colorScheme.primary)
                PolicyItem("기록 제한", "21:01 ~", MaterialTheme.colorScheme.outline)

                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "• 등교 후 21:01까지 하교 태그가 없으면 '조퇴'로 기록됩니다. (결석자 제외)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("확인", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    )
}

@Composable
fun PolicyItem(label: String, time: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = time,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color.copy(alpha = 0.1f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

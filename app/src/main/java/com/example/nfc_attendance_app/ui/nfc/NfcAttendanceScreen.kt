package com.example.nfc_attendance_app.ui.nfc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceStatus
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.domain.AttendancePolicy
import com.example.nfc_attendance_app.ui.main.AttendancePolicyDialog
import com.example.nfc_attendance_app.utils.DateTimeFormatter

@Composable
fun NfcAttendanceRoute(
    viewModel: NfcAttendanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    NfcAttendanceScreen(
        uiState = uiState,
        onReset = { viewModel.resetToWaiting() },
        onConfirmEarlyLeave = { viewModel.confirmEarlyLeave() },
        onCancelEarlyLeave = { viewModel.cancelEarlyLeave() }
    )
}

@Composable
fun NfcAttendanceScreen(
    uiState: NfcAttendanceUiState,
    onReset: () -> Unit,
    onConfirmEarlyLeave: () -> Unit,
    onCancelEarlyLeave: () -> Unit
) {
    var showPolicyDialog by remember { mutableStateOf(false) }

    if (showPolicyDialog) {
        AttendancePolicyDialog(onDismiss = { showPolicyDialog = false })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. 사용자 인사말 섹션
        if (uiState.userName.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = uiState.userName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "님, 안녕하세요!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
                
                IconButton(
                    onClick = { showPolicyDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "출석 정책 정보",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 1. 오늘의 출석 현황 섹션
        TodayStatusSection(uiState.todayRecords)

        Spacer(modifier = Modifier.height(32.dp))

        // 2. NFC 인식 상태 섹션 (중앙 배치)
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (val actionState = uiState.actionState) {
                is ActionState.Waiting -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "NFC 태그를 접촉해주세요.",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "스마트폰 뒷면에 카드를 대주세요",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is ActionState.Loading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("정보를 처리 중입니다...")
                    }
                }

                is ActionState.ConfirmEarlyLeave -> {
                    AlertDialog(
                        onDismissRequest = onCancelEarlyLeave,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        title = {
                            Text(
                                text = "조퇴 확인",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "아직 하교 시작 시각 전입니다.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "지금 하교하면 조퇴로 기록됩니다.\n정말 하교 처리하시겠습니까?",
                                    fontSize = 14.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = onConfirmEarlyLeave,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text("네, 하교하겠습니다", color = Color.White)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = onCancelEarlyLeave,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                            ) {
                                Text("아니요, 더 있다 갈게요", color = MaterialTheme.colorScheme.outline)
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    )
                }

                is ActionState.Success -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val statusColor = actionState.status?.toColor() ?: Color(0xFF4CAF50)
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = actionState.message,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        actionState.status?.let {
                            Text(
                                text = "상태: ${it.toDisplayName()}",
                                fontSize = 18.sp,
                                color = it.toColor(),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Text(
                            text = "시간: ${DateTimeFormatter.formatCheckedAt(actionState.checkedAt)}",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onReset,
                            modifier = Modifier.fillMaxWidth(0.6f)
                        ) {
                            Text("확인")
                        }
                    }
                }

                is ActionState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "처리 실패",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = actionState.message,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = onReset) {
                            Text("다시 시도")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TodayStatusSection(records: List<AttendanceRecord>) {
    val policy = AttendancePolicy()
    val checkIn = records.find { it.type == AttendanceType.CHECK_IN.name }
    val checkOut = records.find { it.type == AttendanceType.CHECK_OUT.name }

    val checkInStatus = checkIn?.status?.let { AttendanceStatus.valueOf(it) }
    val checkOutStatus = checkOut?.status?.let { AttendanceStatus.valueOf(it) }
    
    val dailyStatus = policy.calculateDailyStatus(
        checkInStatus = checkInStatus, 
        checkOutStatus = checkOutStatus, 
        isCheckOutMissing = checkOut == null,
        isToday = true
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "오늘의 출결 상태",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                dailyStatus?.let {
                    Text(
                        text = it.toDisplayName(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = it.toColor(),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(it.toColor().copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusItem("등교", checkIn)
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                StatusItem("하교", checkOut)
            }
        }
    }
}

@Composable
fun StatusItem(label: String, record: AttendanceRecord?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        if (record != null) {
            Text(
                text = DateTimeFormatter.formatToTime(record.checkedAt),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "--:--:--",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

fun AttendanceStatus.toDisplayName(): String {
    return when (this) {
        AttendanceStatus.PRESENT -> "정상 등교"
        AttendanceStatus.LATE -> "지각"
        AttendanceStatus.ABSENT -> "결석"
        AttendanceStatus.EARLY_LEAVE -> "조퇴"
    }
}

fun AttendanceStatus.toColor(): Color {
    return when (this) {
        AttendanceStatus.PRESENT -> Color(0xFF4CAF50) // 초록색
        AttendanceStatus.LATE -> Color(0xFFFF9800)    // 주황색
        AttendanceStatus.ABSENT -> Color(0xFFF44336)  // 빨간색
        AttendanceStatus.EARLY_LEAVE -> Color(0xFFFF9800) // 주황색
    }
}

fun String?.toAttendanceStatusDisplayName(): String? {
    return when (this) {
        "PRESENT" -> "정상 등교"
        "LATE" -> "지각"
        "ABSENT" -> "결석"
        "EARLY_LEAVE" -> "조퇴"
        else -> null
    }
}

package com.example.nfc_attendance_app.ui.nfc

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc_attendance_app.data.model.AttendanceStatus
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        when (uiState) {
            is NfcAttendanceUiState.Waiting -> {
                Text(
                    text = "NFC 태그를 스마트폰 뒷면에 접촉해주세요.",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            is NfcAttendanceUiState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("출석 정보를 처리 중입니다...")
                }
            }

            is NfcAttendanceUiState.ConfirmEarlyLeave -> {
                AlertDialog(
                    onDismissRequest = onCancelEarlyLeave,
                    title = { Text("이른 하교 확인") },
                    text = {
                        Text("아직 하교 시작 시각 전입니다.\n지금 퇴실하면 조퇴로 기록됩니다.\n퇴실 처리하시겠습니까?")
                    },
                    confirmButton = {
                        TextButton(onClick = onConfirmEarlyLeave) {
                            Text("예")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = onCancelEarlyLeave) {
                            Text("아니요")
                        }
                    }
                )
            }

            is NfcAttendanceUiState.Success -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.message,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    uiState.status?.let {
                        Text(
                            text = "상태: ${it.toDisplayName()}",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "시간: ${DateTimeFormatter.formatCheckedAt(uiState.checkedAt)}",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onReset) {
                        Text("확인")
                    }
                }
            }

            is NfcAttendanceUiState.Error -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = uiState.message,
                        fontSize = 18.sp,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = onReset) {
                        Text("다시 시도")
                    }
                }
            }
        }
    }
}

fun AttendanceStatus.toDisplayName(): String {
    return when (this) {
        AttendanceStatus.PRESENT -> "정상 출석"
        AttendanceStatus.LATE -> "지각"
        AttendanceStatus.ABSENT -> "결석"
        AttendanceStatus.EARLY_LEAVE -> "조퇴"
    }
}

fun String?.toAttendanceStatusDisplayName(): String? {
    return when (this) {
        "PRESENT" -> "정상 출석"
        "LATE" -> "지각"
        "ABSENT" -> "결석"
        "EARLY_LEAVE" -> "조퇴"
        else -> null
    }
}

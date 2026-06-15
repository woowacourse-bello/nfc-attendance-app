package com.example.nfc_attendance_app.ui.home

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.ui.main.AttendancePolicyDialog
import com.example.nfc_attendance_app.ui.nfc.ActionState
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeRoute(
    viewModel: NfcAttendanceViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        userName = uiState.userName,
        todayRecords = uiState.todayRecords.map { it.type },
        onManualClick = { viewModel.onManualAttendanceClick() },
        onRefresh = { viewModel.refresh() }
    )
}

@Composable
fun HomeScreen(
    userName: String,
    todayRecords: List<String>,
    onManualClick: () -> Unit,
    onRefresh: () -> Unit
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var showPolicyDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    if (showPolicyDialog) {
        AttendancePolicyDialog(onDismiss = { showPolicyDialog = false })
    }

    val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.KOREA)
    val hasCheckIn = todayRecords.contains(AttendanceType.CHECK_IN.name)
    val hasCheckOut = todayRecords.contains(AttendanceType.CHECK_OUT.name)

    val buttonText = when {
        !hasCheckIn -> "등교"
        !hasCheckOut -> "하교"
        else -> "기록 완료"
    }

    val isButtonEnabled = !hasCheckIn || !hasCheckOut

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 헤더
        Box(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "우아한테크코스",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            IconButton(
                onClick = { showPolicyDialog = true },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // 2. 등/하교 기록 안내
        Text(
            text = "등/하교를 기록해 주세요",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 3. 현재 시간 표시 박스
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = timeFormatter.format(Date(currentTime)),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. 메인 동작 버튼
        Button(
            onClick = {
                if (!hasCheckIn || !hasCheckOut) {
                    onManualClick()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isButtonEnabled) Color(0xFF2196F3) else Color.LightGray,
                contentColor = Color.White
            ),
            enabled = isButtonEnabled
        ) {
            Text(
                text = buttonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. 새로고침 버튼
        TextButton(onClick = onRefresh) {
            Text(
                text = "새로고침",
                fontSize = 16.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 6. 일러스트레이션 (간단한 드로잉)
        PlanetIllustration()
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun PlanetIllustration() {
    Canvas(modifier = Modifier.size(200.dp)) {
        val center = center
        val radius = 60.dp.toPx()
        
        // 행성 원체
        drawCircle(
            color = Color.LightGray,
            radius = radius,
            center = center,
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 고리
        drawOval(
            color = Color.LightGray,
            topLeft = center.copy(x = center.x - radius - 20.dp.toPx(), y = center.y - 15.dp.toPx()),
            size = androidx.compose.ui.geometry.Size((radius + 20.dp.toPx()) * 2, 30.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        
        // 다리(?) - 이미지에 있는 외계인 형태
        drawLine(
            color = Color.LightGray,
            start = center.copy(x = center.x - 10.dp.toPx(), y = center.y + radius),
            end = center.copy(x = center.x - 10.dp.toPx(), y = center.y + radius + 30.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color.LightGray,
            start = center.copy(x = center.x + 10.dp.toPx(), y = center.y + radius),
            end = center.copy(x = center.x + 10.dp.toPx(), y = center.y + radius + 30.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        
        // 안테나(?)
        drawLine(
            color = Color.LightGray,
            start = center.copy(x = center.x - radius + 10.dp.toPx(), y = center.y - radius + 10.dp.toPx()),
            end = center.copy(x = center.x - radius - 30.dp.toPx(), y = center.y - radius - 30.dp.toPx()),
            strokeWidth = 2.dp.toPx()
        )
        
        // 점들
        drawCircle(color = Color.LightGray, radius = 2.dp.toPx(), center = center.copy(x = center.x - 20.dp.toPx(), y = center.y - 20.dp.toPx()))
        drawCircle(color = Color.LightGray, radius = 2.dp.toPx(), center = center.copy(x = center.x + 5.dp.toPx(), y = center.y - 30.dp.toPx()))
        drawCircle(color = Color.LightGray, radius = 2.dp.toPx(), center = center.copy(x = center.x - 5.dp.toPx(), y = center.y + 10.dp.toPx()))
    }
}

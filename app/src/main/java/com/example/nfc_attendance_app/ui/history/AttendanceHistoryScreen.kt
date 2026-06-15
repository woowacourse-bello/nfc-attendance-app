package com.example.nfc_attendance_app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceStatus
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.domain.AttendancePolicy
import com.example.nfc_attendance_app.ui.nfc.toColor
import com.example.nfc_attendance_app.ui.nfc.toDisplayName
import com.example.nfc_attendance_app.utils.DateTimeFormatter

@Composable
fun AttendanceHistoryRoute(
    viewModel: AttendanceHistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    // 화면이 보일 때마다 자동으로 새로고침
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    AttendanceHistoryScreen(
        uiState = uiState,
        onRefresh = { viewModel.refresh() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(
    uiState: AttendanceHistoryUiState,
    onRefresh: () -> Unit,
) {
    Scaffold(
        // topBar 제거하여 제목 노출 안 함
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (uiState) {
                AttendanceHistoryUiState.Loading -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("기록을 불러오는 중입니다.")
                    }
                }

                AttendanceHistoryUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("아직 등하교 기록이 없습니다.")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("새로고침")
                        }
                    }
                }

                is AttendanceHistoryUiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = uiState.message, color = Color.Red)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("새로고침")
                        }
                    }
                }

                is AttendanceHistoryUiState.Success -> {
                    val groupedRecords = remember(uiState.records) {
                        uiState.records.groupBy { DateTimeFormatter.formatDate(it.checkedAt) }
                    }
                    
                    val policy = remember { AttendancePolicy() }
                    val stats = remember(groupedRecords) {
                        calculateStats(groupedRecords, policy)
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // 1. 통계 요약 섹션
                        AttendanceSummarySection(stats)
                        
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("내역 새로고침")
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // 표 헤더 추가
                            item {
                                HistoryTableHeader()
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outline)
                            }
                            
                            items(groupedRecords.keys.toList()) { date ->
                                val recordsForDate = groupedRecords[date] ?: emptyList()
                                HistoryTableRow(date = date, records = recordsForDate)
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class AttendanceStats(
    val totalDays: Int,
    val normalDays: Int,
    val lateDays: Int,
    val earlyLeaveDays: Int,
    val absentDays: Int,
    val totalPoints: Int
)

fun calculateStats(groupedRecords: Map<String, List<AttendanceRecord>>, policy: AttendancePolicy): AttendanceStats {
    var normal = 0
    var late = 0
    var earlyLeave = 0
    var absent = 0
    var points = 0

    groupedRecords.values.forEach { records ->
        val checkIn = records.find { it.type == AttendanceType.CHECK_IN.name }
        val checkOut = records.find { it.type == AttendanceType.CHECK_OUT.name }

        val checkInStatus = checkIn?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
        val checkOutStatus = checkOut?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
        val dailyStatus = policy.calculateDailyStatus(checkInStatus, checkOutStatus)

        when (dailyStatus) {
            AttendanceStatus.PRESENT -> normal++
            AttendanceStatus.LATE -> {
                late++
                points += 1
            }
            AttendanceStatus.EARLY_LEAVE -> {
                earlyLeave++
                points += 1
            }
            AttendanceStatus.ABSENT -> {
                absent++
                points += 3
            }
            null -> {}
        }
    }

    return AttendanceStats(
        totalDays = groupedRecords.size,
        normalDays = normal,
        lateDays = late,
        earlyLeaveDays = earlyLeave,
        absentDays = absent,
        totalPoints = points
    )
}

@Composable
fun AttendanceSummarySection(stats: AttendanceStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
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
                Column {
                    Text(
                        text = "전체 등교 일수",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.totalDays}일",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "누적 벌점",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${stats.totalPoints}점",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stats.totalPoints > 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(20.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                MiniStatItem("정상", "${stats.normalDays}", Color(0xFF4CAF50))
                MiniStatItem("지각", "${stats.lateDays}", Color(0xFFFF9800))
                MiniStatItem("조퇴", "${stats.earlyLeaveDays}", Color(0xFFFF9800))
                MiniStatItem("결석", "${stats.absentDays}", Color(0xFFF44336))
            }
        }
    }
}

@Composable
fun MiniStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun HistoryTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "날짜",
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "등교 ~ 하교",
            modifier = Modifier.weight(2f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "상태",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun HistoryTableRow(date: String, records: List<AttendanceRecord>) {
    val policy = remember { AttendancePolicy() }
    val checkIn = records.find { it.type == AttendanceType.CHECK_IN.name }
    val checkOut = records.find { it.type == AttendanceType.CHECK_OUT.name }

    val checkInStatus = checkIn?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
    val checkOutStatus = checkOut?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
    val dailyStatus = policy.calculateDailyStatus(checkInStatus, checkOutStatus)

    // 날짜 포맷 변경 (2026-06-12 -> 2026.06.12)
    val displayDate = date.replace("-", ".")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 날짜
        Text(
            text = displayDate,
            modifier = Modifier.weight(1.2f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )

        // 2. 등교 ~ 하교 시간
        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = checkIn?.let { DateTimeFormatter.formatToTime(it.checkedAt) } ?: "--:--",
                style = MaterialTheme.typography.bodyMedium,
                color = if (checkIn != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
            Text(
                text = " ~ ",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = checkOut?.let { DateTimeFormatter.formatToTime(it.checkedAt) } ?: "--:--",
                style = MaterialTheme.typography.bodyMedium,
                color = if (checkOut != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            )
        }

        // 3. 최종 상태
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            dailyStatus?.let {
                Text(
                    text = it.toDisplayName().replace("정상 등교", "정상"),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = it.toColor(),
                    modifier = Modifier
                        .background(it.toColor().copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

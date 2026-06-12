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
        topBar = {
            TopAppBar(title = { Text("등하교 히스토리") })
        }
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

                    Column(modifier = Modifier.fillMaxSize()) {
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("새로고침")
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(groupedRecords.keys.toList()) { date ->
                                val recordsForDate = groupedRecords[date] ?: emptyList()
                                DailyHistoryCard(date = date, records = recordsForDate)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DailyHistoryCard(date: String, records: List<AttendanceRecord>) {
    val policy = remember { AttendancePolicy() }
    val checkIn = records.find { it.type == AttendanceType.CHECK_IN.name }
    val checkOut = records.find { it.type == AttendanceType.CHECK_OUT.name }

    val checkInStatus = checkIn?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
    val checkOutStatus = checkOut?.status?.let { try { AttendanceStatus.valueOf(it) } catch(e: Exception) { null } }
    val dailyStatus = policy.calculateDailyStatus(checkInStatus, checkOutStatus)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                dailyStatus?.let {
                    Text(
                        text = it.toDisplayName(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = it.toColor(),
                        modifier = Modifier
                            .background(it.toColor().copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HistoryDetailItem(label = "등교", record = checkIn)
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
                HistoryDetailItem(label = "하교", record = checkOut)
            }
        }
    }
}

@Composable
fun HistoryDetailItem(label: String, record: AttendanceRecord?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (record != null) {
            Text(
                text = DateTimeFormatter.formatToTime(record.checkedAt),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "--:--",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

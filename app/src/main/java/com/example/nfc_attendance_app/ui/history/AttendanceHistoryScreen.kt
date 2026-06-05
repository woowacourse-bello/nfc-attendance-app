package com.example.nfc_attendance_app.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.nfc_attendance_app.data.model.AttendanceRecord
import com.example.nfc_attendance_app.data.model.AttendanceType
import com.example.nfc_attendance_app.utils.DateTimeFormatter

@Composable
fun AttendanceHistoryRoute(
    viewModel: AttendanceHistoryViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

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
            TopAppBar(title = { Text("출석 히스토리") })
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
                        Text("출석 기록을 불러오는 중입니다.")
                    }
                }

                AttendanceHistoryUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("아직 출석/퇴실 기록이 없습니다.")
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
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(uiState.records) { record ->
                                HistoryItem(record = record)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(record: AttendanceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            val typeText = if (record.type == AttendanceType.CHECK_IN.name) "출석" else "퇴실"
            val typeColor = if (record.type == AttendanceType.CHECK_IN.name) 
                MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary

            Text(
                text = typeText,
                style = MaterialTheme.typography.titleMedium,
                color = typeColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "시간: ${DateTimeFormatter.formatCheckedAt(record.checkedAt)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "태그 ID: ${record.tagId}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

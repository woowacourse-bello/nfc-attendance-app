package com.example.nfc_attendance_app.ui.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfc_attendance_app.data.AttendanceRepository
import com.example.nfc_attendance_app.data.LocalUserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NfcAttendanceViewModel(
    private val repository: AttendanceRepository,
    private val preferences: LocalUserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<NfcAttendanceUiState>(NfcAttendanceUiState.Waiting)
    val uiState: StateFlow<NfcAttendanceUiState> = _uiState.asStateFlow()

    fun onNfcTagDetected(tagId: String) {
        // 중복 태깅 방지: 로딩 중이면 무시
        if (_uiState.value is NfcAttendanceUiState.Loading) return

        val userNumber = preferences.getUserNumber()
        val userName = preferences.getUserName()

        // 로그인이 필요한 상태인 경우 에러 처리
        if (userNumber == null || userName == null) {
            _uiState.value = NfcAttendanceUiState.Error("사용자 번호 로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            _uiState.value = NfcAttendanceUiState.Loading
            try {
                // Repository에서 자동으로 CHECK_IN/CHECK_OUT 판단 후 기록
                val result = repository.recordAttendance(
                    userNumber = userNumber,
                    userName = userName,
                    tagId = tagId
                )

                _uiState.value = NfcAttendanceUiState.Success(
                    message = result.message,
                    type = result.type ?: throw Exception("알 수 없는 처리 타입입니다."),
                    checkedAt = result.checkedAt
                )
            } catch (e: Exception) {
                _uiState.value = NfcAttendanceUiState.Error(
                    message = e.message ?: "출석/퇴실 처리에 실패했습니다."
                )
            }
        }
    }

    fun resetToWaiting() {
        _uiState.value = NfcAttendanceUiState.Waiting
    }
}

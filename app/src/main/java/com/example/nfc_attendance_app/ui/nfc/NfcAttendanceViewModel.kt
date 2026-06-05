package com.example.nfc_attendance_app.ui.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfc_attendance_app.data.AttendanceRepository
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.data.model.AttendanceActionResult
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
        // 중복 태깅 방지: 로딩 중이거나 이른 하교 확인 대기 중이면 무시
        if (_uiState.value is NfcAttendanceUiState.Loading) return
        if (_uiState.value is NfcAttendanceUiState.ConfirmEarlyLeave) return

        val userNumber = preferences.getUserNumber()
        val userName = preferences.getUserName()

        if (userNumber == null || userName == null) {
            _uiState.value = NfcAttendanceUiState.Error("사용자 번호 로그인이 필요합니다.")
            return
        }

        viewModelScope.launch {
            _uiState.value = NfcAttendanceUiState.Loading
            try {
                val actionResult = repository.processAttendance(
                    userNumber = userNumber,
                    userName = userName,
                    tagId = tagId
                )

                when (actionResult) {
                    is AttendanceActionResult.Saved -> {
                        val result = actionResult.result
                        _uiState.value = NfcAttendanceUiState.Success(
                            message = result.message,
                            type = result.type,
                            status = result.status,
                            checkedAt = result.checkedAt
                        )
                    }
                    is AttendanceActionResult.PendingEarlyLeave -> {
                        _uiState.value = NfcAttendanceUiState.ConfirmEarlyLeave(
                            userNumber = actionResult.userNumber,
                            userName = actionResult.userName,
                            tagId = actionResult.tagId,
                            checkedAt = actionResult.checkedAt
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = NfcAttendanceUiState.Error(
                    message = e.message ?: "출석/퇴실 처리에 실패했습니다."
                )
            }
        }
    }

    fun confirmEarlyLeave() {
        val currentState = _uiState.value
        if (currentState !is NfcAttendanceUiState.ConfirmEarlyLeave) return

        viewModelScope.launch {
            _uiState.value = NfcAttendanceUiState.Loading
            try {
                val result = repository.confirmEarlyLeave(
                    userNumber = currentState.userNumber,
                    userName = currentState.userName,
                    tagId = currentState.tagId,
                    checkedAt = currentState.checkedAt
                )

                _uiState.value = NfcAttendanceUiState.Success(
                    message = result.message,
                    type = result.type,
                    status = result.status,
                    checkedAt = result.checkedAt
                )
            } catch (e: Exception) {
                _uiState.value = NfcAttendanceUiState.Error(
                    message = e.message ?: "퇴실 처리에 실패했습니다."
                )
            }
        }
    }

    fun cancelEarlyLeave() {
        _uiState.value = NfcAttendanceUiState.Waiting
    }

    fun resetToWaiting() {
        _uiState.value = NfcAttendanceUiState.Waiting
    }
}

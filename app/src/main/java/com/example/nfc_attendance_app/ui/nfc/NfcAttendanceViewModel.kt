package com.example.nfc_attendance_app.ui.nfc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfc_attendance_app.data.AttendanceRepository
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.data.model.AttendanceActionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NfcAttendanceViewModel(
    private val repository: AttendanceRepository,
    private val preferences: LocalUserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(NfcAttendanceUiState())
    val uiState: StateFlow<NfcAttendanceUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(userName = preferences.getUserName() ?: "") }
        loadTodayRecords()
    }

    private fun loadTodayRecords() {
        val userNumber = preferences.getUserNumber() ?: return
        viewModelScope.launch {
            try {
                val records = repository.getTodayRecords(userNumber)
                _uiState.update { it.copy(todayRecords = records) }
            } catch (e: Exception) {
                // 실시간 출석 기록 로드 실패는 조용히 처리하거나 에러 상태로 전이하지 않음
            }
        }
    }

    fun onNfcTagDetected(tagId: String) {
        // 중복 태깅 방지: 로딩 중이거나 조퇴 확인 대기 중이면 무시
        if (_uiState.value.actionState is ActionState.Loading) return
        if (_uiState.value.actionState is ActionState.ConfirmEarlyLeave) return

        val userNumber = preferences.getUserNumber()
        val userName = preferences.getUserName()

        if (userNumber == null || userName == null) {
            _uiState.update { it.copy(actionState = ActionState.Error("사용자 번호 로그인이 필요합니다.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(actionState = ActionState.Loading) }
            try {
                val actionResult = repository.processAttendance(
                    userNumber = userNumber,
                    userName = userName,
                    tagId = tagId
                )

                when (actionResult) {
                    is AttendanceActionResult.Saved -> {
                        val result = actionResult.result
                        _uiState.update { 
                            it.copy(actionState = ActionState.Success(
                                message = result.message,
                                type = result.type,
                                status = result.status,
                                checkedAt = result.checkedAt
                            ))
                        }
                        // 기록 저장 후 오늘의 기록 갱신
                        loadTodayRecords()
                    }
                    is AttendanceActionResult.PendingEarlyLeave -> {
                        _uiState.update { 
                            it.copy(actionState = ActionState.ConfirmEarlyLeave(
                                userNumber = actionResult.userNumber,
                                userName = actionResult.userName,
                                tagId = actionResult.tagId,
                                checkedAt = actionResult.checkedAt
                            ))
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(actionState = ActionState.Error(
                        message = e.message ?: "출석/퇴실 처리에 실패했습니다."
                    ))
                }
            }
        }
    }

    fun confirmEarlyLeave() {
        val currentState = _uiState.value.actionState
        if (currentState !is ActionState.ConfirmEarlyLeave) return

        viewModelScope.launch {
            _uiState.update { it.copy(actionState = ActionState.Loading) }
            try {
                val result = repository.confirmEarlyLeave(
                    userNumber = currentState.userNumber,
                    userName = currentState.userName,
                    tagId = currentState.tagId,
                    checkedAt = currentState.checkedAt
                )

                _uiState.update { 
                    it.copy(actionState = ActionState.Success(
                        message = result.message,
                        type = result.type,
                        status = result.status,
                        checkedAt = result.checkedAt
                    ))
                }
                // 기록 저장 후 오늘의 기록 갱신
                loadTodayRecords()
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(actionState = ActionState.Error(
                        message = e.message ?: "퇴실 처리에 실패했습니다."
                    ))
                }
            }
        }
    }

    fun cancelEarlyLeave() {
        _uiState.update { it.copy(actionState = ActionState.Waiting) }
    }

    fun resetToWaiting() {
        _uiState.update { it.copy(actionState = ActionState.Waiting) }
    }
}

package com.example.nfc_attendance_app.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.data.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val repository: LoginRepository,
    private val preferences: LocalUserPreferences
) : ViewModel() {

    var userNumber by mutableStateOf("")
        private set

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onUserNumberChanged(value: String) {
        userNumber = value
    }

    fun login() {
        if (userNumber.isBlank()) {
            _uiState.value = LoginUiState.Error("사용자 번호를 입력해주세요.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val userInfo = repository.loginWithUserNumber(userNumber)
                preferences.saveUser(userInfo.userNumber, userInfo.name)
                _uiState.value = LoginUiState.Success
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Error(e.message ?: "로그인 처리에 실패했습니다.")
            }
        }
    }
}

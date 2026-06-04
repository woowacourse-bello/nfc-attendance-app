package com.example.nfc_attendance_app.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    userNumber: String,
    onUserNumberChanged: (String) -> Unit,
    onLoginClick: () -> Unit,
    onLoginSuccess: () -> Unit,
) {
    if (uiState is LoginUiState.Success) {
        onLoginSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "사용자 번호 로그인",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = userNumber,
            onValueChange = onUserNumberChanged,
            label = { Text("사용자 번호") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = uiState !is LoginUiState.Loading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = uiState !is LoginUiState.Loading
        ) {
            if (uiState is LoginUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("로그인")
            }
        }

        if (uiState is LoginUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.message,
                color = Color.Red,
                fontSize = 14.sp
            )
        }
    }
}

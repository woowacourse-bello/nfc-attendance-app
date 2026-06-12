package com.example.nfc_attendance_app

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.data.RealtimeAttendanceRepository
import com.example.nfc_attendance_app.data.RealtimeLoginRepository
import com.example.nfc_attendance_app.ui.login.LoginScreen
import com.example.nfc_attendance_app.ui.login.LoginViewModel
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceRoute
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceViewModel
import com.example.nfc_attendance_app.ui.theme.NfcattendanceappTheme

import com.example.nfc_attendance_app.data.RealtimeAttendanceHistoryRepository
import com.example.nfc_attendance_app.ui.main.MainScreen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.nfc_attendance_app.ui.main.BottomNavItem

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private var pendingIntent: PendingIntent? = null

    // ViewModel 및 내비게이션 상태 관리
    private var nfcViewModel: NfcAttendanceViewModel? = null
    private var externalTabRequest by mutableStateOf<BottomNavItem?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. NFC 어댑터 초기화 및 지원 여부 확인
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        checkNfcStatus()

        // 2. PendingIntent 생성 (NFC 인식 시 현재 Activity로 Intent 전달)
        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        setContent {
            NfcattendanceappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    @Composable
    fun AppNavigation() {
        val navController = rememberNavController()
        val preferences = LocalUserPreferences(applicationContext)
        val loginRepository = RealtimeLoginRepository()
        val attendanceRepository = RealtimeAttendanceRepository()
        val historyRepository = RealtimeAttendanceHistoryRepository()

        NavHost(
            navController = navController,
            startDestination = if (preferences.isLoggedIn()) "main" else "login"
        ) {
            composable("login") {
                val viewModel: LoginViewModel = viewModel {
                    LoginViewModel(loginRepository, preferences)
                }
                val uiState by viewModel.uiState.collectAsState()

                LoginScreen(
                    uiState = uiState,
                    userNumber = viewModel.userNumber,
                    onUserNumberChanged = { viewModel.onUserNumberChanged(it) },
                    onLoginClick = { viewModel.login() },
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("login") { inclusive = true }
                        }
                    }
                )
            }

            composable("main") {
                MainScreen(
                    attendanceRepository = attendanceRepository,
                    historyRepository = historyRepository,
                    preferences = preferences,
                    onNfcViewModelCreated = { nfcViewModel = it },
                    externalTabRequest = externalTabRequest,
                    onTabRequestConsumed = { externalTabRequest = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. Foreground Dispatch 활성화
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        // 4. Foreground Dispatch 비활성화
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 5. NFC 태그 감지 처리
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == intent.action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action
        ) {
            val tagId = extractTagId(intent)
            if (tagId != null) {
                // 7. 히스토리 탭 등에 있더라도 등하교 탭으로 강제 이동 후 처리
                externalTabRequest = BottomNavItem.Attendance
                nfcViewModel?.onNfcTagDetected(tagId)
            }
        }
    }

    private fun extractTagId(intent: Intent): String? {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        return tag?.id?.joinToString("") {
            "%02X".format(it)
        }
    }

    private fun checkNfcStatus() {
        when {
            nfcAdapter == null -> {
                Toast.makeText(this, "이 기기는 NFC를 지원하지 않습니다.", Toast.LENGTH_LONG).show()
            }

            !nfcAdapter!!.isEnabled -> {
                Toast.makeText(this, "NFC가 꺼져 있습니다. 설정에서 NFC를 켜주세요.", Toast.LENGTH_LONG).show()
            }
        }
    }
}

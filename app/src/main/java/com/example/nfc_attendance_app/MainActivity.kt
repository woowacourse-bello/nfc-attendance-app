package com.example.nfc_attendance_app

import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nfc_attendance_app.data.LocalUserPreferences
import com.example.nfc_attendance_app.data.RealtimeAttendanceHistoryRepository
import com.example.nfc_attendance_app.data.RealtimeAttendanceRepository
import com.example.nfc_attendance_app.data.RealtimeLoginRepository
import com.example.nfc_attendance_app.ui.login.LoginScreen
import com.example.nfc_attendance_app.ui.login.LoginViewModel
import com.example.nfc_attendance_app.ui.main.BottomNavItem
import com.example.nfc_attendance_app.ui.main.MainScreen
import com.example.nfc_attendance_app.ui.nfc.NfcAttendanceViewModel
import com.example.nfc_attendance_app.ui.theme.NfcattendanceappTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    // ViewModel 및 내비게이션 상태 관리
    private var nfcViewModel: NfcAttendanceViewModel? = null
    private var externalTabRequest by mutableStateOf<BottomNavItem?>(null)
    private var pendingTagId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. NFC 어댑터 초기화 및 지원 여부 확인
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        checkNfcStatus()

        // 초기 인텐트 처리 (Cold Start 대응: 앱이 꺼진 상태에서 NFC로 켜질 때)
        // 안드로이드 14+에서는 Manifest의 intent-filter를 통해 시스템이 직접 실행합니다.
        handleIntent(getIntent())

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
        val context = applicationContext
        val preferences = LocalUserPreferences(context)
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
                    onNfcViewModelCreated = { viewModel ->
                        nfcViewModel = viewModel
                        // ViewModel이 준비되면 대기 중인 태그 처리
                        pendingTagId?.let { tagId ->
                            externalTabRequest = BottomNavItem.Attendance
                            viewModel.onNfcTagDetected(tagId)
                            pendingTagId = null
                        }
                    },
                    externalTabRequest = externalTabRequest,
                    onTabRequestConsumed = { externalTabRequest = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 3. Reader Mode 활성화 (Android 14+ 권장 방식)
        // Foreground Dispatch(PendingIntent 방식)보다 안정적이며 백그라운드 제한 문제를 피할 수 있습니다.
        nfcAdapter?.let { adapter ->
            val options = Bundle()
            // 필요한 경우 특수 옵션 추가 가능
            adapter.enableReaderMode(this, { tag ->
                val tagId = tag.id.joinToString("") { "%02X".format(it) }
                runOnUiThread {
                    processDetectedTag(tagId)
                }
            }, NfcAdapter.FLAG_READER_NFC_A or 
               NfcAdapter.FLAG_READER_NFC_B or 
               NfcAdapter.FLAG_READER_NFC_F or 
               NfcAdapter.FLAG_READER_NFC_V or 
               NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS, options)
        }
    }

    override fun onPause() {
        super.onPause()
        // 4. Reader Mode 비활성화
        nfcAdapter?.disableReaderMode(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 새로운 인텐트(앱 실행 중 Manifest intent-filter로 인식)를 현재 인텐트로 설정
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action
        if (NfcAdapter.ACTION_TAG_DISCOVERED == action ||
            NfcAdapter.ACTION_TECH_DISCOVERED == action ||
            NfcAdapter.ACTION_NDEF_DISCOVERED == action
        ) {
            val tagId = extractTagId(intent)
            if (tagId != null) {
                processDetectedTag(tagId)
            }
        }
    }

    private fun processDetectedTag(tagId: String) {
        // 햅틱 피드백 제공 (인식 성공 알림)
        vibrate()
        
        // 인식 알림 토스트
        Toast.makeText(this, "NFC가 인식되었습니다", Toast.LENGTH_SHORT).show()
        
        val currentViewModel = nfcViewModel
        if (currentViewModel != null) {
            // 이미 메인 화면인 경우 즉시 처리
            externalTabRequest = BottomNavItem.Attendance
            currentViewModel.onNfcTagDetected(tagId)
        } else {
            // 아직 초기화 전이거나 로그인 전인 경우 대기열에 저장
            pendingTagId = tagId
        }
    }

    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    private fun extractTagId(intent: Intent): String? {
        val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        }

        // ByteArray를 Hex String으로 변환
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

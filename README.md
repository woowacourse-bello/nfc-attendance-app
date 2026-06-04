# NFC Attendance MVP (Firebase Realtime Database)

Android NFC 기술과 Firebase Realtime Database를 활용한 초간편 출석 체크 MVP 앱입니다. 별도의 회원가입 없이 관리자가 등록한 사용자 번호를 통해 로그인하고, NFC 태그 접촉만으로 출석과 퇴실을 자동으로 기록합니다.

## 프로젝트 목표
- **Firebase Authentication/Firestore 미사용**: Realtime Database(RTDB)만으로 기능 구현.
- **자동 출퇴근 판단**: 사용자의 별도 선택 없이 시스템이 기록 현황에 따라 `CHECK_IN`/`CHECK_OUT`을 결정.
- **최소 데이터 설계**: MVP 목적에 맞게 핵심 데이터(`userNumber`, `userName`, `tagId`, `type`, `checkedAt`)만 저장.

## 기술 스택
- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Architecture**: MVVM
- **Database**: Firebase Realtime Database
- **Asynchronous**: Kotlin Coroutines & StateFlow
- **Local Storage**: SharedPreferences
- **NFC**: Foreground Dispatch System

## 파일 구조
```text
com.example.nfc_attendance_app/
├── data/
│   ├── model/
│   │   ├── User.kt               // 사용자 정보 (userNumber, userName, isActive)
│   │   ├── NfcTag.kt             // 태그 정보 (tagId, isActive)
│   │   └── AttendanceRecord.kt   // 출결 기록 (userNumber, userName, tagId, type, checkedAt)
│   ├── repository/
│   │   ├── UserRepository.kt     // 사용자 조회 및 로그인 관리
│   │   └── AttendanceRepository.kt // NFC 검증 및 기록 Read/Write
│   └── local/
│       └── PreferenceManager.kt  // SharedPreferences 관리
├── ui/
│   ├── login/                    // 로그인 화면 (번호 입력 및 검증)
│   ├── attendance/               // 메인 출석 화면 (NFC 대기 및 결과 표시)
│   └── components/               // 공통 UI 컴포넌트
├── utils/
│   └── DateFormatter.kt          // Timestamp 변환 도구
└── MainActivity.kt               // NFC Intent 수신 및 네비게이션
```

## Realtime Database 구조
```json
{
  "users": {
    "{userNumber}": {
      "userName": "홍길동",
      "isActive": true
    }
  },
  "nfcTags": {
    "{tagId}": {
      "isActive": true
    }
  },
  "attendanceRecords": {
    "{yyyy-MM-dd}": {
      "{userNumber}": {
        "{pushId}": {
          "userNumber": "12345",
          "userName": "홍길동",
          "tagId": "NFC_ID_001",
          "type": "CHECK_IN",
          "checkedAt": 1717468800000
        }
      }
    }
  }
}
```

## 전체 프로세스
1. **임시 로그인**: `users/{userNumber}` 조회 후 `isActive == true`이면 성공. 정보를 `SharedPreferences`에 저장.
2. **NFC 태그 인식**: 앱 실행 중 NFC 태그 접촉 시 `tagId` 추출.
3. **태그 검증**: `nfcTags/{tagId}` 조회 후 활성 상태 확인.
4. **자동 판단 로직**:
   - 오늘 날짜의 해당 유저 기록이 없으면 → **CHECK_IN** 저장.
   - **CHECK_IN** 기록만 있으면 → **CHECK_OUT** 저장.
   - 둘 다 존재하면 → **이미 완료됨** 처리.
5. **결과 표시**: 성공/실패 상태를 UI에 표시.

## MVP 한계 및 주의사항
- **보안**: 사용자 번호만으로 인증하므로 실제 서비스용으로는 부적합합니다.
- **네트워크**: 온라인 환경에서만 작동하며, 오프라인 기록 저장은 지원하지 않습니다.

## 구현 단계
1. **Step 1**: Firebase 설정 및 NFC 권한 세팅
2. **Step 2**: 데이터 모델 및 SharedPreferences 구현
3. **Step 3**: 사용자 번호 기반 로그인 기능 구현
4. **Step 4**: NFC 태그 ID 인식 및 검증 로직 구현
5. **Step 5**: 출퇴근 자동 판단 및 기록 저장 로직 구현
6. **Step 6**: UI 상태 피드백 및 최종 테스트

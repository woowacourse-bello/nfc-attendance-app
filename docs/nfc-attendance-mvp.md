# NFC 출석 MVP 구현 문서

## 1. 기능 개요
NFC 태그와 안드로이드 스마트폰을 활용하여 사용자의 출석 및 퇴실을 자동으로 기록하는 시스템입니다. Firebase Realtime Database를 연동하여 실시간으로 데이터를 관리하며, 별도의 출석/퇴실 버튼 선택 없이 태그 접촉만으로 상태를 자동 판단합니다.

## 2. MVP 범위
- 사용자 번호 기반의 간편 로그인
- NFC 태그 인식 및 유효성 검증
- 출석/퇴실 상태 자동 판별 및 기록 저장
- 실시간 데이터 동기화 (Firebase)
- 로컬 기기에 로그인 세션 유지 (SharedPreferences)

## 3. 전체 동작 흐름
1. **앱 실행**: 최초 실행 시 로그인 화면 표시.
2. **사용자 로그인**: 사용자 번호 입력 → `users/{userNumber}` 조회 → 유효한 사용자 확인.
3. **상태 저장**: 로그인 성공 시 `SharedPreferences`에 `userNumber`, `userName` 저장.
4. **NFC 대기**: NFC 출석 화면에서 태그 접촉 대기.
5. **태그 인식**: 스마트폰에 NFC 태그 접촉 → 태그 ID(Hex) 추출.
6. **태그 검증**: `nfcTags/{tagId}` 조회하여 활성화(`isActive: true`) 여부 확인.
7. **기록 조회**: `attendanceRecords`에서 해당 사용자의 오늘 날짜 기록 확인.
8. **상태 판단**: 오늘 기록이 없으면 `CHECK_IN`, 출석 기록만 있으면 `CHECK_OUT`.
9. **저장 및 완료**: 결과를 Database에 저장하고 UI에 성공 메시지 표시.

## 4. 기술 스택
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Database**: Firebase Realtime Database
- **Local Storage**: SharedPreferences
- **NFC**: Android NFC Basic (NfcAdapter)

## 5. Realtime Database 데이터 구조

### 사용자 데이터 (`users`)
```json
{
  "users": {
    "2026001": {
      "userNumber": "2026001",
      "name": "김민수",
      "isActive": true
    }
  }
}
```

### NFC 태그 데이터 (`nfcTags`)
```json
{
  "nfcTags": {
    "04A1B2C3D4": {
      "tagId": "04A1B2C3D4",
      "isActive": true
    }
  }
}
```

### 출석 기록 데이터 (`attendanceRecords`)
```json
{
  "attendanceRecords": {
    "-Nxxxxx...": {
      "userNumber": "2026001",
      "userName": "김민수",
      "tagId": "04A1B2C3D4",
      "type": "CHECK_IN",
      "checkedAt": 1717200600000
    }
  }
}
```

## 6. 사용자 번호 기반 임시 로그인
- **방식**: 사전에 등록된 `userNumber`를 입력하여 로그인.
- **장점**: 별도의 회원가입이나 복잡한 인증 없이 즉시 사용 가능.
- **한계**: 
  - 보안성이 낮음 (사용자 번호만 알면 타인 계정으로 접근 가능).
  - 비밀번호나 2단계 인증이 없음.
  - 관리자가 사전에 `users` 노드에 데이터를 직접 등록해야 함.

## 7. SharedPreferences 로그인 상태 저장
- 로그인 성공 시 `userNumber`와 `userName`을 기기에 영구 저장.
- 앱 재실행 시 저장된 정보가 있으면 로그인 화면을 건너뛰고 바로 NFC 출석 화면으로 진입.
- 로그아웃 시 저장된 데이터를 초기화.

## 8. NFC 인식 방식
1. `MainActivity`에서 `NfcAdapter`를 통해 `NDEF` 또는 기본 `TAG` 인텐트 감지.
2. `Tag` 객체에서 `id` 바이트 배열을 추출.
3. 바이트 배열을 16진수 문자열(Hex String)로 변환 (예: `04A1B2C3D4`).
4. 추출된 ID를 `ViewModel`로 전달하여 처리.

## 9. 주요 파일 구조
- `MainActivity.kt`: NFC 인텐트 수신 및 ViewModel 전달.
- `data/model/`: 데이터 클래스 (`AttendanceRecord`, `UserInfo`, `NfcTagInfo`).
- `data/repository/`: Firebase API 호출 및 비즈니스 로직.
- `ui/login/`: 로그인 화면 구성 및 ViewModel.
- `ui/nfc/`: 출석 화면 구성 및 ViewModel.
- `utils/DateTimeFormatter.kt`: 타임스탬프 표시를 위한 포맷터.

## 10. 화면 상태 설계
`NfcAttendanceUiState`를 사용하여 상태를 관리:
- `Waiting`: 태그 대기 중 (기본 상태).
- `Loading`: 태그 인식 후 서버 처리 중.
- `Success`: 출석/퇴실 처리 성공 (결과 메시지 표시).
- `Error`: 유효하지 않은 태그나 중복 기록 등 에러 발생.

## 11. 출석/퇴실 자동 판단 방식
앱이 자동으로 사용자의 오늘 기록을 조회하여 타입을 결정합니다:
- **CHECK_IN**: 오늘 날짜의 `CHECK_IN` 기록이 없는 경우.
- **CHECK_OUT**: 오늘 날짜의 `CHECK_IN` 기록은 있으나, `CHECK_OUT` 기록이 없는 경우.
- **처리 불가**: 오늘 날짜에 `CHECK_IN`, `CHECK_OUT` 기록이 모두 존재하는 경우.

## 12. checkedAt timestamp 저장 방식
- 서버 시간이 아닌 앱 내 `System.currentTimeMillis()`를 사용하여 밀리초 단위 타임스탬프로 저장.
- 정수형(`Long`) 데이터로 저장되어 데이터 용량이 효율적이며 시간 연산이 용이함.

## 13. checkedAt formatter 표시 방식
- `DateTimeFormatter.kt`를 통해 사용자에게 친숙한 포맷으로 변환.
- 포맷 예시: `yyyy-MM-dd HH:mm:ss` (예: 2026-06-05 09:30:15).

## 14. Realtime Database 저장 흐름
1. `push()`를 사용하여 고유한 Push ID를 키로 생성.
2. `setValue(record)`를 호출하여 객체 데이터를 저장.
3. `await()`을 사용하여 비동기 처리가 완료될 때까지 대기 후 UI 업데이트.

## 15. Realtime Database Rules
MVP 단계의 보안 규칙 (테스트용):
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": { ".read": true },
    "nfcTags": { ".read": true },
    "attendanceRecords": {
      ".indexOn": ["userNumber"]
    }
  }
}
```

## 16. 테스트 시나리오
1. **정상 출석**: 미출석 상태에서 태그 → `CHECK_IN` 저장 확인.
2. **정상 퇴실**: 출석 상태에서 태그 → `CHECK_OUT` 저장 확인.
3. **중복 태깅**: 퇴실 완료 후 다시 태그 → "이미 완료되었습니다" 에러 메시지 확인.
4. **미등록 태그**: 등록되지 않은 NFC 태그 접촉 → 에러 메시지 확인.
5. **재로그인**: 앱 종료 후 재실행 → 자동 로그인 되어 출석 화면 진입 확인.

## 17. MVP에서 제외한 기능
- **역할 및 위치 정보**: `role`, `locationName`, `roomName` 필드 제외.
- **수동 선택**: 출석/퇴실 버튼 선택 UI 제외 (자동 판단으로 대체).
- **텍스트 날짜**: `checkedAtText` 필드 제외 (Timestamp만 사용).
- **관리자 모드**: 태그 등록 및 사용자 관리 UI.
- **통계**: 주간/월간 출석부 보기 기능.

## 18. 현재 구조의 한계
- **보안**: 사용자 번호만으로 로그인이 가능하여 계정 도용 위험 존재.
- **시간 조작**: 서버 시간이 아닌 클라이언트 시간을 저장하므로 기기 시간 변경에 취약.
- **오프라인**: 인터넷 연결이 끊긴 상태에서는 출석 처리가 불가능.
- **NFC 호환성**: 일부 NFC 포맷이나 기기에 따라 태그 인식이 원활하지 않을 수 있음.

## 19. 향후 개선 방향
- **Firebase Auth 연동**: 이메일/비밀번호 또는 전화번호 인증 도입.
- **Server Timestamp**: `ServerValue.TIMESTAMP`를 사용하여 시간 조작 방지.
- **Offline Persistence**: Firebase 오프라인 캐시 활성화 및 동기화 처리.
- **Location 서비스**: 출석 시 GPS 위치 정보를 함께 저장하여 부정 출석 방지.
- **Admin App**: 관리자가 현장에서 즉시 태그를 등록하고 사용자를 관리하는 기능.

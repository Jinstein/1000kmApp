# 아키텍처 문서

## 개요

1000km 걷기 챌린지 앱은 iOS와 Android 두 플랫폼에서 동일한 기능을 제공하는 네이티브 앱입니다.
두 플랫폼 모두 **MVVM(Model-View-ViewModel)** 아키텍처를 사용합니다.

---

## 계층 구조

```
┌─────────────────────────────────┐
│           View Layer            │  UI 렌더링 및 사용자 입력 처리
│  (SwiftUI / Jetpack Compose)    │
└────────────┬────────────────────┘
             │ observes / collects
┌────────────▼────────────────────┐
│        ViewModel Layer          │  비즈니스 로직 및 상태 관리
│  (ObservableObject / ViewModel) │
└────────────┬────────────────────┘
             │ read/write
┌────────────▼────────────────────┐
│          Model Layer            │  데이터 구조 정의
│       (WalkEntry struct)        │
└────────────┬────────────────────┘
             │ persist
┌────────────▼────────────────────┐
│        Storage Layer            │  로컬 영구 저장
│  (UserDefaults / SharedPrefs)   │
└─────────────────────────────────┘
```

---

## 플랫폼별 구현 비교

### 모델 (WalkEntry)

| 항목 | iOS (Swift) | Android (Kotlin) |
|---|---|---|
| 파일 | `ios/1000kmApp/WalkEntry.swift` | `android/.../WalkEntry.kt` |
| 타입 | `struct` (Codable) | `data class` |
| ID | `UUID` | `UUID.randomUUID().toString()` |
| 날짜 | `Date` | `Long` (milliseconds) |
| 직렬화 | `JSONEncoder/JSONDecoder` | `Gson` |

**iOS:**
```swift
struct WalkEntry: Identifiable, Codable {
    let id: UUID
    let date: Date
    let distance: Double
}
```

**Android:**
```kotlin
data class WalkEntry(
    val id: String = UUID.randomUUID().toString(),
    val dateMillis: Long = System.currentTimeMillis(),
    val distance: Double
)
```

---

### ViewModel (WalkViewModel)

| 항목 | iOS (Swift) | Android (Kotlin) |
|---|---|---|
| 파일 | `ios/1000kmApp/WalkViewModel.swift` | `android/.../WalkViewModel.kt` |
| 기반 클래스 | `ObservableObject` | `AndroidViewModel` |
| 상태 발행 | `@Published` | `MutableStateFlow` |
| 구독 | SwiftUI 자동 관찰 | `collectAsStateWithLifecycle()` |

**공통 상태 및 메서드:**

| 상태/메서드 | 역할 |
|---|---|
| `entries` | 걷기 기록 목록 (최신순) |
| `inputKmText` | 입력 필드 문자열 |
| `totalDistance` | 누적 거리 합계 |
| `remainingDistance` | 잔여 거리 (max 0) |
| `progress` | 0.0~1.0 진행률 |
| `goalReached` | 1000km 달성 여부 |
| `addEntry()` | 새 기록 추가 (유효성 검사 포함) |
| `deleteEntry(id)` | 특정 기록 삭제 |
| `resetChallenge()` | 챌린지 리셋 + 아카이브 |

---

### View 컴포넌트 구성

```
WalkChallengeApp (루트)
├── ProgressRingSection       원형 진행률 링
│   └── 목표 달성 시 축하 메시지 표시
├── StatCardsRow              통계 카드 행
│   ├── StatCard (총 거리)
│   └── StatCard (남은 거리)
├── InputRow                  거리 입력 행
│   ├── TextField
│   └── 추가 버튼
├── 걷기 기록 목록
│   └── EntryRow (× n)       각 기록 행 (스와이프 삭제)
└── ResetDialog               챌린지 리셋 확인 다이얼로그
```

---

## 데이터 흐름

```
사용자 입력 (거리 입력 + 추가 버튼)
        │
        ▼
  ViewModel.addEntry()
  ┌─ 유효성 검사 (0 < distance ≤ 500)
  ├─ WalkEntry 생성
  ├─ entries 목록 앞에 삽입
  └─ SharedPrefs/UserDefaults 저장
        │
        ▼
  State 변경 (entries, totalDistance, progress 등)
        │
        ▼
  View 자동 재렌더링
  ┌─ 진행률 링 업데이트
  ├─ 통계 카드 숫자 갱신
  └─ 기록 목록 새 항목 표시
```

---

## 데이터 저장 구조

앱은 기기 로컬 저장소를 사용합니다. 서버 통신 없이 모든 데이터가 기기에 저장됩니다.

### 저장 키

| 키 | 내용 |
|---|---|
| `walkEntries_current` | 현재 진행 중인 챌린지의 기록 목록 (JSON 배열) |
| `walkEntries_archive_{timestamp}` | 리셋 시 보관된 이전 챌린지 데이터 |

### 저장 형식 (JSON)

```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "dateMillis": 1709500800000,
    "distance": 5.3
  },
  {
    "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "dateMillis": 1709414400000,
    "distance": 8.0
  }
]
```

---

## 유효성 검사 규칙

거리 입력 시 다음 규칙이 적용됩니다:

1. 빈 문자열 → 거부
2. 숫자가 아닌 값 → 거부
3. 0 이하 → 거부
4. 500 초과 → 거부 (하루 최대 500km)
5. 소수점 구분자: `.` 또는 `,` 모두 허용 (로케일 대응)

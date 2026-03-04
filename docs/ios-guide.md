# iOS 개발 가이드

## 요구사항

| 항목 | 버전 |
|---|---|
| macOS | 13.0 (Ventura) 이상 권장 |
| Xcode | 14.0 이상 |
| Swift | 5.0 이상 |
| iOS 배포 타겟 | 16.0 이상 |

---

## 프로젝트 구조

```
ios/
├── 1000kmApp.xcodeproj/              Xcode 프로젝트 파일
│   ├── project.pbxproj               빌드 설정, 파일 참조
│   └── project.xcworkspace/
│       └── contents.xcworkspacedata
└── 1000kmApp/                        앱 소스 코드
    ├── App.swift                     앱 진입점
    ├── ContentView.swift             전체 UI 컴포넌트
    ├── WalkEntry.swift               데이터 모델
    ├── WalkViewModel.swift           상태 관리
    ├── Info.plist                    앱 메타데이터
    └── Assets.xcassets/              이미지 및 아이콘 에셋
        ├── AppIcon.appiconset/
        └── Contents.json
```

---

## 소스 파일 설명

### App.swift

SwiftUI 앱의 진입점입니다. `@main` 어트리뷰트로 앱 시작점을 지정하고, `WindowGroup` 안에 `ContentView`를 루트 뷰로 설정합니다.

```swift
@main
struct thousandkmApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
```

---

### WalkEntry.swift

걷기 기록 하나를 나타내는 데이터 모델입니다.

```swift
struct WalkEntry: Identifiable, Codable {
    let id: UUID        // 고유 식별자
    let date: Date      // 기록 시각
    let distance: Double // 거리 (km)
}
```

- `Identifiable`: SwiftUI List에서 각 항목을 고유하게 구분
- `Codable`: JSON 직렬화/역직렬화 자동 지원 (UserDefaults 저장용)

---

### WalkViewModel.swift

앱의 비즈니스 로직과 상태를 관리하는 ViewModel입니다.

**주요 Published 프로퍼티:**

| 프로퍼티 | 타입 | 역할 |
|---|---|---|
| `entries` | `[WalkEntry]` | 걷기 기록 목록 (최신순) |
| `inputKmText` | `String` | 거리 입력 필드 값 |
| `showResetConfirm` | `Bool` | 리셋 확인 다이얼로그 표시 여부 |

**주요 Computed 프로퍼티:**

| 프로퍼티 | 타입 | 계산식 |
|---|---|---|
| `totalDistance` | `Double` | entries 합산 |
| `remainingDistance` | `Double` | `max(0, 1000 - total)` |
| `progress` | `Double` | `total / 1000` (0~1 범위) |
| `goalReached` | `Bool` | `totalDistance >= 1000` |

**주요 메서드:**

```swift
func addEntry()
// 1. inputKmText를 Double로 파싱 (콤마→점 변환 포함)
// 2. 유효성 검사 (0 < distance ≤ 500)
// 3. WalkEntry 생성 후 entries 앞에 삽입
// 4. UserDefaults에 저장

func deleteEntry(id: UUID)
// entries에서 해당 id 항목 제거 후 저장

func resetChallenge()
// 현재 entries를 타임스탬프 키로 아카이브
// entries 초기화 후 저장

func loadEntries() / saveEntries()
// UserDefaults ↔ [WalkEntry] JSON 변환
```

**저장 키:**
- `walkEntries_current`: 현재 챌린지 데이터
- `walkEntries_archive_{timestamp}`: 리셋 시 보관 데이터

---

### ContentView.swift

앱의 전체 UI를 구성하는 SwiftUI 뷰 파일입니다. 다음 컴포넌트로 구성됩니다.

#### ProgressRingView

원형 진행률 링을 그리는 뷰입니다.

```
- 직경: 220pt, 선 두께: 20pt
- 배경 링: 회색 (Color(UIColor.systemGray5))
- 진행 링: 파란색 (#007AFF), 목표 달성 시 초록색 (#34C759)
- 중앙 텍스트: 현재 거리 km / 달성률 %, 또는 목표 달성 축하 메시지
- 애니메이션: trim(0, progress) 변화 시 withAnimation 적용
```

#### StatCardsView

총 거리와 남은 거리를 나란히 보여주는 카드 뷰입니다.

```
- 총 거리 카드: 파란색 (#007AFF), 아이콘: figure.walk
- 남은 거리 카드: 주황색 (#FF9500), 아이콘: flag.checkered
- RoundedRectangle 배경 (12pt 모서리)
- 폰트: .monospacedDigit (숫자 정렬)
```

#### InputRowView

거리 입력 및 추가 기능을 제공하는 뷰입니다.

```
- TextField: 소수점 키보드 (.decimalPad)
- 추가 버튼: 입력값 유효할 때 활성화
- 로케일 인식: 쉼표(,)를 점(.)으로 자동 변환
- 유효 범위: 0 초과, 500 이하
```

#### EntryListView / EntryRowView

걷기 기록 목록을 표시하는 뷰입니다.

```
- 최신 기록이 상단에 표시 (entries는 이미 역순)
- 날짜: "ko_KR" 로케일로 포맷 (예: 2026년 3월 4일 오후 3:22)
- 스와이프 왼쪽: 삭제 버튼 (빨간 배경, trash 아이콘)
- 빈 목록: 안내 메시지 표시
```

---

## 시뮬레이터 실행

```bash
# Xcode에서 열기
open ios/1000kmApp.xcodeproj

# 또는 xcodebuild로 빌드
cd ios
xcodebuild -project 1000kmApp.xcodeproj \
           -scheme 1000kmApp \
           -destination 'platform=iOS Simulator,name=iPhone 15' \
           build
```

---

## 실제 기기 배포

1. **Apple Developer 계정 필요** (무료 계정으로도 가능, 단 7일 만료)
2. Xcode → Signing & Capabilities → Team 선택
3. Bundle Identifier를 고유값으로 변경 (예: `com.yourname.thousandkm`)
4. 기기 연결 후 신뢰 설정
5. Cmd+R로 빌드 및 설치
6. 기기에서: **설정 → 일반 → VPN 및 기기 관리 → 개발자 앱 → 신뢰**

---

## 빌드 설정

`ios/1000kmApp.xcodeproj/project.pbxproj` 주요 설정:

| 설정 | 값 |
|---|---|
| `SWIFT_VERSION` | 5.0 |
| `IPHONEOS_DEPLOYMENT_TARGET` | 16.0 |
| `TARGETED_DEVICE_FAMILY` | 1 (iPhone) |
| `SUPPORTS_MACCATALYST` | NO |
| 지원 방향 | Portrait 전용 |

---

## 의존성

외부 라이브러리 없음. Swift Package Manager 불필요.

- SwiftUI (iOS 16.0+ 내장)
- Foundation (UUID, Date, UserDefaults)

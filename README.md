# 1000km 걷기 챌린지 앱

1년 동안 누적 1000km 걷기 목표를 달성하기 위한 모바일 앱입니다.
iOS(SwiftUI)와 Android(Jetpack Compose) 네이티브 앱을 모두 제공합니다.

## 기능

- 하루 걸은 거리(km) 입력 및 기록
- 누적 거리 및 남은 거리 실시간 표시
- 원형 프로그레스 링으로 달성률 시각화 (목표 달성 시 초록색으로 변경)
- 기록 스와이프 삭제
- 새 도전 시작 (수동 초기화, 이전 기록 자동 보관)
- 로케일 인식 소수점 입력 (콤마/점 모두 지원)

## 프로젝트 구조

```
1000kmApp/
├── ios/           # iOS 앱 (Swift + SwiftUI)
├── android/       # Android 앱 (Kotlin + Jetpack Compose)
└── docs/          # 프로젝트 문서
```

## 플랫폼별 실행 방법

### iOS

> **요구사항:** macOS, Xcode 14.0+, iOS 16.0+

**시뮬레이터:**
1. `ios/1000kmApp.xcodeproj` 더블클릭 → Xcode 열기
2. 상단에서 **iPhone 시뮬레이터** 선택
3. **Cmd+R** 또는 ▶ 버튼으로 실행

**실제 기기:**
1. USB로 아이폰 연결 → "이 컴퓨터를 신뢰하겠습니까?" → **신뢰** 선택
2. Xcode 상단에서 **내 아이폰** 선택
3. **Signing & Capabilities** → Apple ID 및 Bundle Identifier 설정
4. **Cmd+R** 실행 후, 아이폰에서 **설정 → 일반 → VPN 및 기기 관리 → 개발자 앱 → 신뢰**

> 무료 Apple ID로 설치한 앱은 7일 후 만료됩니다.

### Android

> **요구사항:** Android Studio, JDK 8+, Android 8.0(API 26)+

**에뮬레이터:**
1. Android Studio에서 `android/` 폴더 열기
2. AVD Manager에서 에뮬레이터 생성 및 실행
3. **Run** (▶) 버튼으로 빌드 및 실행

**실제 기기:**
1. 기기에서 **개발자 옵션 → USB 디버깅** 활성화
2. USB로 기기 연결
3. Android Studio에서 기기 선택 후 **Run** (▶)

**커맨드라인 빌드:**
```bash
cd android
./gradlew assembleDebug
```

자세한 내용은 [docs/](docs/) 폴더를 참고하세요.

## 기술 스택

| | iOS | Android |
|---|---|---|
| 언어 | Swift 5.0+ | Kotlin |
| UI | SwiftUI | Jetpack Compose |
| 아키텍처 | MVVM | MVVM |
| 상태 관리 | @Published / ObservableObject | StateFlow / ViewModel |
| 데이터 저장 | UserDefaults | SharedPreferences + Gson |
| 디자인 | SF Symbols + iOS HIG | Material Design 3 |
| 최소 버전 | iOS 16.0 | Android 8.0 (API 26) |

## 라이선스

MIT License © 2026 Jinstein

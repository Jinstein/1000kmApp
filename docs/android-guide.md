# Android 개발 가이드

## 요구사항

| 항목 | 버전 |
|---|---|
| Android Studio | Hedgehog (2023.1.1) 이상 권장 |
| JDK | 8 이상 |
| Gradle | 8.7 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 2.0.0 |
| 최소 SDK | API 26 (Android 8.0 Oreo) |
| 타겟 SDK | API 34 (Android 14) |

---

## 프로젝트 구조

```
android/
├── settings.gradle.kts                  프로젝트 이름, 모듈 설정
├── build.gradle.kts                     프로젝트 레벨 플러그인 설정
├── gradle/
│   ├── libs.versions.toml               버전 카탈로그 (의존성 버전 중앙 관리)
│   └── wrapper/
│       └── gradle-wrapper.properties    Gradle 래퍼 설정
└── app/
    ├── build.gradle.kts                 앱 모듈 빌드 설정
    └── src/main/
        ├── AndroidManifest.xml          앱 선언, 권한, 액티비티 등록
        ├── java/com/jinstein/thousandkm/
        │   ├── MainActivity.kt          UI 진입점 및 Compose 컴포넌트
        │   ├── WalkEntry.kt             데이터 모델
        │   └── WalkViewModel.kt         비즈니스 로직 및 상태 관리
        └── res/
            └── values/
                └── strings.xml          문자열 리소스
```

---

## 소스 파일 설명

### WalkEntry.kt

걷기 기록 하나를 나타내는 데이터 모델입니다.

```kotlin
data class WalkEntry(
    val id: String = UUID.randomUUID().toString(), // 고유 식별자
    val dateMillis: Long = System.currentTimeMillis(), // 기록 시각 (밀리초)
    val distance: Double                            // 거리 (km)
)
```

- `data class`: 자동 생성되는 `equals()`, `hashCode()`, `copy()`, `toString()`
- `dateMillis`: `Long` 타입으로 저장하여 Gson 직렬화 단순화
- Gson으로 JSON 직렬화 후 SharedPreferences에 저장

---

### WalkViewModel.kt

비즈니스 로직과 UI 상태를 관리하는 ViewModel입니다.

```kotlin
class WalkViewModel(application: Application) : AndroidViewModel(application)
```

**상태 (StateFlow):**

| 프로퍼티 | 타입 | 역할 |
|---|---|---|
| `entries` | `StateFlow<List<WalkEntry>>` | 걷기 기록 목록 (최신순) |
| `inputKmText` | `StateFlow<String>` | 거리 입력 필드 값 |

**Computed 프로퍼티 (get):**

| 프로퍼티 | 타입 | 계산 |
|---|---|---|
| `totalDistance` | `Double` | entries 합산 |
| `remainingDistance` | `Double` | `max(0, 1000 - total)` |
| `progress` | `Float` | `(total / 1000).coerceIn(0, 1)` |
| `goalReached` | `Boolean` | `totalDistance >= 1000` |

**주요 메서드:**

```kotlin
fun addEntry()
// 1. inputKmText에서 콤마(,)를 점(.)으로 변환
// 2. Double 파싱 실패 시 return
// 3. 유효 범위 검사 (0 < distance ≤ 500)
// 4. WalkEntry 생성 후 목록 앞에 삽입
// 5. SharedPreferences에 JSON 저장

fun deleteEntry(id: String)
// entries에서 해당 id 필터링 후 저장

fun resetChallenge()
// 현재 entries를 타임스탬프 키로 아카이브 저장
// entries 초기화

private fun loadEntries() / saveEntries()
// SharedPreferences ↔ List<WalkEntry> Gson 변환
```

**SharedPreferences 저장 키:**
- `walkEntries_current`: 현재 챌린지 JSON 배열
- `walkEntries_archive_{timestamp}`: 리셋 시 보관 데이터

---

### MainActivity.kt

Compose UI 컴포넌트가 모두 정의된 파일입니다.

#### WalkChallengeApp (루트 컴포저블)

```kotlin
@Composable
fun WalkChallengeApp(vm: WalkViewModel = viewModel())
```

- `Scaffold`로 TopAppBar (타이틀 + 리셋 버튼) + 본문 구성
- `LazyColumn`으로 스크롤 가능한 수직 레이아웃
- `entries`, `inputText`를 `collectAsStateWithLifecycle()`로 구독
- 리셋 확인 `AlertDialog` 포함

#### ProgressRingSection

원형 진행률 링을 `Canvas`로 직접 그립니다.

```kotlin
@Composable
fun ProgressRingSection(progress: Float, totalDistance: Double, goalReached: Boolean)
```

```
- drawArc()로 배경 링 (회색) + 진행 링 그리기
- strokeWidth: 20dp, StrokeCap.Round
- 진행 색상: #007AFF (파란색), 달성 시 #34C759 (초록색)
- animateFloatAsState로 800ms 애니메이션
- 달성 시: 🎉 이모지 + "목표 달성!" 텍스트
```

#### StatCardsRow / StatCard

총 거리와 남은 거리를 보여주는 카드 컴포넌트입니다.

```kotlin
@Composable
fun StatCard(label: String, value: String, unit: String, color: Color)
```

- `Card` + `RoundedCornerShape(16.dp)`
- Material3 `surfaceVariant` 배경색

#### InputRow

거리 입력 및 추가 버튼을 포함한 행입니다.

```kotlin
@Composable
fun InputRow(inputText: String, onInputChange: (String) -> Unit, onAdd: () -> Unit)
```

- `OutlinedTextField` + `KeyboardType.Decimal`
- `ImeAction.Done`으로 키보드 완료 버튼에서도 추가 가능

#### EntryRow

각 걷기 기록 행으로, 스와이프 삭제를 지원합니다.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryRow(entry: WalkEntry, onDelete: () -> Unit)
```

- `SwipeToDismissBox`: 왼쪽 스와이프로 삭제
- 배경: 빨간색 + 삭제 아이콘
- 날짜 포맷: `SimpleDateFormat("yyyy.MM.dd (E) HH:mm", Locale.KOREAN)`

---

## 의존성 (libs.versions.toml)

| 라이브러리 | 버전 | 용도 |
|---|---|---|
| `androidx.core:core-ktx` | 1.13.1 | Kotlin 확장 함수 |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.4 | Lifecycle 지원 |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.4 | Compose ViewModel |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.4 | `collectAsStateWithLifecycle` |
| `androidx.activity:activity-compose` | 1.9.1 | `ComponentActivity` + Compose |
| `androidx.compose:compose-bom` | 2024.08.00 | Compose 버전 BOM |
| `androidx.compose.ui:ui` | BOM 관리 | Compose UI |
| `androidx.compose.material3:material3` | BOM 관리 | Material Design 3 |
| `com.google.code.gson:gson` | 2.11.0 | JSON 직렬화 |

---

## 빌드 및 실행

### Android Studio

1. Android Studio 실행
2. **Open** → `android/` 폴더 선택
3. Gradle Sync 완료 대기
4. AVD Manager에서 에뮬레이터 생성 (API 26 이상)
5. **Run** (Shift+F10) 또는 ▶ 클릭

### 커맨드라인

```bash
cd android

# 디버그 APK 빌드
./gradlew assembleDebug

# 연결된 기기/에뮬레이터에 설치 및 실행
./gradlew installDebug

# 릴리즈 APK 빌드
./gradlew assembleRelease

# 빌드 결과물 위치
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 실제 기기 배포

1. **개발자 옵션 활성화:** 설정 → 휴대전화 정보 → 빌드 번호 7번 탭
2. **USB 디버깅 활성화:** 설정 → 개발자 옵션 → USB 디버깅
3. USB 케이블로 기기 연결
4. "USB 디버깅을 허용하시겠습니까?" → **확인**
5. Android Studio에서 기기 선택 후 **Run**

---

## AndroidManifest.xml 주요 설정

| 설정 | 값 | 역할 |
|---|---|---|
| `android:screenOrientation` | `portrait` | 세로 방향 고정 |
| `android:windowSoftInputMode` | `adjustResize` | 키보드 표시 시 화면 조절 |
| `android:exported` | `true` | 런처에서 실행 가능 |
| `android:theme` | `@android:style/Theme.Material.Light.NoActionBar` | 기본 테마 (Compose가 덮어씀) |

---

## 트러블슈팅

**Gradle Sync 실패 시:**
```bash
cd android
./gradlew --stop          # 데몬 종료
./gradlew assembleDebug   # 재빌드
```

**`collectAsStateWithLifecycle` import 오류:**
- `lifecycle-runtime-compose` 의존성이 추가되어 있는지 확인
- `import androidx.lifecycle.compose.collectAsStateWithLifecycle` 명시적 import

**SwipeToDismissBox Experimental 경고:**
- `@OptIn(ExperimentalMaterial3Api::class)` 어노테이션 필요
- 향후 Material3 안정 버전에서 정식 API로 전환 예정

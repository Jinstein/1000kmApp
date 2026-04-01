package com.jinstein.thousandkm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import java.text.SimpleDateFormat
import java.util.*

val CHALLENGE_COLORS = listOf(
    Color(0xFF007AFF),
    Color(0xFF34C759),
    Color(0xFFFF9500),
    Color(0xFFAF52DE),
    Color(0xFFFF3B30),
    Color(0xFF00C7BE),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChallengeApp()
                }
            }
        }
    }
}

@Composable
fun ChallengeApp(vm: ChallengeViewModel = viewModel()) {
    var selectedChallengeId by remember { mutableStateOf<String?>(null) }
    val challenges by vm.challenges.collectAsStateWithLifecycle()

    val selected = selectedChallengeId
    if (selected != null && challenges.any { it.id == selected }) {
        BackHandler { selectedChallengeId = null }
        ChallengeDetailScreen(
            challengeId = selected,
            vm = vm,
            onBack = { selectedChallengeId = null }
        )
    } else {
        if (selected != null) selectedChallengeId = null
        ChallengeListScreen(
            challenges = challenges,
            vm = vm,
            onChallengeClick = { selectedChallengeId = it }
        )
    }
}

// ─── List Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeListScreen(
    challenges: List<Challenge>,
    vm: ChallengeViewModel,
    onChallengeClick: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("챌린지", fontWeight = FontWeight.Bold) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "챌린지 추가")
            }
        },
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        if (challenges.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏆", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "아직 챌린지가 없어요.\n+ 버튼을 눌러 추가해보세요!",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challenges, key = { it.id }) { challenge ->
                    ChallengeCard(
                        challenge = challenge,
                        color = CHALLENGE_COLORS[challenges.indexOf(challenge) % CHALLENGE_COLORS.size],
                        onClick = { onChallengeClick(challenge.id) },
                        onDelete = { vm.deleteChallenge(challenge.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddChallengeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, unit, emoji, photoUri, finalGoal, dailyGoal, monthlyGoal, yearlyGoal ->
                vm.addChallenge(name, unit, emoji, photoUri, finalGoal, dailyGoal, monthlyGoal, yearlyGoal)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeCard(
    challenge: Challenge,
    color: Color,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val total = challenge.totalValue()
    val today = challenge.todayTotal()
    val month = challenge.thisMonthTotal()

    // Pick primary goal for progress bar
    val (progress, progressLabel) = when {
        challenge.dailyGoal != null -> Pair((today / challenge.dailyGoal).coerceIn(0.0, 1.0).toFloat(), "오늘")
        challenge.monthlyGoal != null -> Pair((month / challenge.monthlyGoal).coerceIn(0.0, 1.0).toFloat(), "이번 달")
        challenge.finalGoal != null -> Pair((total / challenge.finalGoal).coerceIn(0.0, 1.0).toFloat(), "최종")
        else -> Pair(0f, "")
    }
    val primaryGoal = challenge.dailyGoal ?: challenge.monthlyGoal ?: challenge.finalGoal
    val primaryValue = when {
        challenge.dailyGoal != null -> today
        challenge.monthlyGoal != null -> month
        else -> total
    }
    val goalReached = primaryGoal != null && primaryValue >= primaryGoal
    val barColor = if (goalReached) Color(0xFF34C759) else color

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.White, modifier = Modifier.padding(end = 20.dp))
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (challenge.photoUri != null) {
                    AsyncImage(
                        model = challenge.photoUri,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(challenge.emoji, fontSize = 36.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(challenge.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        if (primaryGoal != null) {
                            if (goalReached) {
                                Text("🎉 달성!", fontSize = 13.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                            } else {
                                Text(
                                    "${String.format("%.1f", progress * 100)}%",
                                    fontSize = 14.sp, color = barColor, fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Text(
                                "${String.format("%.1f", total)} ${challenge.unit}",
                                fontSize = 13.sp, color = color, fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (primaryGoal != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = barColor,
                            trackColor = Color(0xFFE5E5EA)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${String.format("%.1f", primaryValue)} / ${String.format("%.0f", primaryGoal)} ${challenge.unit}  ·  $progressLabel",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "기록 ${challenge.entries.size}개",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─── Add Challenge Dialog ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChallengeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String?, Double?, Double?, Double?, Double?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("km") }
    var customUnit by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🔥") }
    var selectedPhotoUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var finalGoalText by remember { mutableStateOf("") }
    var dailyGoalText by remember { mutableStateOf("") }
    var monthlyGoalText by remember { mutableStateOf("") }
    var yearlyGoalText by remember { mutableStateOf("") }

    val presetUnits = listOf("km", "회", "일", "분", "권")
    val presetEmojis = listOf("🔥", "🏃", "💪", "📚", "🚴", "✍️", "🎯", "⭐", "🏋️", "🚶")

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            selectedPhotoUri = uri
        }
    }

    fun String.toGoal() = replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 챌린지", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("챌린지 이름") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                // Unit
                Text("단위", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    presetUnits.forEach { unit ->
                        FilterChip(
                            selected = selectedUnit == unit && customUnit.isEmpty(),
                            onClick = { selectedUnit = unit; customUnit = "" },
                            label = { Text(unit) }
                        )
                    }
                }
                OutlinedTextField(
                    value = customUnit,
                    onValueChange = { customUnit = it; if (it.isNotEmpty()) selectedUnit = it },
                    label = { Text("직접 입력") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )

                // Emoji / Photo
                Text("이모지 또는 사진", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (selectedPhotoUri != null) {
                    // Photo selected — show preview + remove button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = selectedPhotoUri,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        OutlinedButton(onClick = { selectedPhotoUri = null }) {
                            Text("사진 제거")
                        }
                    }
                } else {
                    // Emoji grid
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetEmojis.take(5).forEach { emoji ->
                            Box(
                                modifier = Modifier.size(44.dp)
                                    .background(
                                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 22.sp) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        presetEmojis.drop(5).forEach { emoji ->
                            Box(
                                modifier = Modifier.size(44.dp)
                                    .background(
                                        if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedEmoji = emoji },
                                contentAlignment = Alignment.Center
                            ) { Text(emoji, fontSize = 22.sp) }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (selectedPhotoUri != null) "📷 사진 변경" else "📷 갤러리에서 사진 선택")
                }

                HorizontalDivider()

                // Goals
                Text("목표 설정 (선택사항)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                GoalField("최종 목표", finalGoalText, selectedUnit) { finalGoalText = it }
                GoalField("하루 목표", dailyGoalText, selectedUnit) { dailyGoalText = it }
                GoalField("월 목표", monthlyGoalText, selectedUnit) { monthlyGoalText = it }
                GoalField("년 목표", yearlyGoalText, selectedUnit) { yearlyGoalText = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) return@TextButton
                val unit = if (customUnit.isNotEmpty()) customUnit else selectedUnit
                onConfirm(
                    name.trim(), unit, selectedEmoji,
                    selectedPhotoUri?.toString(),
                    finalGoalText.toGoal(),
                    dailyGoalText.toGoal(),
                    monthlyGoalText.toGoal(),
                    yearlyGoalText.toGoal()
                )
            }) { Text("추가") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } }
    )
}

@Composable
fun GoalField(label: String, value: String, unit: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label ($unit)") },
        placeholder = { Text("미설정") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}

// ─── Detail Screen ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChallengeDetailScreen(
    challengeId: String,
    vm: ChallengeViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val challenges by vm.challenges.collectAsStateWithLifecycle()
    val challenge = challenges.find { it.id == challengeId } ?: return

    val inputText by vm.inputText.collectAsStateWithLifecycle()
    val selectedDateMillis by vm.selectedDateMillis.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val total = challenge.totalValue()
    val colorIndex = challenges.indexOf(challenge)
    val challengeColor = CHALLENGE_COLORS[colorIndex.coerceAtLeast(0) % CHALLENGE_COLORS.size]

    val finalProgress = challenge.finalGoal?.let { (total / it).coerceIn(0.0, 1.0).toFloat() } ?: 0f
    val goalReached = challenge.finalGoal != null && total >= challenge.finalGoal

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (challenge.photoUri != null) {
                            AsyncImage(
                                model = challenge.photoUri,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Text("${challenge.emoji} ")
                        }
                        Text(challenge.name, fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("${challenge.emoji} ${challenge.name} 진행 중!")
                            appendLine("📍 총 달성: ${String.format("%.1f", total)} ${challenge.unit}")
                            challenge.finalGoal?.let {
                                appendLine("📊 최종 달성률: ${String.format("%.1f", (finalProgress * 100).coerceIn(0f, 100f))}%")
                            }
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "공유하기"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "공유")
                    }
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("리셋", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        bottomBar = { BannerAdView() }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Progress ring (only if finalGoal set)
            if (challenge.finalGoal != null) {
                item {
                    ProgressRingSection(
                        progress = finalProgress,
                        totalValue = total,
                        unit = challenge.unit,
                        goalReached = goalReached,
                        ringColor = challengeColor
                    )
                }
                item {
                    StatCardsRow(
                        totalValue = total,
                        remainingValue = maxOf(0.0, challenge.finalGoal - total),
                        unit = challenge.unit,
                        color = challengeColor
                    )
                }
            } else {
                // No final goal — show simple total header
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    ) {
                        Text(
                            String.format("%.1f", total),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Bold,
                            color = challengeColor
                        )
                        Text(challenge.unit, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("누적 총합", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Period goals section
            if (challenge.dailyGoal != null || challenge.monthlyGoal != null || challenge.yearlyGoal != null) {
                item {
                    PeriodProgressSection(challenge = challenge, color = challengeColor)
                }
            }

            // Input
            item {
                InputRow(
                    inputText = inputText,
                    selectedDateMillis = selectedDateMillis,
                    unit = challenge.unit,
                    onInputChange = { vm.updateInput(it) },
                    onAdd = { vm.addEntry(challenge.id); focusManager.clearFocus() },
                    onDateClick = { showDatePicker = true }
                )
            }

            // Entries header / empty state
            item {
                if (challenge.entries.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "아직 기록이 없어요.\n오늘의 기록을 추가해보세요! ${challenge.emoji}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        "기록",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            items(challenge.entries, key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    unit = challenge.unit,
                    color = challengeColor,
                    onDelete = { vm.deleteEntry(challenge.id, entry.id) }
                )
            }
        }
    }

    if (showDatePicker) {
        val utcMidnight = remember(selectedDateMillis) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = utcMidnight)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { vm.updateSelectedDate(it) }
                    showDatePicker = false
                }) { Text("확인") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("취소") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("챌린지 리셋") },
            text = { Text("${challenge.name}의 모든 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { vm.resetChallenge(challenge.id); showResetDialog = false }) {
                    Text("리셋", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("취소") } }
        )
    }
}

// ─── Period Progress Section ──────────────────────────────────────────────────

@Composable
fun PeriodProgressSection(challenge: Challenge, color: Color) {
    val todayVal = challenge.todayTotal()
    val monthVal = challenge.thisMonthTotal()
    val yearVal = challenge.thisYearTotal()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("주기 목표", fontWeight = FontWeight.SemiBold, fontSize = 17.sp,
            modifier = Modifier.padding(vertical = 4.dp))

        if (challenge.dailyGoal != null) {
            PeriodGoalCard("오늘", todayVal, challenge.dailyGoal, challenge.unit, color)
        }
        if (challenge.monthlyGoal != null) {
            PeriodGoalCard("이번 달", monthVal, challenge.monthlyGoal, challenge.unit, color)
        }
        if (challenge.yearlyGoal != null) {
            PeriodGoalCard("올해", yearVal, challenge.yearlyGoal, challenge.unit, color)
        }
    }
}

@Composable
fun PeriodGoalCard(label: String, current: Double, goal: Double, unit: String, color: Color) {
    val progress = (current / goal).coerceIn(0.0, 1.0).toFloat()
    val reached = current >= goal
    val barColor = if (reached) Color(0xFF34C759) else color
    val animProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(600), label = "period")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (reached) {
                    Text("🎉 달성!", fontSize = 13.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                } else {
                    Text(
                        "${String.format("%.1f", current)} / ${String.format("%.0f", goal)} $unit",
                        fontSize = 13.sp, color = barColor, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = barColor,
                trackColor = Color(0xFFE5E5EA)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${String.format("%.1f", (progress * 100).coerceIn(0f, 100f))}%",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Shared Composables ───────────────────────────────────────────────────────

@Composable
fun ProgressRingSection(
    progress: Float,
    totalValue: Double,
    unit: String,
    goalReached: Boolean,
    ringColor: Color
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(800), label = "progress")
    val color = if (goalReached) Color(0xFF34C759) else ringColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(Color(0xFFE5E5EA), -90f, 360f, false, topLeft, Size(diameter, diameter), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            if (animatedProgress > 0f) {
                drawArc(color, -90f, 360f * animatedProgress, false, topLeft, Size(diameter, diameter), style = Stroke(strokeWidth, cap = StrokeCap.Round))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (goalReached) {
                Text("🎉", fontSize = 36.sp)
                Text("목표 달성!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            } else {
                Text(String.format("%.1f", totalValue), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format("%.1f%%", (progress * 100).coerceIn(0f, 100f)), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatCardsRow(totalValue: Double, remainingValue: Double, unit: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(Modifier.weight(1f), "달성", String.format("%.1f", totalValue), unit, color)
        StatCard(Modifier.weight(1f), "남은", String.format("%.1f", remainingValue), unit, Color(0xFFFF9500))
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, label: String, value: String, unit: String, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = color)
            Text(unit, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun InputRow(
    inputText: String,
    selectedDateMillis: Long,
    unit: String,
    onInputChange: (String) -> Unit,
    onAdd: () -> Unit,
    onDateClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd (E)", Locale.KOREAN) }
    val isToday = remember(selectedDateMillis) {
        val sel = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val today = Calendar.getInstance()
        sel.get(Calendar.YEAR) == today.get(Calendar.YEAR) && sel.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
    val dateLabel = if (isToday) "오늘" else dateFormat.format(Date(selectedDateMillis))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputText, onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("기록 ($unit)") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                shape = RoundedCornerShape(12.dp)
            )
            Button(onClick = onAdd, modifier = Modifier.height(56.dp), shape = RoundedCornerShape(12.dp)) {
                Text("추가", fontSize = 16.sp)
            }
        }
        TextButton(onClick = onDateClick) {
            Text("날짜: $dateLabel", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun BannerAdView() {
    AndroidView(
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = "ca-app-pub-6455118906319702/6435670198"
                loadAd(AdRequest.Builder().build())
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryRow(entry: ChallengeEntry, unit: String, color: Color, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { v -> if (v == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.White, modifier = Modifier.padding(end = 20.dp))
            }
        }
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    SimpleDateFormat("yyyy.MM.dd (E) HH:mm", Locale.KOREAN).format(Date(entry.dateMillis)),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.1f %s", entry.value, unit),
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color
                )
            }
        }
    }
}

package com.jinstein.thousandkm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
            TopAppBar(
                title = { Text("챌린지", fontWeight = FontWeight.Bold) }
            )
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(challenges, key = { it.id }) { challenge ->
                    val colorIndex = challenges.indexOf(challenge)
                    ChallengeCard(
                        challenge = challenge,
                        color = CHALLENGE_COLORS[colorIndex % CHALLENGE_COLORS.size],
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
            onConfirm = { name, goal, unit, emoji ->
                vm.addChallenge(name, goal, unit, emoji)
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
    val total = challenge.entries.sumOf { it.value }
    val progress = (total / challenge.goal).coerceIn(0.0, 1.0).toFloat()
    val goalReached = total >= challenge.goal

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete(); true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "삭제",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 20.dp)
                )
            }
        }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(challenge.emoji, fontSize = 36.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(challenge.name, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        if (goalReached) {
                            Text("🎉 달성!", fontSize = 13.sp, color = Color(0xFF34C759), fontWeight = FontWeight.Bold)
                        } else {
                            Text(
                                "${String.format("%.1f", progress * 100)}%",
                                fontSize = 14.sp,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = if (goalReached) Color(0xFF34C759) else color,
                        trackColor = Color(0xFFE5E5EA)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "${String.format("%.1f", total)} / ${String.format("%.0f", challenge.goal)} ${challenge.unit}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChallengeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var goalText by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf("km") }
    var customUnit by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("🔥") }

    val presetUnits = listOf("km", "회", "일", "분", "권")
    val presetEmojis = listOf("🔥", "🏃", "💪", "📚", "🚴", "✍️", "🎯", "⭐", "🏋️", "🚶")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 챌린지", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("챌린지 이름") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = goalText,
                    onValueChange = { goalText = it },
                    label = { Text("목표") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("단위", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    label = { Text("직접 입력") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Text("이모지", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    presetEmojis.take(5).forEach { emoji ->
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
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
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedEmoji = emoji },
                            contentAlignment = Alignment.Center
                        ) { Text(emoji, fontSize = 22.sp) }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val goal = goalText.replace(",", ".").toDoubleOrNull() ?: return@TextButton
                if (name.isBlank() || goal <= 0) return@TextButton
                val unit = if (customUnit.isNotEmpty()) customUnit else selectedUnit
                onConfirm(name.trim(), goal, unit, selectedEmoji)
            }) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        }
    )
}

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

    val totalValue = challenge.entries.sumOf { it.value }
    val remainingValue = maxOf(0.0, challenge.goal - totalValue)
    val progress = (totalValue / challenge.goal).coerceIn(0.0, 1.0).toFloat()
    val goalReached = totalValue >= challenge.goal

    val colorIndex = challenges.indexOf(challenge)
    val challengeColor = CHALLENGE_COLORS[colorIndex.coerceAtLeast(0) % CHALLENGE_COLORS.size]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${challenge.emoji} ${challenge.name}", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("${challenge.emoji} ${challenge.name} 진행 중!")
                            appendLine("📍 달성: ${String.format("%.1f", totalValue)} ${challenge.unit}")
                            appendLine("📊 달성률: ${String.format("%.1f", (progress * 100).coerceIn(0f, 100f))}%")
                            appendLine("🎯 남은: ${String.format("%.1f", remainingValue)} ${challenge.unit}")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                ProgressRingSection(
                    progress = progress,
                    totalValue = totalValue,
                    unit = challenge.unit,
                    goalReached = goalReached,
                    ringColor = challengeColor
                )
            }
            item {
                StatCardsRow(
                    totalValue = totalValue,
                    remainingValue = remainingValue,
                    unit = challenge.unit,
                    color = challengeColor
                )
            }
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
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("취소") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("챌린지 리셋") },
            text = { Text("${challenge.name}의 모든 기록을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = {
                    vm.resetChallenge(challenge.id)
                    showResetDialog = false
                }) { Text("리셋", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("취소") }
            }
        )
    }
}

@Composable
fun ProgressRingSection(
    progress: Float,
    totalValue: Double,
    unit: String,
    goalReached: Boolean,
    ringColor: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )
    val color = if (goalReached) Color(0xFF34C759) else ringColor

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(top = 24.dp, bottom = 16.dp).size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            drawArc(
                color = Color(0xFFE5E5EA), startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = topLeft, size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            if (animatedProgress > 0f) {
                drawArc(
                    color = color, startAngle = -90f, sweepAngle = 360f * animatedProgress,
                    useCenter = false, topLeft = topLeft, size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (goalReached) {
                Text("🎉", fontSize = 36.sp)
                Text("목표 달성!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            } else {
                Text(String.format("%.1f", totalValue), fontSize = 36.sp, fontWeight = FontWeight.Bold, color = color)
                Text(unit, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    String.format("%.1f%%", (progress * 100).coerceIn(0f, 100f)),
                    fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        sel.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
        sel.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }
    val dateLabel = if (isToday) "오늘" else dateFormat.format(Date(selectedDateMillis))

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("기록 ($unit)") },
                singleLine = true,
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
                adUnitId = "ca-app-pub-3940256099942544/6300978111"
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
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDelete(); true } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
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
                val dateFormat = SimpleDateFormat("yyyy.MM.dd (E) HH:mm", Locale.KOREAN)
                Text(dateFormat.format(Date(entry.dateMillis)), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format("%.1f %s", entry.value, unit), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = color)
            }
        }
    }
}

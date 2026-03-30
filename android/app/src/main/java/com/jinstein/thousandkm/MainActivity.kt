package com.jinstein.thousandkm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

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
                    WalkChallengeApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkChallengeApp(vm: WalkViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val entries by vm.entries.collectAsStateWithLifecycle()
    val inputText by vm.inputKmText.collectAsStateWithLifecycle()
    val selectedDateMillis by vm.selectedDateMillis.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val totalDistance = entries.sumOf { it.distance }
    val remainingDistance = maxOf(0.0, 1000.0 - totalDistance)
    val progress = (totalDistance / 1000.0).coerceIn(0.0, 1.0).toFloat()
    val goalReached = totalDistance >= 1000.0

    Scaffold(
        bottomBar = {
            BannerAdView()
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "1000km 챌린지",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = {
                        val shareText = buildString {
                            appendLine("🚶 1000km 챌린지 진행 중!")
                            appendLine("📍 총 거리: ${String.format("%.2f", totalDistance)} km")
                            appendLine("📊 달성률: ${String.format("%.1f", (progress * 100).coerceIn(0f, 100f))}%")
                            appendLine("🎯 남은 거리: ${String.format("%.2f", remainingDistance)} km")
                        }
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "공유하기"))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "공유"
                        )
                    }
                    TextButton(onClick = { showResetDialog = true }) {
                        Text("리셋", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
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
                    totalDistance = totalDistance,
                    goalReached = goalReached
                )
            }
            item {
                StatCardsRow(
                    totalDistance = totalDistance,
                    remainingDistance = remainingDistance
                )
            }
            item {
                InputRow(
                    inputText = inputText,
                    selectedDateMillis = selectedDateMillis,
                    onInputChange = { vm.updateInput(it) },
                    onAdd = {
                        vm.addEntry()
                        focusManager.clearFocus()
                    },
                    onDateClick = { showDatePicker = true }
                )
            }
            item {
                if (entries.isEmpty()) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        "아직 기록이 없어요.\n오늘 걸은 거리를 추가해보세요! 🚶",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                } else {
                    Text(
                        "걷기 기록",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
            items(entries, key = { it.id }) { entry ->
                EntryRow(
                    entry = entry,
                    onDelete = { vm.deleteEntry(entry.id) }
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
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("챌린지 리셋") },
            text = { Text("현재 기록을 보관하고 새로운 챌린지를 시작할까요?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.resetChallenge()
                        showResetDialog = false
                    }
                ) {
                    Text("리셋", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("취소")
                }
            }
        )
    }
}

@Composable
fun ProgressRingSection(
    progress: Float,
    totalDistance: Double,
    goalReached: Boolean
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    val ringColor = if (goalReached) Color(0xFF34C759) else Color(0xFF007AFF)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(top = 24.dp, bottom = 16.dp)
            .size(220.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 20.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )

            // Background track
            drawArc(
                color = Color(0xFFE5E5EA),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(diameter, diameter),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(diameter, diameter),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (goalReached) {
                Text("🎉", fontSize = 36.sp)
                Text(
                    "목표 달성!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
            } else {
                Text(
                    String.format("%.1f", totalDistance),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ringColor
                )
                Text(
                    "km",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.1f%%", (progress * 100).coerceIn(0f, 100f)),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatCardsRow(totalDistance: Double, remainingDistance: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "총 거리",
            value = String.format("%.2f", totalDistance),
            unit = "km",
            color = Color(0xFF007AFF)
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "남은 거리",
            value = String.format("%.2f", remainingDistance),
            unit = "km",
            color = Color(0xFFFF9500)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    unit: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                unit,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun InputRow(
    inputText: String,
    selectedDateMillis: Long,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("걸은 거리 (km)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onAdd() }),
                shape = RoundedCornerShape(12.dp)
            )
            Button(
                onClick = onAdd,
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("추가", fontSize = 16.sp)
            }
        }
        TextButton(onClick = onDateClick) {
            Text(
                "날짜: $dateLabel",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
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
fun EntryRow(entry: WalkEntry, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
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
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(12.dp)
                    ),
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dateFormat = SimpleDateFormat("yyyy.MM.dd (E) HH:mm", Locale.KOREAN)
                Text(
                    dateFormat.format(Date(entry.dateMillis)),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    String.format("%.2f km", entry.distance),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF007AFF)
                )
            }
        }
    }
}

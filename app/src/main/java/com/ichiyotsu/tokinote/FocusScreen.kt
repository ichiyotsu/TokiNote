package com.ichiyotsu.tokinote

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.ceil

private enum class FocusPhase(val title: String, val seconds: Int) {
    FOCUS("专注", 25 * 60),
    SHORT_BREAK("短休息", 5 * 60),
    LONG_BREAK("长休息", 15 * 60)
}

@Composable
fun FocusScreen(
    darkTheme: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("tokinote_focus", Activity.MODE_PRIVATE) }
    val todayKey = remember { "sessions_${LocalDate.now()}" }
    var phase by rememberSaveable { mutableStateOf(runCatching { FocusPhase.valueOf(prefs.getString("phase", FocusPhase.FOCUS.name) ?: FocusPhase.FOCUS.name) }.getOrDefault(FocusPhase.FOCUS)) }
    var totalSeconds by rememberSaveable { mutableIntStateOf(prefs.getInt("total_seconds", phase.seconds)) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(prefs.getInt("seconds_left", totalSeconds).coerceAtLeast(0)) }
    var deadlineMillis by rememberSaveable { mutableLongStateOf(prefs.getLong("deadline", 0L)) }
    var completedToday by rememberSaveable { mutableIntStateOf(prefs.getInt(todayKey, 0)) }
    val isRunning = deadlineMillis > 0L
    val accent = when (phase) {
        FocusPhase.FOCUS -> Color(0xFFE56955)
        FocusPhase.SHORT_BREAK -> Color(0xFF4F9D7A)
        FocusPhase.LONG_BREAK -> Color(0xFF5685B8)
    }
    val paleAccent = when (phase) {
        FocusPhase.FOCUS -> Color(0xFFF9E5DF)
        FocusPhase.SHORT_BREAK -> Color(0xFFE1F1E9)
        FocusPhase.LONG_BREAK -> Color(0xFFE4EDF8)
    }
    val displayLeft = if (isRunning) ceil((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0).toInt() else secondsLeft
    val progress by animateFloatAsState(
        targetValue = if (totalSeconds == 0) 0f else (displayLeft.toFloat() / totalSeconds).coerceIn(0f, 1f),
        animationSpec = tween(350),
        label = "focus-progress"
    )

    BackHandler(onBack = onBack)

    LaunchedEffect(deadlineMillis) {
        while (deadlineMillis > 0L) {
            val left = ceil((deadlineMillis - System.currentTimeMillis()) / 1000.0).toInt()
            if (left <= 0) {
                secondsLeft = 0
                deadlineMillis = 0L
                if (phase == FocusPhase.FOCUS) {
                    completedToday += 1
                    val nextPhase = if (completedToday % 4 == 0) FocusPhase.LONG_BREAK else FocusPhase.SHORT_BREAK
                    phase = nextPhase
                    totalSeconds = nextPhase.seconds
                    secondsLeft = nextPhase.seconds
                } else {
                    phase = FocusPhase.FOCUS
                    totalSeconds = FocusPhase.FOCUS.seconds
                    secondsLeft = FocusPhase.FOCUS.seconds
                }
                break
            }
            secondsLeft = left
            delay(250L)
        }
    }

    LaunchedEffect(phase, totalSeconds, secondsLeft, deadlineMillis, completedToday, todayKey) {
        prefs.edit {
            putString("phase", phase.name)
            putInt("total_seconds", totalSeconds)
            putInt("seconds_left", secondsLeft)
            putLong("deadline", deadlineMillis)
            putInt(todayKey, completedToday)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (darkTheme) listOf(Color(0xFF201C22), Color(0xFF17151A))
                    else listOf(Color(0xFFFFF8F4), Color(0xFFF6F3EF))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回笔记主页")
                }
                Column(Modifier.weight(1f)) {
                    Text("TOKINOTE · 专注", style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.1.sp, fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("把注意力留给此刻。", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                }
                Surface(shape = CircleShape, color = paleAccent) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = accent, modifier = Modifier.padding(11.dp).size(21.dp))
                }
            }

            Column(
                Modifier.fillMaxSize().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("一段时间，只做一件事", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    FocusPhase.entries.forEach { option ->
                        FilterChip(
                            selected = phase == option,
                            onClick = {
                                if (!isRunning) {
                                    phase = option
                                    totalSeconds = option.seconds
                                    secondsLeft = option.seconds
                                }
                            },
                            label = { Text(option.title) },
                            enabled = !isRunning,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(286.dp)) {
                    Canvas(Modifier.fillMaxSize().padding(10.dp)) {
                        val stroke = 11.dp.toPx()
                        drawCircle(
                            color = if (darkTheme) Color(0xFF373139) else Color(0xFFEDE6E2),
                            style = Stroke(width = stroke)
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(phase.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = accent)
                        Text(
                            text = "%02d:%02d".format(displayLeft / 60, displayLeft % 60),
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.Medium, letterSpacing = (-2).sp, fontFeatureSettings = "tnum"),
                            textAlign = TextAlign.Center
                        )
                        Text(if (isRunning) "正在进行" else "准备好时，轻触开始", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            if (isRunning) {
                                secondsLeft = ceil((deadlineMillis - System.currentTimeMillis()).coerceAtLeast(0L) / 1000.0).toInt()
                                deadlineMillis = 0L
                            } else {
                                if (secondsLeft <= 0) secondsLeft = totalSeconds
                                deadlineMillis = System.currentTimeMillis() + secondsLeft * 1000L
                            }
                        },
                        modifier = Modifier.width(176.dp).height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White)
                    ) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isRunning) "暂停" else "开始专注", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                    }
                    FilledTonalButton(
                        onClick = { secondsLeft = totalSeconds; deadlineMillis = 0L },
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "重置计时")
                    }
                    FilledTonalButton(
                        onClick = {
                            val continueRunning = isRunning
                            deadlineMillis = 0L
                            val next = if (phase == FocusPhase.FOCUS) {
                                if ((completedToday + 1) % 4 == 0) FocusPhase.LONG_BREAK else FocusPhase.SHORT_BREAK
                            } else FocusPhase.FOCUS
                            if (phase == FocusPhase.FOCUS) completedToday += 1
                            phase = next
                            totalSeconds = next.seconds
                            secondsLeft = next.seconds
                            if (continueRunning) deadlineMillis = System.currentTimeMillis() + next.seconds * 1000L
                        },
                        modifier = Modifier.size(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "跳过当前阶段")
                    }
                }
                Spacer(Modifier.height(22.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .45f), RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = if (darkTheme) .62f else .86f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(13.dp), color = paleAccent) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = accent, modifier = Modifier.padding(10.dp).size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("今天完成", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$completedToday 个专注时段", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold))
                        }
                        Text("每 4 轮，休息久一点", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("专注时段结束后会自动切换到休息。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

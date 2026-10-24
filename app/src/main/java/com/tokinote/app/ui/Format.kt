package com.tokinote.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val noteDateFormatter = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

fun formatNoteDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(noteDateFormatter)

/** 把秒数格式化成 mm:ss。 */
fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
}

/** 「25 分钟」「1 小时 5 分钟」这样的可读时长。 */
fun formatMinutes(minutes: Int): String =
    if (minutes < 60) {
        "$minutes 分钟"
    } else {
        val hours = minutes / 60
        val rest = minutes % 60
        if (rest == 0) "$hours 小时" else "$hours 小时 $rest 分钟"
    }

fun isToday(epochMillis: Long): Boolean =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .toLocalDate() == LocalDate.now()

/** 正向计时：超过一小时显示 h:mm:ss。 */
fun formatElapsed(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.US, hours, (safe % 3600) / 60, safe % 60)
    } else {
        "%02d:%02d".format(Locale.US, safe / 60, safe % 60)
    }
}
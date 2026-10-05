package com.zhihuminus.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant as KotlinInstant

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatDateTime(seconds: Long): String =
    Instant
        .ofEpochSecond(seconds)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

fun formatCount(value: Long): String {
    fun scaled(divisor: Double, unit: String): String {
        val number = (value / divisor * 10).toLong() / 10.0
        val text = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
        return "$text $unit"
    }
    return when {
        value >= 100_000_000 -> scaled(100_000_000.0, "亿")
        value >= 10_000 -> scaled(10_000.0, "万")
        else -> value.toString()
    }
}

fun formatDailyDate(dateString: String): String {
    if (dateString.length != 8 || dateString.any { !it.isDigit() }) {
        return dateString
    }
    return "${dateString.substring(0, 4)}年${dateString.substring(4, 6)}月${dateString.substring(6, 8)}日"
}

@OptIn(ExperimentalTime::class)
fun formatRelativeTime(
    epochSeconds: Long,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val diff = nowEpochSeconds - epochSeconds

    return when {
        diff < 60 -> "刚刚"
        diff < 3_600 -> "${diff / 60}分钟前"
        diff < 86_400 -> "${diff / 3_600}小时前"
        diff < 604_800 -> "${diff / 86_400}天前"
        else -> {
            val dateTime = KotlinInstant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(timeZone)
            "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
        }
    }
}

fun Int.twoDigitString(): String = toString().padStart(2, '0')

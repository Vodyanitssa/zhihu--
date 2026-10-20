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

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
private const val SECONDS_PER_WEEK = 7L * SECONDS_PER_DAY

fun formatDateTime(seconds: Long): String {
    if (seconds <= 0) return ""
    return Instant
        .ofEpochSecond(seconds)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)
}

fun formatCount(value: Long): String {
    val normalized = value.coerceAtLeast(0)

    fun scaled(divisor: Double, unit: String): String {
        val number = (normalized / divisor * 10).toLong() / 10.0
        val text = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
        return "$text $unit"
    }
    return when {
        normalized >= 100_000_000 -> scaled(100_000_000.0, "亿")
        normalized >= 10_000 -> scaled(10_000.0, "万")
        else -> normalized.toString()
    }
}

fun formatCount(value: Int): String = formatCount(value.toLong())

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
    if (epochSeconds <= 0) return ""

    val diff = (nowEpochSeconds - epochSeconds).coerceAtLeast(0)
    return when {
        diff < SECONDS_PER_MINUTE -> "刚刚"
        diff < SECONDS_PER_HOUR -> "${diff / SECONDS_PER_MINUTE}分钟前"
        diff < SECONDS_PER_DAY -> "${diff / SECONDS_PER_HOUR}小时前"
        diff < SECONDS_PER_WEEK -> "${diff / SECONDS_PER_DAY}天前"
        else -> {
            val dateTime = KotlinInstant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(timeZone)
            val nowDateTime = KotlinInstant
                .fromEpochSeconds(nowEpochSeconds)
                .toLocalDateTime(timeZone)
            if (dateTime.year == nowDateTime.year) {
                "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
            } else {
                "${dateTime.year}-${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()}"
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
fun formatChatTime(
    epochSeconds: Long,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (epochSeconds <= 0) return ""

    val dateTime = KotlinInstant
        .fromEpochSeconds(epochSeconds)
        .toLocalDateTime(timeZone)
    val now = KotlinInstant
        .fromEpochSeconds(nowEpochSeconds)
        .toLocalDateTime(timeZone)
    val dateDays = dateTime.date.toEpochDays()
    val nowDays = now.date.toEpochDays()
    val time = "${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
    return when {
        dateDays == nowDays -> time
        dateDays == nowDays - 1 -> "昨天 $time"
        dateTime.year == now.year ->
            "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} $time"
        else -> "${dateTime.year}-${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} $time"
    }
}

@OptIn(ExperimentalTime::class)
fun formatShortDate(
    epochSeconds: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (epochSeconds <= 0) return ""

    val dateTime = KotlinInstant
        .fromEpochSeconds(epochSeconds)
        .toLocalDateTime(timeZone)
    return "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()}"
}

fun Int.twoDigitString(): String = toString().padStart(2, '0')

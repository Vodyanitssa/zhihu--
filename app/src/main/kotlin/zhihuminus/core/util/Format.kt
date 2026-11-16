package com.zhihuminus.core.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.net.URLDecoder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Clock
import kotlin.time.Instant as KotlinInstant

private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
private const val SECONDS_PER_WEEK = 7L * SECONDS_PER_DAY

private const val ZHIHU_LINK_HOST = "link.zhihu.com"
private const val MAX_ZHIHU_REDIRECT_DEPTH = 3

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

fun formatRelativeTime(
    epochSeconds: Long,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    if (epochSeconds <= 0) return ""

    val diff = (nowEpochSeconds - epochSeconds).coerceAtLeast(0)
    return when {
        diff < SECONDS_PER_MINUTE -> "刚刚"
        diff < SECONDS_PER_HOUR -> " ${diff / SECONDS_PER_MINUTE} 分钟前"
        diff < SECONDS_PER_DAY -> " ${diff / SECONDS_PER_HOUR} 小时前"
        diff < SECONDS_PER_WEEK -> " ${diff / SECONDS_PER_DAY} 天前"
        else -> {
            val dateTime = KotlinInstant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(timeZone)
            val nowDateTime = KotlinInstant
                .fromEpochSeconds(nowEpochSeconds)
                .toLocalDateTime(timeZone)
            if (dateTime.year == nowDateTime.year) {
                " ${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
            } else {
                " ${dateTime.year}-${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()}"
            }
        }
    }
}

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

/**
 * 解开知乎外链跳转地址：`https://link.zhihu.com/?target=<真实链接>` 中的 `target` 参数即目标链接，
 * 通常经过 URL 编码（个别情况下会二次编码）。这里提取并解码后返回真实链接；
 * 非 `link.zhihu.com` 地址或无 `target` 参数时原样返回。
 */
fun unwrapZhihuRedirectUrl(url: String): String {
    var current = url.trim()
    repeat(MAX_ZHIHU_REDIRECT_DEPTH) {
        current = extractZhihuLinkTarget(current) ?: return current
    }
    return current
}

private fun extractZhihuLinkTarget(url: String): String? {
    val schemeEnd = url.indexOf("://")
    if (schemeEnd < 0) return null
    val authority = url
        .substring(schemeEnd + 3)
        .substringBefore('/')
        .substringBefore('?')
        .substringBefore('#')
        .substringBefore(':')
    if (!authority.equals(ZHIHU_LINK_HOST, ignoreCase = true)) return null
    val query = url.substringAfter('?', "").substringBefore('#')
    if (query.isEmpty()) return null
    val target = query
        .split('&')
        .firstOrNull { it.startsWith("target=") }
        ?.substringAfter("target=")
        ?.takeIf { it.isNotEmpty() }
        ?: return null
    return decodeZhihuTarget(target)
}

private fun decodeZhihuTarget(value: String): String {
    val decoded = percentDecode(value)
    // 二次编码时首轮解码结果仍是形如 https%3A%2F%2F... 的字符串，需要再解一次
    return if (!decoded.contains("://") && decoded.contains("%3A", ignoreCase = true)) {
        percentDecode(decoded)
    } else {
        decoded
    }
}

private fun percentDecode(value: String): String {
    if ('%' !in value) return value
    // URLDecoder 会把 "+" 解码成空格，先转义为 %2B 以保留 URL 中真实的加号
    return runCatching {
        URLDecoder.decode(value.replace("+", "%2B"), "UTF-8")
    }.getOrDefault(value)
}

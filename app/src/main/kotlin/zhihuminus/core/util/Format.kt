package com.zhihuminus.core.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

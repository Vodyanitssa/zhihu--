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

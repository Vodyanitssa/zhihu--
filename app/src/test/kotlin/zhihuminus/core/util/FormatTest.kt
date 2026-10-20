package com.zhihuminus.core.util

import kotlinx.datetime.TimeZone
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class FormatTest {
    private val now = Instant.parse("2024-06-01T12:00:00Z").epochSecond

    @Test
    fun formatCountUsesChineseUnits() {
        assertEquals("9999", formatCount(9_999))
        assertEquals("1 万", formatCount(10_000))
        assertEquals("1.2 万", formatCount(12_345))
        assertEquals("1 亿", formatCount(100_000_000))
        assertEquals("0", formatCount(-1))
        assertEquals("1.2 万", formatCount(12_345L))
    }

    @Test
    fun formatDateTimeHandlesMissingTimestamp() {
        assertEquals("", formatDateTime(0))
        assertEquals("", formatDateTime(-1))
    }

    @Test
    fun formatRelativeTimeCoversRecentRanges() {
        assertEquals("刚刚", formatRelativeTime(now, now, TimeZone.UTC))
        assertEquals("刚刚", formatRelativeTime(now - 59, now, TimeZone.UTC))
        assertEquals("1分钟前", formatRelativeTime(now - 60, now, TimeZone.UTC))
        assertEquals("1小时前", formatRelativeTime(now - 3_600, now, TimeZone.UTC))
        assertEquals("1天前", formatRelativeTime(now - 86_400, now, TimeZone.UTC))
    }

    @Test
    fun formatRelativeTimeAddsYearOnlyWhenNeeded() {
        assertEquals("05-25 12:00", formatRelativeTime(now - 7 * 86_400, now, TimeZone.UTC))

        val newYear = Instant.parse("2024-01-01T00:00:00Z").epochSecond
        val lastYear = Instant.parse("2023-12-25T00:00:00Z").epochSecond
        assertEquals("2023-12-25", formatRelativeTime(lastYear, newYear, TimeZone.UTC))
    }

    @Test
    fun formatRelativeTimeHandlesFutureAndMissingTimestamps() {
        assertEquals("刚刚", formatRelativeTime(now + 3_600, now, TimeZone.UTC))
        assertEquals("", formatRelativeTime(0, now, TimeZone.UTC))
        assertEquals("", formatRelativeTime(-1, now, TimeZone.UTC))
    }

    @Test
    fun formatChatTimeUsesCompactChatStyle() {
        assertEquals("11:00", formatChatTime(now - 3_600, now, TimeZone.UTC))
        assertEquals("昨天 12:00", formatChatTime(now - 86_400, now, TimeZone.UTC))
        assertEquals("05-01 12:00", formatChatTime(now - 31 * 86_400, now, TimeZone.UTC))

        val newYear = Instant.parse("2024-01-01T00:00:00Z").epochSecond
        val lastYear = Instant.parse("2023-05-01T12:00:00Z").epochSecond
        assertEquals("2023-05-01 12:00", formatChatTime(lastYear, newYear, TimeZone.UTC))
        assertEquals("", formatChatTime(0, now, TimeZone.UTC))
    }

    @Test
    fun formatShortDateUsesMonthAndDay() {
        assertEquals("06-01", formatShortDate(now, TimeZone.UTC))
        assertEquals("", formatShortDate(0, TimeZone.UTC))
    }

    @Test
    fun formatDailyDateUsesChineseDate() {
        assertEquals("2024年06月01日", formatDailyDate("20240601"))
        assertEquals("invalid", formatDailyDate("invalid"))
    }
}

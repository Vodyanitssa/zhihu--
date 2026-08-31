package com.zhihuminus.data.zhihu.dto

/**
 * 一页在线浏览历史记录及续页游标。[nextUrl] 为 null 表示没有下一页。
 */
data class HistoryPage(
    val items: List<HistoryItemDto>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

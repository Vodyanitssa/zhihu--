package com.zhihuminus.data.zhihu.dto

/**
 * 一页 feed 条目及续页游标。[nextUrl] 为 null 表示没有下一页。
 */
data class FeedPage(
    val items: List<FeedDto>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

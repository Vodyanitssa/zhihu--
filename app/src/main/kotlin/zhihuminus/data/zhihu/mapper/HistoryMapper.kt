package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.zhihu.dto.HistoryItemDto
import com.zhihuminus.feature.history.HistoryItem

/**
 * 浏览历史数据模型映射器。
 */
object HistoryMapper {
    fun toHistoryItem(dto: HistoryItemDto): HistoryItem = HistoryItem(
        title = dto.data.header.title,
        summary = dto.data.content?.summary ?: "",
        details = dto.data.matrix
            ?.firstOrNull()
            ?.data
            ?.text
            ?.takeIf { it != dto.data.extra.contentType }
            ?: "",
        authorName = dto.data.content?.authorName,
        contentTypeLabel = when (
            dto.data.extra.contentType
                .lowercase()
        ) {
            "answer" -> "回答"
            "article" -> "文章"
            "pin" -> "想法"
            "question" -> "问题"
            "profile" -> "主页"
            else -> dto.data.extra.contentType
        },
        actionUrl = dto.data.action.url,
        contentToken = dto.data.extra.contentToken,
        contentType = dto.data.extra.contentType,
        coverImage = dto.data.content
            ?.coverImage
            ?.takeIf { it.isNotBlank() },
    )
}

fun HistoryItemDto.toHistoryItem(): HistoryItem = HistoryMapper.toHistoryItem(this)

package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class HistoryItemDto(
    val cardType: String,
    val data: HistoryDataDto,
)

@Serializable
data class HistoryDataDto(
    val header: HistoryHeaderDto,
    val content: HistoryContentDto? = null,
    val action: HistoryActionDto,
    val extra: HistoryExtraDto,
    val matrix: List<HistoryMatrixItemDto>? = null,
)

@Serializable
data class HistoryMatrixItemDto(
    val type: String,
    val data: HistoryMatrixDataDto,
)

@Serializable
data class HistoryMatrixDataDto(
    val text: String,
)

@Serializable
data class HistoryHeaderDto(
    val icon: String,
    val title: String,
    val action: HistoryActionDto? = null,
)

@Serializable
data class HistoryContentDto(
    val authorName: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
)

@Serializable
data class HistoryActionDto(
    val type: String,
    val url: String,
)

@Serializable
data class HistoryExtraDto(
    val contentToken: String,
    val contentType: String,
    val readTime: Long,
    val questionToken: String,
)

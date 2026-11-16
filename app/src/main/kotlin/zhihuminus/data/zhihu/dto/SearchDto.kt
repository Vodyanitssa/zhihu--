package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class SearchItemDto(
    val type: String = "",
    val id: String = "",
    @SerialName("object")
    val obj: JsonElement? = null,
)

@Serializable
@SerialName("scholar")
data class ScholarTargetDto(
    val id: String,
    override val url: String = "",
    override val title: String = "",
    override val excerpt: String? = null,
    val keywords: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val journal: String = "",
    val year: Int = 0,
    val period: String = "",
) : FeedTargetDto {
    override val author: FeedAuthorDto?
        get() = null

    override val createdTime: Long
        get() = -1

    override val updatedTime: Long
        get() = -1

    override val detailsText: String = listOfNotNull(
        "论文",
        journal.takeIf { it.isNotBlank() },
        year.takeIf { it > 0 }?.toString(),
    ).joinToString(" · ")
}

@Serializable
data class TopicSearchDto(
    val id: String,
    val type: String,
    val url: String,
    val name: String,
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val excerpt: String = "",
    val visitCount: Long = 0,
    val topAnswerCount: Long = 0,
    val isFollowing: Boolean = false,
)

@Serializable
data class SearchResponseDto(
    val data: List<SearchItemDto> = emptyList(),
    val paging: PagingDto? = null,
)

@Serializable
data class HotSearchItemDto(
    val query: String = "",
    val hotShow: String = "",
    val label: String = "",
)

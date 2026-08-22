package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PinDto(
    val id: String,
    val content: List<PinContentItemDto> = emptyList(),
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val created: Long = 0,
    val updated: Long = 0,
    val author: AuthorDto,
    val topics: List<TopicDto>? = null,
    val excerptTitle: String = "",
    val contentHtml: String = "",
    val virtuals: JsonObject? = null,
    val bottomPoll: PinBottomPollDto? = null,
)

@Serializable
data class PinContentItemDto(
    val type: String,
    val content: String? = null,
    val title: String? = null,
    val url: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val thumbnail: String? = null,
    val dataContentId: String? = null,
    val dataContentType: String? = null,
)

@Serializable
data class PinBottomPollDto(
    val voting: PinPollDto? = null,
)

@Serializable
data class PinPollDto(
    val id: String,
    val title: String = "",
    val maxSelections: Int = 1,
    val type: String = "",
    val beginAt: Long = 0L,
    val endAt: Long = -1L,
    val votingCount: Int = 0,
    val memberCount: Int = 0,
    val isVoted: Boolean = false,
    val isReviewing: Boolean = false,
    val options: List<PinPollOptionDto> = emptyList(),
)

@Serializable
data class PinPollOptionDto(
    val id: String,
    val title: String = "",
    val votingCount: Int = 0,
    val isSelected: Boolean = false,
)

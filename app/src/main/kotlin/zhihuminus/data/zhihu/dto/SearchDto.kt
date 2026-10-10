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
    @SerialName("hot_show")
    val hotShow: String = "",
    val label: String = "",
)

@Serializable
data class HotSearchResponseDto(
    @SerialName("hot_search_queries")
    val hotSearchQueries: List<HotSearchItemDto> = emptyList(),
)

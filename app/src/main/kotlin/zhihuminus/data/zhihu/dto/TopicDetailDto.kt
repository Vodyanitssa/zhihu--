package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class TopicDetailDto(
    val id: String,
    val name: String = "",
    val excerpt: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val questionsCount: Int = 0,
    val isFollowing: Boolean = false,
    val topicId: Long? = null,
    val totalPv: Long = 0,
    val discussCount: Long = 0,
)

@Serializable
data class TopicPinFeedDto(
    val type: String,
    val target: TopicPinTargetDto,
)

@Serializable
data class TopicPinTargetDto(
    val id: JsonPrimitive,
    val type: String,
    val url: String = "",
    val author: TopicPinAuthorDto,
    val title: String = "",
    val excerpt: String = "",
    val content: String = "",
    val plainContent: String = "",
    val counter: TopicPinCounterDto = TopicPinCounterDto(),
)

@Serializable
data class TopicPinAuthorDto(
    val avatarUrl: String = "",
    val name: String = "",
)

@Serializable
data class TopicPinCounterDto(
    val applaud: Int = 0,
    val comment: Int = 0,
    val favorite: Int = 0,
    val forward: Int = 0,
    val pv: Int = 0,
)

@Serializable
data class TopicFeedResponseDto(
    val data: List<JsonElement> = emptyList(),
    val paging: PagingDto? = null,
)

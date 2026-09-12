package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class TopicDetailDto(
    val id: String,
    val name: String = "",
    val excerpt: String = "",
    val avatar_url: String? = null,
    val followers_count: Int = 0,
    val questions_count: Int = 0,
    val is_following: Boolean = false,
    val topic_id: Long? = null,
    val total_pv: String = "",
    val discuss_count: String = "",
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
    val plain_content: String = "",
    val counter: TopicPinCounterDto = TopicPinCounterDto(),
)

@Serializable
data class TopicPinAuthorDto(
    val avatar_url: String = "",
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
data class TopicPagingDto(
    val is_end: Boolean = true,
    val next: String? = null,
)

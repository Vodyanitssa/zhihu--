package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

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
)

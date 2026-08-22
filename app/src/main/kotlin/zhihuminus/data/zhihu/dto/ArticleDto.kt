package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleDto(
    val id: Long,
    val title: String,
    val content: String,
    val excerpt: String = "",
    val voteupCount: Int = 0,
    val commentCount: Int = 0,
    val created: Long = 0,
    val updated: Long = 0,
    val author: AuthorDto,
    val ipInfo: String? = null,
    val reaction: ReactionDto? = null,
    val topics: List<TopicDto> = emptyList(),
    val segmentInfos: List<SegmentInfoDto> = emptyList(),
)

@Serializable
data class TopicDto(
    val id: String,
    val name: String,
    val url: String = "",
)

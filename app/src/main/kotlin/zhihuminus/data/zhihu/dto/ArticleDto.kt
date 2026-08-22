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

@Serializable
data class AuthorDto(
    val id: String,
    val name: String,
    val headline: String = "",
    val avatarUrl: String,
    val urlToken: String = "",
    val badgeV2: BadgeV2Dto? = null,
)

@Serializable
data class BadgeV2Dto(
    val title: String = "",
    val detailBadges: List<BadgeItemDto> = emptyList(),
)

@Serializable
data class BadgeItemDto(
    val type: String = "",
    val description: String = "",
    val icon: String = "",
)

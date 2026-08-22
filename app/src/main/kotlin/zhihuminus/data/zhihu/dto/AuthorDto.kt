package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class AuthorDto(
    val id: String,
    val name: String,
    val headline: String = "",
    val avatarUrl: String,
    val urlToken: String = "",
    val badgeV2: BadgeV2Dto? = null,
    val isFollowing: Boolean = false,
)

@Serializable
data class BadgeV2Dto(
    val title: String = "",
    val detailBadges: List<BadgeItemDto>? = null,
)

@Serializable
data class BadgeItemDto(
    val type: String = "",
    val description: String = "",
    val icon: String = "",
)

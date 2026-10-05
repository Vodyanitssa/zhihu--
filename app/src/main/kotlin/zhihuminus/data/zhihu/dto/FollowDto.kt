package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class FollowingUserActorDto(
    val id: String = "",
    val urlToken: String = "",
    val name: String = "",
    val avatarUrl: String = "",
)

@Serializable
data class FollowingUserItemDto(
    val actor: FollowingUserActorDto,
    val unreadCount: Int = 0,
)

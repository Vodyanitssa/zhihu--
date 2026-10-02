package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.Feed
import com.zhihuminus.data.Person
import com.zhihuminus.data.ZhihuPaging
import kotlinx.serialization.Serializable

@Serializable
data class CollectionDto(
    val id: String,
    val isFavorited: Boolean = false,
    val type: String = "collection",
    val title: String = "",
    val isPublic: Boolean = false,
    val url: String = "",
    val description: String = "",
    val followerCount: Int = 0,
    val answerCount: Int = 0,
    val itemCount: Int = 0,
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLiking: Boolean = false,
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val creator: Person? = null,
    val isDefault: Boolean = false,
)

@Serializable
data class CollectionResponseDto(
    val data: List<CollectionDto> = emptyList(),
    val paging: ZhihuPaging = ZhihuPaging(isEnd = true, next = ""),
)

@Serializable
data class CollectionItemDto(
    val created: String = "",
    val content: Feed.Target,
)

@Serializable
data class CollectionItemsPageDto(
    val data: List<CollectionItemDto> = emptyList(),
    val paging: ZhihuPaging = ZhihuPaging(isEnd = true, next = ""),
)

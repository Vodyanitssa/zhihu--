package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.DataHolder
import kotlinx.serialization.Serializable

@Serializable
data class FollowedQuestionDto(
    val id: String,
    val type: String = "question",
    val url: String = "",
    val title: String = "",
    val questionType: String = "",
    val created: Long = 0L,
    val updatedTime: Long = 0L,
)

@Serializable
data class FollowedTopicDto(
    val id: String = "",
    val type: String = "topic",
    val url: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val topic: DataHolder.Topic? = null,
) {
    val displayId: String get() = topic?.id ?: id
    val displayName: String get() = topic?.name ?: name
    val displayAvatarUrl: String? get() = topic?.avatarUrl ?: avatarUrl
}

data class PeoplePageDto<T>(
    val items: List<T>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

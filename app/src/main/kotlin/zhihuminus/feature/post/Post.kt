package com.zhihuminus.feature.post

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.data.VoteUpState

enum class PostType {
    Answer,
    Article,
    Pin,
}

data class Author(
    val id: String,
    val name: String,
    val headline: String = "",
    val avatarUrl: String,
    val urlToken: String = "",
    val badgeText: String? = null,
)

data class Post(
    val id: Long,
    val type: PostType,
    val title: String,
    val author: Author,
    val content: List<ContentNode>,
    val voteCount: Int,
    val commentCount: Int,
    val voteState: VoteUpState = VoteUpState.Neutral,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val ipInfo: String? = null,
    val excerpt: String = "",
    // Answer-specific
    val questionId: Long? = null,
    // Article-specific
    val topics: List<String> = emptyList(),
)

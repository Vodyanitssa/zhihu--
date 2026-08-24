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
    val isFollowing: Boolean = false,
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
    // Pin-specific
    val poll: PostPoll? = null,
    val linkCards: List<PostLinkCard> = emptyList(),
)

data class PostPoll(
    val id: String,
    val title: String,
    val maxSelections: Int,
    val votingCount: Int,
    val memberCount: Int,
    val isVoted: Boolean,
    val isReviewing: Boolean,
    val endAt: Long,
    val options: List<PostPollOption>,
) {
    fun acceptsVote(
        nowEpochSeconds: Long = kotlin.time.Clock.System
            .now()
            .epochSeconds,
    ): Boolean =
        !isReviewing && (endAt !in 0..nowEpochSeconds)

    fun statusText(): String {
        val voteState = if (isVoted) {
            "已投票"
        } else if (maxSelections > 1) {
            "最多选择 $maxSelections 项"
        } else {
            "最多选择一项"
        }
        val validity = when {
            endAt < 0 -> "长期有效"
            endAt <= kotlin.time.Clock.System
                .now()
                .epochSeconds -> "投票已结束"

            else -> null
        }
        return buildString {
            append(voteState)
            if (isVoted || memberCount > 0) {
                append("，")
                append(memberCount)
                append(" 人参与")
            }
            if (validity != null) {
                append("，")
                append(validity)
            }
        }
    }
}

data class PostPollOption(
    val id: String,
    val title: String,
    val votingCount: Int,
    val isSelected: Boolean,
)

data class PostLinkCard(
    val dataContentId: String,
    val dataContentType: String,
    val url: String,
)

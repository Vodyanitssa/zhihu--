package com.zhihuminus.feature.topic

import com.zhihuminus.data.FeedDisplayItem

data class TopicDetail(
    val id: String,
    val name: String = "",
    val excerpt: String = "",
    val avatarUrl: String? = null,
    val followersCount: Int = 0,
    val questionsCount: Int = 0,
    val isFollowing: Boolean = false,
    val topicId: Long? = null,
    val totalPv: String = "",
    val discussCount: String = "",
)

enum class TopicFeedTab(
    val title: String,
) {
    Discussion("讨论"),
    Ideas("想法"),
    Unanswered("待回答"),
}

enum class TopicDiscussionSort(
    val title: String,
) {
    Essence("精华"),
    Hot("最热"),
    Timeline("最新"),
}

enum class TopicIdeasSort(
    val title: String,
    val endpoint: String,
) {
    Hot("最热", "pin-hot"),
    Latest("最新", "pin-new"),
}

data class TopicFeedResult(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
    val error: String? = null,
)

interface TopicRepository {
    suspend fun getTopicDetail(topicId: String): TopicDetail

    suspend fun loadTopicFeed(
        topicId: String,
        tab: TopicFeedTab,
        discussionSort: TopicDiscussionSort,
        ideasSort: TopicIdeasSort,
        nextUrl: String?,
    ): TopicFeedResult

    suspend fun setFollowing(topicId: String, following: Boolean): Result<Unit>
}

fun formatTopicCount(raw: String): String {
    val value = raw.toLongOrNull() ?: return raw

    fun scaled(divisor: Double, unit: String): String {
        val number = (value / divisor * 10).toLong() / 10.0
        val text = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
        return "$text $unit"
    }
    return when {
        value >= 100_000_000 -> scaled(100_000_000.0, "亿")
        value >= 10_000 -> scaled(10_000.0, "万")
        else -> value.toString()
    }
}

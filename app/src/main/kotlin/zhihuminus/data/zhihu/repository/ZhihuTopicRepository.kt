package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.api.ZhihuTopicApi
import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.dto.TopicPinFeedDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.topic.TopicDetail
import com.zhihuminus.feature.topic.TopicDiscussionSort
import com.zhihuminus.feature.topic.TopicFeedResult
import com.zhihuminus.feature.topic.TopicFeedTab
import com.zhihuminus.feature.topic.TopicIdeasSort
import com.zhihuminus.feature.topic.TopicRepository
import com.zhihuminus.navigation.PostDestination
import io.ktor.http.Url
import kotlinx.serialization.json.JsonElement

class ZhihuTopicRepository(
    private val api: ZhihuTopicApi,
) : TopicRepository {
    override suspend fun getTopicDetail(topicId: String): TopicDetail {
        val dto = api.getTopicDetail(topicId)
        return TopicDetail(
            id = dto.id,
            name = dto.name,
            excerpt = dto.excerpt,
            avatarUrl = dto.avatarUrl,
            followersCount = dto.followersCount,
            questionsCount = dto.questionsCount,
            isFollowing = dto.isFollowing,
            topicId = dto.topicId,
            viewCount = dto.totalPv,
            discussCount = dto.discussCount,
        )
    }

    override suspend fun loadTopicFeed(
        topicId: String,
        tab: TopicFeedTab,
        discussionSort: TopicDiscussionSort,
        ideasSort: TopicIdeasSort,
        nextUrl: String?,
    ): TopicFeedResult {
        val url = nextUrl ?: topicFeedUrl(topicId, tab, discussionSort, ideasSort)
        val feedResponse = api.getTopicFeed(url, "data[*].content,excerpt,target.author.badge_v2")
        val responseItems = feedResponse.data
        val loadedItems = if (tab == TopicFeedTab.Ideas) {
            decodeTopicPinFeeds(responseItems)
        } else {
            val feeds = responseItems.mapNotNull { element ->
                runCatching { ZhihuJson.decodeJson<FeedDto>(element) }.getOrNull()
            }
            feeds.flattenFeeds().map { it.toDisplayItem() }
        }
        if (responseItems.isNotEmpty() && loadedItems.isEmpty()) {
            return TopicFeedResult(
                items = emptyList(),
                nextUrl = null,
                isEnd = true,
                error = "话题内容解码失败：服务端返回 ${responseItems.size} 项，但没有可显示内容",
            )
        }
        val paging = feedResponse.paging
        val rawNext = paging?.next
        val normalizedNext = rawNext?.let(::normalizeTopicPagingUrl)
        val isEnd = if (rawNext != null && normalizedNext == null) {
            true
        } else {
            paging?.hasMore != true
        }
        val error = if (rawNext != null && normalizedNext == null) {
            "服务端返回了不受信任的分页地址，已停止加载"
        } else {
            null
        }
        return TopicFeedResult(
            items = loadedItems,
            nextUrl = normalizedNext,
            isEnd = isEnd,
            error = error,
        )
    }

    override suspend fun setFollowing(topicId: String, following: Boolean): Result<Unit> = runCatching {
        api.followTopic(topicId, following)
    }
}

private fun topicFeedUrl(
    topicId: String,
    tab: TopicFeedTab,
    discussionSort: TopicDiscussionSort = TopicDiscussionSort.Hot,
    ideasSort: TopicIdeasSort = TopicIdeasSort.Hot,
): String = when (tab) {
    TopicFeedTab.Discussion -> when (discussionSort) {
        TopicDiscussionSort.Essence -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/top_activity/v2?limit=20&offset=0"
        TopicDiscussionSort.Hot -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/essence/v2?limit=20&offset=0"
        TopicDiscussionSort.Timeline -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/timeline_activity/v2?limit=20&offset=0"
    }
    TopicFeedTab.Ideas -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/${ideasSort.endpoint}?offset=0&limit=10"
    TopicFeedTab.Unanswered -> "https://www.zhihu.com/api/v5.1/topics/$topicId/feeds/top_question/v2?limit=20&offset=0"
}

private fun decodeTopicPinFeeds(items: List<JsonElement>): List<FeedDisplayItem> =
    items.mapNotNull { element ->
        runCatching { ZhihuJson.decodeJson<TopicPinFeedDto>(element) }.getOrNull()?.target?.let { target ->
            val pinId = target.id.content.toLongOrNull() ?: return@let null
            FeedDisplayItem(
                title = target.title.ifBlank { "想法" },
                summary = target.plainContent
                    .ifBlank { target.excerpt.ifBlank { target.content } }
                    .takeIf(String::isNotBlank),
                details = "想法 · ${target.counter.applaud} 赞 · ${target.counter.comment} 评论",
                feed = null,
                navDestinationJson = PostDestination(
                    type = PostType.Pin,
                    id = pinId,
                ).toFeedDisplayItemNavDestinationJson(),
                avatarSrc = target.author.avatarUrl,
                authorName = target.author.name,
                contentTypeLabel = "想法",
            )
        }
    }

private fun normalizeTopicPagingUrl(rawUrl: String): String? {
    val url = runCatching { Url(rawUrl) }.getOrNull() ?: return null
    if (url.host == "www.zhihu.com" &&
        (url.encodedPath.startsWith("/api/v4/") || url.encodedPath.startsWith("/api/v5.1/"))
    ) {
        return "https://www.zhihu.com${url.encodedPath}" + url.encodedQuery
            .takeIf(String::isNotEmpty)
            ?.let { "?$it" }
            .orEmpty()
    }
    if (url.host != "172.16.201.121" || url.port != 80) return null
    return buildString {
        append("https://www.zhihu.com/api/v4")
        append(url.encodedPath)
        if (url.encodedQuery.isNotEmpty()) append('?').append(url.encodedQuery)
    }
}

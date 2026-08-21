/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.viewmodel.filter

import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.target
import com.zhihuminus.filter.ContentOpenEventSupport
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Pin
import com.zhihuminus.platform.SettingsStore
import kotlinx.serialization.json.Json

/**
 * Unified feed filter pipeline that merges all filtering concerns into a single pass:
 * 1. Foreground read filtering — removes already-viewed content (local, fast).
 * 2. Ad blocking — removes ads, WeChat official accounts, etc. (settings-based, local).
 * 3. Keyword blocking — exact-match keyword filtering (local).
 * 4. Topic blocking — removes content above the blocked-topic threshold (local).
 *
 * Author follow status is ignored — all items go through the same filtering logic.
 * Blocked feed records are not persisted to the database.
 */
class FeedFilterPipeline(
    private val settings: FeedFilterSettings,
    private val blockedKeywordDao: BlockedKeywordDao,
    private val blockedTopicDao: BlockedTopicDao,
    private val contentFilterManager: ContentFilterManager,
    private val contentDetailProvider: ContentDetailProvider,
    private val onDetailFetchFailed: (FeedDisplayItem) -> Unit = {},
    private val onDetailsKeywordFiltered: (FeedDisplayItem, String) -> Unit = { _, _ -> },
) {
    suspend fun filter(items: List<FeedDisplayItem>): List<FeedDisplayItem> {
        if (!settings.enableContentFilter) {
            return items
        }
        val result = items.filterNot { shouldFilter(it) }
        return result
    }

    private suspend fun shouldFilter(it: FeedDisplayItem): Boolean {
        if (settings.enableKeywordBlocking) {
            if (containsBlockedKeyword(it.title)) {
                return true
            }
        }
        if (settings.enableTopicBlocking) {
            if (containsBlockedTopic(extractTopicIds(resolveRawContent(it)))) {
                return true
            }
        }
        return false
    }

    private suspend fun containsBlockedKeyword(
        text: String?,
    ): Boolean {
        if (text.isNullOrBlank()) return false
        val keywords = blockedKeywordDao.getAllKeywords()
        return keywords.any { blockedKeyword ->
            runCatching {
                when {
                    blockedKeyword.isRegex -> {
                        val pattern = if (blockedKeyword.caseSensitive) {
                            Regex(blockedKeyword.keyword)
                        } else {
                            Regex(blockedKeyword.keyword, RegexOption.IGNORE_CASE)
                        }
                        pattern.containsMatchIn(text)
                    }

                    blockedKeyword.caseSensitive -> text.contains(blockedKeyword.keyword)
                    else -> text.contains(blockedKeyword.keyword, ignoreCase = true)
                }
            }.getOrDefault(false)
        }
    }

    private suspend fun containsBlockedTopic(
        ids: List<String>?,
    ): Boolean {
        if (ids.isNullOrEmpty()) return false
        val topics = blockedTopicDao.getAllTopics()
        return topics.any { it.topicId in ids }
    }

    private suspend fun resolveRawContent(item: FeedDisplayItem): DataHolder.Content =
        when (val dest = item.navDestination) {
            is Article -> contentDetailProvider.get(dest) ?: DataHolder.DummyContent
            is Pin -> contentDetailProvider.get(dest) ?: DataHolder.DummyContent
            else -> DataHolder.DummyContent
        }
}

fun interface ContentDetailProvider {
    suspend fun get(navDestination: NavDestination): DataHolder.Content?
}

/**
 * 常见内容身份类型。
 * 用于 feed 过滤、内容打开记录和导航查询，不属于 Android 平台语义。
 */
object ContentType {
    const val ANSWER = "answer"
    const val ARTICLE = "article"
    const val QUESTION = "question"
    const val TOPIC = "topic"
    const val COLUMN = "column"
    const val VIDEO = "video"
    const val PIN = "pin"
}

/**
 * 从 feed item 提炼出的内容快照。
 * 这个结构只在 feed 过滤流水线内部流转，用来承接关键词、NLP、作者、主题等内容级规则。
 */
data class FilterableContent(
    val title: String,
    val summary: String?,
    val content: String?,
    val authorName: String?,
    val authorId: String?,
    val contentId: String,
    val contentType: String,
    val raw: DataHolder.Content,
    val url: String? = null,
    val feedJson: String? = null,
    val navDestinationJson: String? = null,
)

data class FeedContentIdentity(
    val type: String,
    val id: String,
)

fun FeedDisplayItem.resolveContentIdentity(): FeedContentIdentity {
    val identity = navDestination?.let(ContentOpenEventSupport::toTrackedContentIdentity)
    return if (identity != null) {
        FeedContentIdentity(identity.type, identity.id)
    } else {
        FeedContentIdentity("unknown", navDestination.hashCode().toString())
    }
}

fun FeedDisplayItem.toFilterableContent(
    identity: FeedContentIdentity,
    rawContent: DataHolder.Content,
): FilterableContent = FilterableContent(
    title = title,
    summary = summary,
    content = when (rawContent) {
        is DataHolder.Answer -> rawContent.content
        is DataHolder.Article -> rawContent.content
        is DataHolder.Pin -> rawContent.contentHtml
        else -> null
    } ?: content ?: summary,
    authorName = authorName,
    authorId = rawContent.author?.id,
    contentId = identity.id,
    contentType = identity.type,
    raw = rawContent,
    url = feed?.target?.url,
    feedJson = feed?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
    navDestinationJson = navDestination?.let { runCatching { feedFilterRecordJson.encodeToString(it) }.getOrNull() },
)

/** 从内容实体中提取主题 ID 列表，供 feed 过滤阶段的主题规则使用。 */
fun extractTopicIds(raw: DataHolder.Content): List<String>? = when (raw) {
    is DataHolder.Answer -> raw.question.topics.map { it.id }
    is DataHolder.Question -> raw.topics.map { it.id }
    is DataHolder.Article -> raw.topics?.map { it.id }
    is DataHolder.Pin -> raw.topics?.map { it.id }
    else -> null
}

private val DataHolder.Content.author: DataHolder.Author?
    get() = when (this) {
        is DataHolder.Answer -> this.author
        is DataHolder.Article -> this.author
        is DataHolder.Pin -> this.author
        is DataHolder.Question -> this.author
        else -> null
    }

private val feedFilterRecordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

data class FeedAdBlockSettings(
    val blockZhihuAdPlatform: Boolean = true,
    val blockZhihuSchool: Boolean = true,
    val blockWeChatOfficialAccount: Boolean = true,
    val blockPaidContent: Boolean = true,
)

fun getFeedAdBlockReason(
    content: FilterableContent,
    settings: FeedAdBlockSettings,
): String? = when (val raw = content.raw) {
    is DataHolder.Answer -> {
        if (settings.blockPaidContent && raw.paidInfo != null) {
            "知乎盐选付费内容"
        } else {
            getLinkBasedAdReason(raw.content, settings)
        }
    }

    is DataHolder.Article -> {
        if (settings.blockPaidContent && raw.paidInfo != null) {
            "知乎盐选付费内容"
        } else {
            getLinkBasedAdReason(raw.content, settings)
        }
    }

    is DataHolder.Pin -> getLinkBasedAdReason(raw.contentHtml, settings)
    else -> null
}

private fun getLinkBasedAdReason(
    content: String,
    settings: FeedAdBlockSettings,
): String? {
    if (settings.blockZhihuAdPlatform && "xg.zhihu.com" in content) return "知乎广告平台内容"
    if (settings.blockZhihuSchool && ("d.zhihu.com" in content || "data-edu-card-id" in content)) return "知乎学堂内容"
    if (settings.blockWeChatOfficialAccount && "mp.weixin.qq.com" in content) return "微信公众号文章"
    return null
}

data class FeedFilterSettings(
    val enableContentFilter: Boolean = true,
    val enableKeywordBlocking: Boolean = true,
    val enableTopicBlocking: Boolean = true,
    val topicBlockingThreshold: Int = 1,
    val adBlockSettings: FeedAdBlockSettings = FeedAdBlockSettings(),
)

fun SettingsStore.toFeedFilterSettings(): FeedFilterSettings = FeedFilterSettings(
    enableContentFilter = getBoolean("enableContentFilter", true),
    enableKeywordBlocking = getBoolean("enableKeywordBlocking", true),
    enableTopicBlocking = getBoolean("enableTopicBlocking", true),
    topicBlockingThreshold = getInt("topicBlockingThreshold", 1),
    adBlockSettings = FeedAdBlockSettings(
        blockZhihuAdPlatform = getBoolean("blockZhihuAdPlatform", true),
        blockZhihuSchool = getBoolean("blockZhihuSchool", true),
        blockWeChatOfficialAccount = getBoolean("blockWeChatOfficialAccount", true),
        blockPaidContent = getBoolean("blockPaidContent", true),
    ),
)

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

package com.zhihuminus.data

import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class FeedDisplayItem(
    val title: String,
    val summary: String?,
    val details: String,
    val feed: Feed?,
    val navDestinationJson: String? = null,
    val avatarSrc: String? = null,
    val authorName: String? = null,
    val authorBadgeV2: DataHolder.BadgeV2? = null,
    val content: String? = null,
    var raw: DataHolder.Content? = null,
    val contentTypeLabel: String? = null,
    val publishTimeSeconds: Long? = null,
) {
    val stableKey: String
        get() = navDestinationJson
            ?: feed?.target?.stableTargetKey
            ?: "$title|${summary.orEmpty()}|$details"
}

private val Feed.Target.stableTargetKey: String
    get() = when (this) {
        is Feed.AnswerTarget -> "answer:$id"
        is Feed.ArticleTarget -> "article:$id"
        is Feed.QuestionTarget -> "question:$id"
        is Feed.PinTarget -> "pin:$id"
        is Feed.VideoTarget -> "video:$id"
    }

fun List<Feed>.flattenFeeds(): List<Feed> = flatMap {
    (it as? GroupFeed)?.list ?: listOf(it)
}

fun Feed.toDisplayItem(): FeedDisplayItem = when (this) {
    is CommonFeed, is FeedItemIndexGroup, is MomentsFeed, is HotListFeed, is TopicFeed -> toTargetDisplayItem()

    is AdvertisementFeed -> FeedDisplayItem(
        title = ad.creatives
            .firstOrNull()
            ?.title ?: "",
        summary = ad.creatives
            .firstOrNull()
            ?.description ?: actionText,
        details = actionText + "广告",
        feed = this,
        content = ad.creatives
            .firstOrNull()
            ?.landingUrl,
    )

    is GroupFeed -> error("GroupFeed should be flattened before creating display items")
    is QuestionFeedCard -> FeedDisplayItem(
        title = target.title,
        summary = target.excerpt,
        details = listOfNotNull(target.detailsText, actionText).joinToString(" · "),
        avatarSrc = target.author?.avatarUrl,
        authorName = target.author?.name,
        authorBadgeV2 = target.author?.badgeV2,
        feed = this,
        contentTypeLabel = target.typeLabel,
        publishTimeSeconds = target.publishTimeSeconds,
    )
}

private val Feed.Target.typeLabel: String
    get() = description()

private val Feed.Target.publishTimeSeconds: Long?
    get() = createdTime.takeIf { it > 0 }

private fun Feed.toTargetDisplayItem(): FeedDisplayItem = when (val target = target) {
    is Feed.AnswerTarget,
    is Feed.ArticleTarget,
    is Feed.QuestionTarget,
    -> FeedDisplayItem(
        title = target.title,
        summary = target.excerpt,
        details = listOfNotNull(target.detailsText, actionText).joinToString(" · "),
        avatarSrc = target.author?.avatarUrl,
        authorName = target.author?.name,
        authorBadgeV2 = target.author?.badgeV2,
        feed = this,
        contentTypeLabel = target.typeLabel,
        publishTimeSeconds = target.publishTimeSeconds,
    )

    is Feed.PinTarget -> {
        val textContent = target.content
            .filterIsInstance<DataHolder.Pin.ContentText>()
            .firstOrNull()
        val title = textContent?.title.orEmpty()
        val contentSummary = textContent
            ?.content
            ?.let { Jsoup.parse(it).text() }
            ?.takeIf { it.isNotBlank() }
        val excerptSummary = target.excerpt
            ?.let { Jsoup.parse(it).text() }
            ?.takeIf { it.isNotBlank() }
        val summary = (contentSummary ?: excerptSummary)?.takeUnless { it == title }

        FeedDisplayItem(
            title = title,
            summary = summary,
            details = target.detailsText,
            avatarSrc = target.author.avatarUrl,
            authorName = target.author.name,
            authorBadgeV2 = target.author.badgeV2,
            feed = this,
            contentTypeLabel = target.typeLabel,
            publishTimeSeconds = target.publishTimeSeconds,
        )
    }

    else -> FeedDisplayItem(
        title = target?.description() ?: "广告",
        summary = "Not Implemented",
        details = target?.detailsText ?: "广告",
        feed = this,
        contentTypeLabel = target?.typeLabel,
        publishTimeSeconds = target?.publishTimeSeconds,
    )
}

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

import com.zhihuminus.data.zhihu.dto.AdvertisementFeedDto
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.CommonFeedDto
import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.dto.FeedItemIndexGroupDto
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.GroupFeedDto
import com.zhihuminus.data.zhihu.dto.MomentsFeedDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionFeedCardDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import com.zhihuminus.data.zhihu.dto.TopicFeedDto
import com.zhihuminus.data.zhihu.dto.VideoTargetDto
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class FeedDisplayItem(
    val title: String,
    val summary: String?,
    val details: String,
    val feed: FeedDto? = null,
    val navDestinationJson: String? = null,
    val avatarSrc: String? = null,
    val authorName: String? = null,
    val authorBadge: OfficialBadge? = null,
    val content: String? = null,
    val contentTypeLabel: String? = null,
    val publishTimeSeconds: Long? = null,
    val sourceLabel: String? = null,
    val thumbnailUrl: String? = null,
    val pinImages: List<String> = emptyList(),
) {
    val stableKey: String
        get() = navDestinationJson
            ?: feed?.target?.stableTargetKey
            ?: "$title|${summary.orEmpty()}|$details"
}

val PinContentItemDto.feedThumbnailUrl: String
    get() = thumbnail?.takeIf { it.isNotBlank() } ?: url.orEmpty()

private val FeedTargetDto.stableTargetKey: String
    get() = when (this) {
        is AnswerTargetDto -> "answer:$id"
        is ArticleTargetDto -> "article:$id"
        is QuestionTargetDto -> "question:$id"
        is PinTargetDto -> "pin:$id"
        is VideoTargetDto -> "video:$id"
    }

fun List<FeedDto>.flattenFeeds(): List<FeedDto> = flatMap {
    (it as? GroupFeedDto)?.list ?: listOf(it)
}

fun FeedDto.toDisplayItem(): FeedDisplayItem = when (this) {
    is CommonFeedDto, is FeedItemIndexGroupDto, is MomentsFeedDto, is TopicFeedDto -> toTargetDisplayItem()

    is AdvertisementFeedDto -> FeedDisplayItem(
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
        sourceLabel = actionText.takeIf { it.isNotBlank() },
    )

    is GroupFeedDto -> error("GroupFeed should be flattened before creating display items")
    is QuestionFeedCardDto -> FeedDisplayItem(
        title = target.title,
        summary = target.excerpt,
        details = listOfNotNull(target.detailsText, actionText).joinToString(" · "),
        avatarSrc = target.author?.avatarUrl,
        authorName = target.author?.name,
        authorBadge = target.author?.badgeV2?.toOfficialBadge(),
        feed = this,
        contentTypeLabel = target.typeLabel,
        publishTimeSeconds = target.publishTimeSeconds,
        thumbnailUrl = (target as? AnswerTargetDto)?.thumbnail,
        pinImages = (target as? PinTargetDto)
            ?.content
            ?.filter { it.type == "image" }
            ?.map { it.feedThumbnailUrl }
            .orEmpty(),
        navDestinationJson = target.navDestination?.toFeedDisplayItemNavDestinationJson(),
    )
}

private val FeedTargetDto.typeLabel: String
    get() = description()

private val FeedTargetDto.publishTimeSeconds: Long?
    get() = createdTime.takeIf { it > 0 }

private fun FeedDto.toTargetDisplayItem(): FeedDisplayItem {
    val currentTarget = target
    val resolvedSourceLabel = sourceLabel
    val resolvedThumbnail = (currentTarget as? AnswerTargetDto)?.thumbnail
    val resolvedNavDestinationJson = currentTarget?.navDestination?.toFeedDisplayItemNavDestinationJson()

    return when (currentTarget) {
        is AnswerTargetDto,
        is ArticleTargetDto,
        is QuestionTargetDto,
        -> FeedDisplayItem(
            title = currentTarget.title,
            summary = currentTarget.excerpt,
            details = listOfNotNull(currentTarget.detailsText, actionText).joinToString(" · "),
            avatarSrc = currentTarget.author?.avatarUrl,
            authorName = currentTarget.author?.name,
            authorBadge = currentTarget.author?.badgeV2?.toOfficialBadge(),
            feed = this,
            contentTypeLabel = currentTarget.typeLabel,
            publishTimeSeconds = currentTarget.publishTimeSeconds,
            sourceLabel = resolvedSourceLabel,
            thumbnailUrl = resolvedThumbnail,
            navDestinationJson = resolvedNavDestinationJson,
        )

        is PinTargetDto -> {
            val textContent = currentTarget.content
                .firstOrNull { it.type == "text" }
            val title = textContent?.title.orEmpty()
            val contentSummary = textContent
                ?.content
                ?.let { Jsoup.parse(it).text() }
                ?.takeIf { it.isNotBlank() }
            val excerptSummary = currentTarget.excerpt
                ?.let { Jsoup.parse(it).text() }
                ?.takeIf { it.isNotBlank() }
            val summary = (contentSummary ?: excerptSummary)?.takeUnless { it == title }
            val pinImages = currentTarget.content
                .filter { it.type == "image" }
                .map { it.feedThumbnailUrl }

            FeedDisplayItem(
                title = title,
                summary = summary,
                details = currentTarget.detailsText,
                avatarSrc = currentTarget.author?.avatarUrl,
                authorName = currentTarget.author?.name,
                authorBadge = currentTarget.author?.badgeV2?.toOfficialBadge(),
                feed = this,
                contentTypeLabel = currentTarget.typeLabel,
                publishTimeSeconds = currentTarget.publishTimeSeconds,
                sourceLabel = resolvedSourceLabel,
                thumbnailUrl = resolvedThumbnail,
                pinImages = pinImages,
                navDestinationJson = resolvedNavDestinationJson,
            )
        }

        else -> FeedDisplayItem(
            title = currentTarget?.description() ?: "广告",
            summary = "Not Implemented",
            details = currentTarget?.detailsText ?: "广告",
            feed = this,
            contentTypeLabel = currentTarget?.typeLabel,
            publishTimeSeconds = currentTarget?.publishTimeSeconds,
            sourceLabel = resolvedSourceLabel,
            thumbnailUrl = resolvedThumbnail,
            navDestinationJson = resolvedNavDestinationJson,
        )
    }
}

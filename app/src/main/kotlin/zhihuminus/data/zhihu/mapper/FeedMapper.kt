package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
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
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import org.jsoup.Jsoup

/**
 * 知乎信息流数据模型映射器。
 *
 * 负责将各类 Feed DTO（推荐流、关注流、话题流、搜索结果流等）转换为展示层模型 [FeedDisplayItem]，
 * 以及解析信息流 Target 对应的导航目标 [NavDestination] 与图片列表。
 */
object FeedMapper {
    fun toDisplayItem(feed: FeedDto): FeedDisplayItem = when (feed) {
        is CommonFeedDto, is FeedItemIndexGroupDto, is MomentsFeedDto, is TopicFeedDto -> toTargetDisplayItem(feed)

        is AdvertisementFeedDto -> FeedDisplayItem(
            title = feed.ad.creatives
                .firstOrNull()
                ?.title ?: "",
            summary = feed.ad.creatives
                .firstOrNull()
                ?.description ?: feed.actionText,
            details = feed.actionText + "广告",
            feed = feed,
            content = feed.ad.creatives
                .firstOrNull()
                ?.landingUrl,
            sourceLabel = feed.actionText.takeIf { it.isNotBlank() },
        )

        is GroupFeedDto -> error("GroupFeed should be flattened before creating display items")
        is QuestionFeedCardDto -> {
            val images = resolveFeedImages(feed.target)
            FeedDisplayItem(
                title = feed.target.title,
                summary = feed.target.excerpt,
                details = listOfNotNull(feed.target.detailsText, feed.actionText).joinToString(" · "),
                avatarSrc = feed.target.author?.avatarUrl,
                authorName = feed.target.author?.name,
                authorBadge = feed.target.author
                    ?.badgeV2
                    ?.toOfficialBadge(),
                feed = feed,
                contentTypeLabel = feed.target.description(),
                publishTimeSeconds = feed.target.createdTime.takeIf { it > 0 },
                images = images,
                navDestinationJson = toNavDestination(feed.target)?.toFeedDisplayItemNavDestinationJson(),
            )
        }
    }

    fun resolveFeedImages(target: FeedTargetDto): List<String> = when (target) {
        is PinTargetDto -> {
            target.content
                .filter { it.type == "image" }
                .map { it.thumbnail?.takeIf { t -> t.isNotBlank() } ?: it.url.orEmpty() }
        }

        is AnswerTargetDto -> listOfNotNull(target.thumbnail?.takeIf { it.isNotBlank() })

        is ArticleTargetDto -> listOfNotNull((target.thumbnail ?: target.imageUrl)?.takeIf { it.isNotBlank() })

        else -> emptyList()
    }

    fun toNavDestination(target: FeedTargetDto): NavDestination? = when (target) {
        is AnswerTargetDto -> PostDestination(
            title = target.question.title,
            type = PostType.Answer,
            id = target.id,
            authorName = target.author?.name ?: "loading...",
            authorBio = target.author?.headline ?: "",
            avatarSrc = target.author?.avatarUrl,
            excerpt = target.excerpt,
        )

        is ArticleTargetDto -> PostDestination(
            title = target.title,
            type = PostType.Article,
            id = target.id,
            authorName = target.author?.name ?: "loading...",
            authorBio = target.author?.headline ?: "",
            avatarSrc = target.author?.avatarUrl,
            excerpt = target.excerpt,
        )

        is PinTargetDto -> PostDestination(
            type = PostType.Pin,
            id = target.id,
            authorName = target.author?.name ?: "loading...",
        )

        is QuestionTargetDto -> Question(
            questionId = target.id,
            title = target.title,
        )

        is VideoTargetDto -> null
    }

    fun toStableTargetKey(target: FeedTargetDto): String = when (target) {
        is AnswerTargetDto -> "answer:${target.id}"
        is ArticleTargetDto -> "article:${target.id}"
        is QuestionTargetDto -> "question:${target.id}"
        is PinTargetDto -> "pin:${target.id}"
        is VideoTargetDto -> "video:${target.id}"
    }

    fun flattenFeeds(feeds: List<FeedDto>): List<FeedDto> = feeds.flatMap {
        (it as? GroupFeedDto)?.list ?: listOf(it)
    }

    private fun toTargetDisplayItem(feed: FeedDto): FeedDisplayItem {
        val currentTarget = feed.target
        val resolvedSourceLabel = feed.sourceLabel
        val resolvedNavDestinationJson = currentTarget?.let(::toNavDestination)?.toFeedDisplayItemNavDestinationJson()
        val resolvedImages = currentTarget?.let(::resolveFeedImages) ?: emptyList()

        return when (currentTarget) {
            is AnswerTargetDto,
            is ArticleTargetDto,
            is QuestionTargetDto,
            -> FeedDisplayItem(
                title = currentTarget.title,
                summary = currentTarget.excerpt,
                details = listOfNotNull(currentTarget.detailsText, feed.actionText).joinToString(" · "),
                avatarSrc = currentTarget.author?.avatarUrl,
                authorName = currentTarget.author?.name,
                authorBadge = currentTarget.author?.badgeV2?.toOfficialBadge(),
                feed = feed,
                contentTypeLabel = currentTarget.description(),
                publishTimeSeconds = currentTarget.createdTime.takeIf { it > 0 },
                sourceLabel = resolvedSourceLabel,
                images = resolvedImages,
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

                FeedDisplayItem(
                    title = title,
                    summary = summary,
                    details = currentTarget.detailsText,
                    avatarSrc = currentTarget.author?.avatarUrl,
                    authorName = currentTarget.author?.name,
                    authorBadge = currentTarget.author?.badgeV2?.toOfficialBadge(),
                    feed = feed,
                    contentTypeLabel = currentTarget.description(),
                    publishTimeSeconds = currentTarget.createdTime.takeIf { it > 0 },
                    sourceLabel = resolvedSourceLabel,
                    images = resolvedImages,
                    navDestinationJson = resolvedNavDestinationJson,
                )
            }

            else -> FeedDisplayItem(
                title = currentTarget?.description() ?: "广告",
                summary = "Not Implemented",
                details = currentTarget?.detailsText ?: "广告",
                feed = feed,
                contentTypeLabel = currentTarget?.description(),
                publishTimeSeconds = currentTarget?.createdTime?.takeIf { it > 0 },
                sourceLabel = resolvedSourceLabel,
                images = resolvedImages,
                navDestinationJson = resolvedNavDestinationJson,
            )
        }
    }
}

val PinContentItemDto.feedThumbnailUrl: String
    get() = thumbnail?.takeIf { it.isNotBlank() } ?: url.orEmpty()

val FeedTargetDto.stableTargetKey: String
    get() = FeedMapper.toStableTargetKey(this)

val FeedTargetDto.navDestination: NavDestination?
    get() = FeedMapper.toNavDestination(this)

fun FeedTargetDto.resolveFeedImages(): List<String> = FeedMapper.resolveFeedImages(this)

fun FeedDto.toDisplayItem(): FeedDisplayItem = FeedMapper.toDisplayItem(this)

fun List<FeedDto>.flattenFeeds(): List<FeedDto> = FeedMapper.flattenFeeds(this)

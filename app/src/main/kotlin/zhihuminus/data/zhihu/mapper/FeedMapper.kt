package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson
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
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.router.AppRouter
import io.ktor.http.decodeURLPart
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.jsoup.Jsoup
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

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

        is VideoTargetDto -> listOfNotNull(target.video?.thumbnail?.takeIf { it.isNotBlank() })

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

        is VideoTargetDto -> Video(id = target.id)
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

    /**
     * 将安卓端推荐流的 `ComponentCard` 解析为展示项。
     *
     * 安卓端卡片没有完整 target，无法构造 [FeedDto]；解析不到导航目标时返回 null，由调用方逐卡跳过。
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun toMobileDisplayItem(card: JsonObject): FeedDisplayItem? {
        if (card.string("type") != "ComponentCard") return null
        val parameter = card["action"]
            ?.asObject()
            ?.get("parameter")
            ?.asString()
            ?.substringAfter("route_url=", "")
            ?.substringBefore("&")
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val routeUrl = runCatching { parameter.decodeURLPart() }.getOrDefault(parameter)
        val destination = AppRouter.resolveDestination(routeUrl) ?: return null
        val children = card["children"]?.asArray()?.mapNotNull { it.asObject() }.orEmpty()
        val isPin = destination is PostDestination && destination.type == PostType.Pin
        val extraTitle = card["extra"]
            ?.asObject()
            ?.get("passthrough_info")
            ?.asObject()
            ?.get("content")
            ?.asObject()
            ?.string("title")
        val title = children
            .firstOrNull { it.string("id") == "Text" }
            ?.string("text")
            ?.takeIf { it.isNotBlank() }
            ?: extraTitle?.takeIf { it.isNotBlank() }
            ?: return null
        val summary = children
            .firstOrNull { it.string("id") == "text_pin_summary" }
            ?.string("text")
            ?.takeIf { it.isNotBlank() }
        val footerText = mobileFooterText(children)
        val (avatar, authorName) = mobileAuthor(children)

        return FeedDisplayItem(
            title = title,
            summary = summary,
            details = footerText,
            navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
            avatarSrc = avatar,
            authorName = authorName,
            contentTypeLabel = mobileContentTypeLabel(destination),
            images = if (isPin) mobilePinImages(card) else emptyList(),
        )
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

            is VideoTargetDto -> FeedDisplayItem(
                title = currentTarget.title.ifBlank { currentTarget.description() },
                summary = currentTarget.description
                    .ifBlank { currentTarget.excerpt }
                    .takeIf { it.isNotBlank() },
                details = listOfNotNull(currentTarget.detailsText, feed.actionText?.takeIf { it.isNotBlank() })
                    .joinToString(" · "),
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

private fun mobileContentTypeLabel(destination: NavDestination): String? = when (destination) {
    is PostDestination -> when (destination.type) {
        PostType.Answer -> "回答"
        PostType.Article -> "文章"
        PostType.Pin -> "想法"
    }

    is Video -> "视频"
    is Question -> "问题"
    else -> null
}

private fun mobileFooterText(children: List<JsonObject>): String {
    val lines = children.filter { it.string("type") == "Line" }
    val footer = lines.firstOrNull { it.string("style").orEmpty().startsWith("LineFooter") }
        ?: lines.firstOrNull { line ->
            line["elements"]?.asArray()?.any { (it as? JsonObject)?.containsKey("reaction") == true } == true
        }
        ?: lines.getOrNull(1)
        ?: return ""
    val elements = footer["elements"]?.asArray()?.mapNotNull { it.asObject() }.orEmpty()
    val vote = elements.firstOrNull { it.string("reaction") == "Vote" }
    val comment = elements.firstOrNull { it.string("reaction") == "Comment" }
    val collect = elements.firstOrNull { it.string("reaction") == "Collect" }
    return if (vote != null && comment != null && collect != null) {
        "${vote.int("count")} 赞同 · ${comment.int("count")} 评论 · ${collect.int("count")} 收藏"
    } else {
        elements.firstOrNull { it.string("type") == "Text" }?.string("text").orEmpty()
    }
}

private fun mobileAuthor(children: List<JsonObject>): Pair<String?, String?> {
    val line = children.firstOrNull {
        val style = it.string("style").orEmpty()
        style.startsWith("RecommendAuthorLine") || style.startsWith("LineAuthor_default")
    } ?: return null to null
    val elements = line["elements"]?.asArray()?.mapNotNull { it.asObject() }.orEmpty()
    val avatar = elements
        .firstOrNull { it.string("style") == "Avatar_default" }
        ?.get("image")
        ?.asObject()
        ?.string("url")
    val authorName = elements.firstOrNull { it.string("type") == "Text" }?.string("text")
    return avatar to authorName
}

@OptIn(ExperimentalEncodingApi::class)
private fun mobilePinImages(card: JsonObject): List<String> {
    val businessExt = card["extra"]?.asObject()?.get("business_ext_map")?.asObject()
    val thumbnails = businessExt
        ?.get("images")
        ?.asArray()
        ?.mapNotNull { it.asObject()?.string("url")?.takeIf(String::isNotBlank) }
        .orEmpty()
    if (thumbnails.isNotEmpty()) return thumbnails
    val encoded = businessExt?.string("ori_content")?.takeIf { it.isNotBlank() } ?: return emptyList()
    val original = runCatching {
        ZhihuJson.json.parseToJsonElement(Base64.Default.decode(encoded).decodeToString())
    }.getOrNull()?.asObject() ?: return emptyList()
    return original["media_info"]
        ?.asObject()
        ?.get("images")
        ?.asArray()
        ?.mapNotNull { it.asObject()?.string("url")?.takeIf(String::isNotBlank) }
        .orEmpty()
}

private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArray(): JsonArray? = this as? JsonArray

private fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonObject.string(key: String): String? = this[key].asString()

private fun JsonObject.int(key: String): Int = (this[key] as? JsonPrimitive)?.intOrNull ?: 0

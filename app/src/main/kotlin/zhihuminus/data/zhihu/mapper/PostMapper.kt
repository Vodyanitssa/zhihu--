package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.core.content.AstParser.parseContent
import com.zhihuminus.core.util.booleanCompat
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.dto.FeedTargetDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostLinkCard
import com.zhihuminus.feature.post.PostPoll
import com.zhihuminus.feature.post.PostPollOption
import com.zhihuminus.feature.post.PostTopic
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.VoteUpState
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup

/**
 * 知乎内容（回答、文章、想法）与作者数据模型映射器。
 *
 * 负责将远程传输模型（[AnswerDto]、[ArticleDto]、[PinDto]、[FeedTargetDto]）
 * 以及作者模型（[AuthorDto]、[FeedAuthorDto]）映射转换为领域实体 [Post] 与 [Author]。
 */
object PostMapper {
    private val PIN_DIV_PREFIX_REGEX = Regex("""^<div[^>]*>""", RegexOption.IGNORE_CASE)
    private val TRAILING_PIN_TITLE_REGEX = Regex("""(?:\s*</?br\s*/?>\s*|[ |])+$""", RegexOption.IGNORE_CASE)

    fun toAuthor(dto: AuthorDto): Author = Author(
        id = dto.id,
        name = dto.name,
        headline = dto.headline,
        avatarUrl = dto.avatarUrl,
        urlToken = dto.urlToken,
        badgeText = dto.badgeV2
            ?.detailBadges
            ?.firstOrNull()
            ?.description,
        isFollowing = dto.isFollowing,
        ipInfo = dto.ipInfo,
    )

    fun toAuthor(dto: FeedAuthorDto?): Author = if (dto == null) {
        Author(id = "", name = "知乎用户", avatarUrl = "")
    } else {
        Author(
            id = dto.id,
            name = dto.name,
            headline = dto.headline,
            avatarUrl = dto.avatarUrl,
            urlToken = dto.urlToken.orEmpty(),
            badgeText = dto.badgeV2
                ?.detailBadges
                ?.firstOrNull()
                ?.description ?: dto.badge?.firstOrNull()?.description,
            isFollowing = dto.isFollowing,
            ipInfo = dto.ipInfo,
        )
    }

    fun toPost(dto: AnswerDto): Post {
        val contentNodes = parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Answer,
            title = dto.question.title,
            author = toAuthor(dto.author),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            isFaved = dto.reaction?.relation?.faved,
            createdAt = dto.createdTime,
            updatedAt = dto.updatedTime,
            ipInfo = dto.ipInfo ?: dto.author.ipInfo,
            excerpt = dto.excerpt,
            questionId = dto.question.id,
        )
    }

    fun toPost(dto: ArticleDto): Post {
        val contentNodes = parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Article,
            title = dto.title,
            author = toAuthor(dto.author),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            isFaved = dto.reaction?.relation?.faved,
            createdAt = dto.created,
            updatedAt = dto.updated,
            ipInfo = dto.ipInfo ?: dto.author.ipInfo,
            excerpt = dto.excerpt.ifEmpty { dto.excerptTitle },
            topics = dto.topics.map { PostTopic(id = it.id, name = it.name) },
            coverUrl = dto.imageUrl?.takeIf { it.isNotBlank() },
        )
    }

    /**
     * 从想法的 contentHtml 中解析出想法标题和去除标题后的 HTML 正文。
     *
     * 想法如果有标题，通常位于最外层 <div> 与第一个 <p> 之间，并以 <br/>、<br> 或 "|" 分隔。
     * 解析时会去除末尾的换行标签、分隔符及空白，并解析 HTML 转义字符。
     */
    fun extractTitleAndBodyHtml(contentHtml: String): Pair<String?, String> {
        if (contentHtml.isBlank()) return null to contentHtml

        val firstP = contentHtml.indexOf("<p>", ignoreCase = true)
        if (firstP <= 0) return null to contentHtml

        val divMatch = PIN_DIV_PREFIX_REGEX.find(contentHtml)
        val startIndex = if (divMatch != null) divMatch.range.last + 1 else 0

        if (firstP <= startIndex) return null to contentHtml

        val rawTitle = contentHtml.substring(startIndex, firstP)
        val stripped = rawTitle.replace(TRAILING_PIN_TITLE_REGEX, "").trim()
        val cleanTitle = if (stripped.isNotBlank()) {
            Jsoup.parse(stripped).text().trim()
        } else {
            ""
        }

        val titleText = cleanTitle.takeIf { it.isNotBlank() }
        val remainingHtml = if (titleText != null) {
            contentHtml.removeRange(0, firstP)
        } else {
            contentHtml
        }

        return titleText to remainingHtml
    }

    fun toPost(dto: PinDto): Post {
        val (titleText, bodyHtml) = extractTitleAndBodyHtml(dto.contentHtml)
        val content = if (bodyHtml.isNotBlank()) parseContent(bodyHtml) else emptyList()
        val isLiked = dto.virtuals.booleanCompat("isLiked", "is_liked")
        val poll = dto.bottomPoll?.voting?.let { p ->
            PostPoll(
                id = p.id,
                title = p.title,
                maxSelections = p.maxSelections,
                votingCount = p.votingCount,
                memberCount = p.memberCount,
                isVoted = p.isVoted,
                isReviewing = p.isReviewing,
                endAt = p.endAt,
                options = p.options.map { o ->
                    PostPollOption(
                        id = o.id,
                        title = o.title,
                        votingCount = o.votingCount,
                        isSelected = o.isSelected,
                    )
                },
            )
        }
        val linkCards = dto.content
            .filter { it.type == "link_card" && !it.url.isNullOrBlank() }
            .map { item ->
                PostLinkCard(
                    dataContentId = item.dataContentId.orEmpty(),
                    dataContentType = item.dataContentType.orEmpty(),
                    url = item.url!!,
                )
            }
        val author = toAuthor(dto.author)
        val virtualsIp = dto.virtuals
            ?.get("ip_info")
            ?.jsonPrimitive
            ?.contentOrNull

        return Post(
            id = dto.id.toLong(),
            type = PostType.Pin,
            title = titleText ?: "${author.name}的想法",
            author = author,
            content = content,
            voteCount = dto.likeCount,
            commentCount = dto.commentCount,
            voteState = if (isLiked) VoteUpState.Up else VoteUpState.Neutral,
            createdAt = dto.created,
            updatedAt = dto.updated,
            ipInfo = dto.ipInfo ?: dto.author.ipInfo ?: virtualsIp,
            excerpt = dto.excerptTitle,
            topics = dto.topics?.map { PostTopic(id = it.id, name = it.name) }.orEmpty(),
            poll = poll,
            linkCards = linkCards,
        )
    }

    fun toPost(target: PinTargetDto): Post? {
        if (target.contentHtml.isBlank()) return null

        val author = toAuthor(target.author)
        val (titleText, bodyHtml) = extractTitleAndBodyHtml(target.contentHtml)
        val content = if (bodyHtml.isNotBlank()) parseContent(bodyHtml) else emptyList()
        val linkCards = target.content
            .filter { it.type == "link_card" && !it.url.isNullOrBlank() }
            .map { item ->
                PostLinkCard(
                    dataContentId = item.dataContentId.orEmpty(),
                    dataContentType = item.dataContentType.orEmpty(),
                    url = item.url!!,
                )
            }

        return Post(
            id = target.id,
            type = PostType.Pin,
            title = titleText ?: "${author.name}的想法",
            author = author,
            content = content,
            voteCount = target.likeCount,
            commentCount = target.commentCount,
            voteState = VoteUpState.Neutral,
            createdAt = target.created,
            updatedAt = target.updated,
            ipInfo = target.ipInfo ?: author.ipInfo,
            excerpt = target.excerptTitle,
            linkCards = linkCards,
        )
    }

    fun toPost(target: FeedTargetDto): Post? = when (target) {
        is AnswerTargetDto -> {
            if (target.content.isBlank()) {
                null
            } else {
                val voteState = when (target.relationship?.voting) {
                    1 -> VoteUpState.Up
                    -1 -> VoteUpState.Down
                    else -> VoteUpState.Neutral
                }
                Post(
                    id = target.id,
                    type = PostType.Answer,
                    title = target.question.title,
                    author = toAuthor(target.author),
                    content = parseContent(target.content),
                    voteCount = target.voteupCount,
                    commentCount = target.commentCount,
                    voteState = voteState,
                    isFaved = target.relationship?.isFollowing,
                    createdAt = target.createdTime,
                    updatedAt = target.updatedTime,
                    ipInfo = target.ipInfo ?: target.author?.ipInfo,
                    excerpt = target.excerpt.orEmpty(),
                    questionId = target.question.id,
                )
            }
        }

        is ArticleTargetDto -> {
            if (target.content.isBlank()) {
                null
            } else {
                val voteState = when (target.voting) {
                    1 -> VoteUpState.Up
                    -1 -> VoteUpState.Down
                    else -> VoteUpState.Neutral
                }
                Post(
                    id = target.id,
                    type = PostType.Article,
                    title = target.title,
                    author = toAuthor(target.author),
                    content = parseContent(target.content),
                    voteCount = target.voteupCount,
                    commentCount = target.commentCount,
                    voteState = voteState,
                    isFaved = null,
                    createdAt = target.created,
                    updatedAt = target.updated,
                    ipInfo = target.ipInfo ?: target.author?.ipInfo,
                    excerpt = target.excerpt,
                    coverUrl = target.imageUrl?.takeIf { it.isNotBlank() },
                )
            }
        }

        is PinTargetDto -> toPost(target)

        else -> null
    }
}

@Deprecated("Use PostMapper instead", ReplaceWith("PostMapper"))
typealias PinMapper = PostMapper

fun AnswerDto.toPost(): Post = PostMapper.toPost(this)

fun ArticleDto.toPost(): Post = PostMapper.toPost(this)

fun PinDto.toPost(): Post = PostMapper.toPost(this)

fun PinTargetDto.toPost(): Post? = PostMapper.toPost(this)

fun FeedTargetDto.toPost(): Post? = PostMapper.toPost(this)

fun AuthorDto.toAuthor(): Author = PostMapper.toAuthor(this)

fun FeedAuthorDto?.toAuthor(): Author = PostMapper.toAuthor(this)

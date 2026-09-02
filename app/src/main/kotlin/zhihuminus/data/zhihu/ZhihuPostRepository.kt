package com.zhihuminus.data.zhihu

import com.zhihuminus.core.content.AstParser.parseContent
import com.zhihuminus.data.Collection
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostLinkCard
import com.zhihuminus.feature.post.PostPoll
import com.zhihuminus.feature.post.PostPollOption
import com.zhihuminus.feature.post.PostRepository
import com.zhihuminus.feature.post.PostTopic
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.ui.booleanCompat
import com.zhihuminus.util.Log
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.coroutines.cancellation.CancellationException

class ZhihuPostRepository(
    private val api: ZhihuApi,
) : PostRepository {
    override suspend fun getPost(type: PostType, id: Long): Post = when (type) {
        PostType.Answer -> api.getAnswer(id).also { cachePost(type, id, it) }.let(::mapAnswer)
        PostType.Article -> api.getArticle(id).also { cachePost(type, id, it) }.let(::mapArticle)
        PostType.Pin -> api.getPin(id).also { cachePost(type, id, it) }.let(::mapPin)
    }

    override suspend fun getCachedPost(type: PostType, id: Long): Post? {
        val json = PostContentCache.get(type, id) ?: return null
        return try {
            when (type) {
                PostType.Answer -> mapAnswer(ZhihuJson.decodeJson<AnswerDto>(json))
                PostType.Article -> mapArticle(ZhihuJson.decodeJson<ArticleDto>(json))
                PostType.Pin -> mapPin(ZhihuJson.decodeJson<PinDto>(json))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ZhihuPostRepository", "Failed to decode cached post $type/$id", e)
            null
        }
    }

    /**
     * 网络详情写入缓存（详情数据保真度高于 feed 预热，直接覆盖）。
     * 编码/写入失败只影响缓存，不抛出。
     */
    private suspend inline fun <reified T : Any> cachePost(
        type: PostType,
        id: Long,
        dto: T,
    ) {
        try {
            val payload = ZhihuJson.json.encodeToJsonElement(dto).jsonObject
            PostContentCache.put(type, id, payload)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("ZhihuPostRepository", "Failed to cache post $type/$id", e)
        }
    }

    override suspend fun vote(postType: PostType, id: Long, vote: String): Int = when (postType) {
        PostType.Answer -> api.voteAnswer(id, vote)
        PostType.Article -> api.voteArticle(id, vote)
        PostType.Pin -> when (vote) {
            "up" -> api.likePin(id)
            "neutral" -> api.unlikePin(id)
            else -> throw UnsupportedOperationException("Pin does not support vote: $vote")
        }
    }

    override suspend fun submitPinPollVote(pollId: String, optionId: String) = api.submitPinPollVote(pollId, optionId)

    override suspend fun getCollections(postType: PostType, id: Long): List<Collection> {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val response = api.getCollections(type, id)
        return response.data
    }

    override suspend fun addToCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        api.addToCollection(type, id, collectionId)
    }

    override suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        api.removeFromCollection(type, id, collectionId)
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection =
        api.createCollection(title, description, isPublic)

    override suspend fun followMember(urlToken: String, follow: Boolean) {
        if (follow) api.followMember(urlToken) else api.unfollowMember(urlToken)
    }

    override suspend fun recordHistory(postType: PostType, id: Long) {
        val apiType = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val contentToken = id.toString()
        api.addHistory(contentToken, apiType)
        api.markAsRead(contentToken, apiType)
    }

    private fun mapAnswer(dto: AnswerDto): Post {
        val contentNodes = parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Answer,
            title = dto.question.title,
            author = dto.author.toAuthor(),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            isFaved = dto.reaction?.relation?.faved,
            createdAt = dto.createdTime,
            updatedAt = dto.updatedTime,
            ipInfo = dto.ipInfo,
            excerpt = dto.excerpt,
            questionId = dto.question.id,
        )
    }

    private fun mapArticle(dto: ArticleDto): Post {
        val contentNodes = parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Article,
            title = dto.title,
            author = dto.author.toAuthor(),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            isFaved = dto.reaction?.relation?.faved,
            createdAt = dto.created,
            updatedAt = dto.updated,
            ipInfo = dto.ipInfo,
            excerpt = dto.excerpt,
            topics = dto.topics.map { PostTopic(id = it.id, name = it.name) },
        )
    }

    private fun mapPin(dto: PinDto): Post {
        val html = dto.contentHtml
        val firstP = html.indexOf("<p>")
        val titleText = if (firstP > 0) {
            html
                .substring("<div>".length, firstP)
                .trimEnd(' ', '|')
                .takeIf { it.isNotBlank() }
        } else {
            null
        }
        val parsed = parseContent(if (titleText != null) html.removeRange(0, firstP) else html)
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
        val author = dto.author.toAuthor()
        return Post(
            id = dto.id.toLong(),
            type = PostType.Pin,
            title = titleText ?: "${author.name}的想法",
            author = author,
            content = parsed,
            voteCount = dto.likeCount,
            commentCount = dto.commentCount,
            voteState = if (isLiked) VoteUpState.Up else VoteUpState.Neutral,
            createdAt = dto.created,
            updatedAt = dto.updated,
            excerpt = dto.excerptTitle,
            topics = dto.topics?.map { PostTopic(id = it.id, name = it.name) }.orEmpty(),
            poll = poll,
            linkCards = linkCards,
        )
    }

    private fun AuthorDto.toAuthor(): Author = Author(
        id = id,
        name = name,
        headline = headline,
        avatarUrl = avatarUrl,
        urlToken = urlToken,
        badgeText = badgeV2?.detailBadges?.firstOrNull()?.description,
        isFollowing = isFollowing,
    )
}

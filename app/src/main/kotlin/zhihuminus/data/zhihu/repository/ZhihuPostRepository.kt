package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.core.content.AstParser.parseContent
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.booleanCompat
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.api.ZhihuCollectionApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.api.ZhihuPostApi
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostLinkCard
import com.zhihuminus.feature.post.PostPoll
import com.zhihuminus.feature.post.PostPollOption
import com.zhihuminus.feature.post.PostRepository
import com.zhihuminus.feature.post.PostTopic
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.VoteUpState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class ZhihuPostRepository(
    private val postApi: ZhihuPostApi,
    private val collectionApi: ZhihuCollectionApi,
    private val peopleApi: ZhihuPeopleApi,
    private val historyApi: ZhihuHistoryApi,
    private val postCache: PostContentCache = PostContentCache,
) : PostRepository {
    private val cacheScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun refreshCache(type: PostType, id: Long) {
        cacheScope.launch {
            try {
                getPost(type, id)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("ZhihuPostRepository", "Failed to refresh cache $type/$id", e)
            }
        }
    }

    override suspend fun getPost(type: PostType, id: Long): Post {
        val post = when (type) {
            PostType.Answer -> mapAnswer(postApi.getAnswer(id))
            PostType.Article -> mapArticle(postApi.getArticle(id))
            PostType.Pin -> mapPin(postApi.getPin(id))
        }
        postCache.put(post)
        return post
    }

    override suspend fun getCachedPost(type: PostType, id: Long): Post? =
        postCache.get(type, id)

    override suspend fun vote(postType: PostType, id: Long, vote: String): Int {
        val result = when (postType) {
            PostType.Answer -> postApi.voteAnswer(id, vote)
            PostType.Article -> postApi.voteArticle(id, vote)
            PostType.Pin -> when (vote) {
                "up" -> postApi.likePin(id)
                "neutral" -> postApi.unlikePin(id)
                else -> throw UnsupportedOperationException("Pin does not support vote: $vote")
            }
        }
        refreshCache(postType, id)
        return result
    }

    override suspend fun submitPinPollVote(pollId: String, optionId: String) = postApi.submitPinPollVote(pollId, optionId)

    override suspend fun getCollections(postType: PostType, id: Long): List<Collection> {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val response = collectionApi.getCollections(type, id)
        return response.data.map { it.toDomain() }
    }

    override suspend fun addToCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        collectionApi.addToCollection(type, id, collectionId)
        refreshCache(postType, id)
    }

    override suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        collectionApi.removeFromCollection(type, id, collectionId)
        refreshCache(postType, id)
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection =
        collectionApi.createCollection(title, description, isPublic).toDomain()

    override suspend fun followMember(urlToken: String, follow: Boolean) {
        if (follow) peopleApi.followMember(urlToken) else peopleApi.unfollowMember(urlToken)
    }

    override suspend fun recordHistory(postType: PostType, id: Long) {
        val apiType = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> "pin"
        }
        val contentToken = id.toString()
        historyApi.addHistory(contentToken, apiType)
        historyApi.markAsRead(contentToken, apiType)
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
            excerpt = dto.excerpt.ifEmpty { dto.excerptTitle },
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

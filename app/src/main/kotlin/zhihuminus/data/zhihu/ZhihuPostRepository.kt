package com.zhihuminus.data.zhihu

import com.zhihuminus.core.content.AstParser
import com.zhihuminus.data.Collection
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostRepository
import com.zhihuminus.feature.post.PostType

class ZhihuPostRepository(
    private val api: ZhihuApi,
) : PostRepository {
    override suspend fun getPost(type: PostType, id: Long): Post = when (type) {
        PostType.Answer -> mapAnswer(api.getAnswer(id))
        PostType.Article -> mapArticle(api.getArticle(id))
        PostType.Pin -> mapPin(api.getPin(id))
    }

    override suspend fun vote(postType: PostType, id: Long, vote: String): Int {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> throw UnsupportedOperationException("Pin does not support vote")
        }
        val response = api.vote(type, id, vote)
        return response.voteupCount
    }

    override suspend fun getCollections(postType: PostType, id: Long): List<Collection> {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> throw UnsupportedOperationException("Pin does not support collection")
        }
        val response = api.getCollections(type, id)
        return response.data
    }

    override suspend fun addToCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> throw UnsupportedOperationException("Pin does not support collection")
        }
        api.addToCollection(type, id, collectionId)
    }

    override suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String) {
        val type = when (postType) {
            PostType.Answer -> "answer"
            PostType.Article -> "article"
            PostType.Pin -> throw UnsupportedOperationException("Pin does not support collection")
        }
        api.removeFromCollection(type, id, collectionId)
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection = api.createCollection(title, description, isPublic)

    private fun mapAnswer(dto: AnswerDto): Post {
        val contentNodes = AstParser.parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Answer,
            title = dto.question.title,
            author = dto.author.toAuthor(),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            createdAt = dto.createdTime,
            updatedAt = dto.updatedTime,
            ipInfo = dto.ipInfo,
            excerpt = dto.excerpt,
            questionId = dto.question.id,
        )
    }

    private fun mapArticle(dto: ArticleDto): Post {
        val contentNodes = AstParser.parseContent(dto.content)
        return Post(
            id = dto.id,
            type = PostType.Article,
            title = dto.title,
            author = dto.author.toAuthor(),
            content = contentNodes,
            voteCount = dto.voteupCount,
            commentCount = dto.commentCount,
            voteState = VoteUpState.from(dto.reaction?.relation?.vote),
            createdAt = dto.created,
            updatedAt = dto.updated,
            ipInfo = dto.ipInfo,
            excerpt = dto.excerpt,
            topics = dto.topics.map { it.name },
        )
    }

    private fun mapPin(dto: PinDto): Post {
        val htmlContent = buildPinHtml(dto)
        val contentNodes = AstParser.parseContent(htmlContent)
        return Post(
            id = dto.id.toLong(),
            type = PostType.Pin,
            title = dto.excerptTitle,
            author = dto.author.toAuthor(),
            content = contentNodes,
            voteCount = dto.likeCount,
            commentCount = dto.commentCount,
            createdAt = dto.created,
            updatedAt = dto.updated,
            excerpt = dto.excerptTitle,
            topics = dto.topics?.map { it.name }.orEmpty(),
        )
    }

    private fun buildPinHtml(dto: PinDto): String = buildString {
        for (item in dto.content) {
            when (item.type) {
                "text" -> {
                    item.content?.let { append(it) }
                }
                "image" -> {
                    item.url?.let { url ->
                        append("<img src=\"$url\"")
                        item.width?.let { append(" width=\"$it\"") }
                        item.height?.let { append(" height=\"$it\"") }
                        append(" />")
                    }
                }
                "link_card" -> {
                    item.url?.let { url ->
                        val title = item.title ?: url
                        append("<a href=\"$url\">$title</a>")
                    }
                }
            }
        }
    }

    private fun AuthorDto.toAuthor(): Author = Author(
        id = id,
        name = name,
        headline = headline,
        avatarUrl = avatarUrl,
        urlToken = urlToken,
        badgeText = badgeV2?.detailBadges?.firstOrNull()?.description,
    )
}

package com.zhihuminus.data.zhihu

import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.CommentDto
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentAuthor
import com.zhihuminus.feature.comment.CommentPage
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentSortOrder
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.util.Log
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ZhihuCommentRepository(
    private val api: ZhihuApi,
) : CommentRepository {
    override suspend fun getRootComments(
        type: PostType,
        id: Long,
        orderBy: CommentSortOrder,
        offset: Int,
    ): CommentPage {
        val orderParam = when (orderBy) {
            CommentSortOrder.SCORE -> "score"
            CommentSortOrder.TIME -> "ts"
        }
        val contentType = type.toApiType()
        val json = api.getRootComments(contentType, id, orderParam, offset)
        return parseCommentPage(json)
    }

    override suspend fun getNextPage(nextUrl: String): CommentPage {
        val json = api.fetchCommentsPage(nextUrl)
        return parseCommentPage(json)
    }

    override suspend fun getChildComments(commentId: String, offset: Int): CommentPage {
        val json = api.getChildComments(commentId, offset)
        return parseCommentPage(json)
    }

    override suspend fun getComment(commentId: String): Comment {
        val json = api.getComment(commentId)
        val dto = ZhihuJson.decodeJson<CommentDto>(json)
        return dto.toDomain()
    }

    override suspend fun submitComment(
        type: PostType,
        id: Long,
        content: String,
        replyToCommentId: String?,
    ): Comment {
        val url = buildSubmitCommentUrl(type, id)
        val escapedContent = content.escapeHtml()
        val body = buildJsonObject {
            put("content", JsonPrimitive("<p>$escapedContent</p>"))
            replyToCommentId?.let { put("reply_comment_id", JsonPrimitive(it)) }
        }
        val json = api.submitComment(url, body)
        val dto = ZhihuJson.decodeJson<CommentDto>(json)
        return dto.toDomain()
    }

    override suspend fun likeComment(commentId: String) {
        val response = api.likeComment(commentId)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("点赞失败: ${response.status}")
        }
    }

    override suspend fun unlikeComment(commentId: String) {
        val response = api.unlikeComment(commentId)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("取消点赞失败: ${response.status}")
        }
    }

    override suspend fun deleteComment(commentId: String) {
        val response = api.deleteComment(commentId)
        if (!response.status.isSuccess()) {
            throw IllegalStateException("删除评论失败: ${response.status}")
        }
    }

    private fun parseCommentPage(json: JsonObject): CommentPage {
        val dataArray = json["data"] as? JsonArray
            ?: throw IllegalStateException("评论 API 响应缺少 data 数组: ${json.keys}")
        val paging = json["paging"] as? JsonObject
        val isEnd = paging?.get("is_end")?.jsonPrimitive?.boolean ?: true
        val nextUrl = paging?.get("next")?.jsonPrimitive?.contentOrNull

        val comments = dataArray.mapIndexedNotNull { index, element ->
            try {
                ZhihuJson.decodeJson<CommentDto>(element).toDomain()
            } catch (e: Exception) {
                Log.e("ZhihuCommentRepo", "Failed to decode comment at index $index", e)
                null
            }
        }

        return CommentPage(comments = comments, isEnd = isEnd, nextUrl = nextUrl)
    }

    private fun buildSubmitCommentUrl(type: PostType, id: Long): String {
        val path = when (type) {
            PostType.Answer -> "answers/$id"
            PostType.Article -> "articles/$id"
            PostType.Pin -> "pins/$id"
        }
        return "https://www.zhihu.com/api/v4/comment_v5/$path/comment"
    }

    private fun PostType.toApiType(): String = when (this) {
        PostType.Answer -> "answer"
        PostType.Article -> "article"
        PostType.Pin -> "pin"
    }

    private fun CommentDto.toDomain(): Comment = Comment(
        id = id,
        content = content,
        author = author.toDomain(),
        createdAt = createdTime,
        likeCount = likeCount,
        liked = liked,
        canDelete = canDelete,
        isAuthor = isAuthor,
        childCommentCount = childCommentCount,
        childComments = childComments.map { it.toDomain() },
        replyToAuthor = replyToAuthor?.toDomain(),
        commentTags = commentTag.map { it.text }.filter { it.isNotEmpty() },
        authorTag = authorTag
            .firstOrNull()
            ?.get("text")
            ?.jsonPrimitive
            ?.content,
    )

    private fun AuthorDto.toDomain(): CommentAuthor = CommentAuthor(
        id = id,
        name = name,
        avatarUrl = avatarUrl,
        urlToken = urlToken,
        headline = headline,
    )
}

private fun String.escapeHtml(): String =
    buildString(length) {
        for (char in this@escapeHtml) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

private fun buildJsonObject(block: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject =
    kotlinx.serialization.json.buildJsonObject(block)

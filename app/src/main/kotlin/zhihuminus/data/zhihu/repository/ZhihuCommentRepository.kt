package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuCommentApi
import com.zhihuminus.data.zhihu.mapper.toComment
import com.zhihuminus.data.zhihu.mapper.toCommentPage
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentContentType
import com.zhihuminus.feature.comment.CommentPage
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentSortOrder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class ZhihuCommentRepository(
    private val api: ZhihuCommentApi,
) : CommentRepository {
    override suspend fun getRootComments(
        type: CommentContentType,
        id: Long,
        orderBy: CommentSortOrder,
        offset: Int,
    ): CommentPage {
        val orderParam = when (orderBy) {
            CommentSortOrder.SCORE -> "score"
            CommentSortOrder.TIME -> "ts"
        }
        val contentType = type.toApiType()
        val page = api.getRootComments(contentType, id, orderParam, offset)
        return page.toCommentPage()
    }

    override suspend fun getNextPage(nextUrl: String): CommentPage {
        val page = api.fetchCommentsPage(nextUrl)
        return page.toCommentPage()
    }

    override suspend fun getChildComments(commentId: String, offset: Int): CommentPage {
        val page = api.getChildComments(commentId, offset)
        return page.toCommentPage()
    }

    override suspend fun getComment(commentId: String): Comment {
        val dto = api.getComment(commentId)
        return dto.toComment()
    }

    override suspend fun submitComment(
        type: CommentContentType,
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
        val dto = api.submitComment(url, body)
        return dto.toComment()
    }

    override suspend fun likeComment(commentId: String) {
        api.likeComment(commentId)
    }

    override suspend fun unlikeComment(commentId: String) {
        api.unlikeComment(commentId)
    }

    override suspend fun deleteComment(commentId: String) {
        api.deleteComment(commentId)
    }

    private fun buildSubmitCommentUrl(type: CommentContentType, id: Long): String {
        val path = when (type) {
            CommentContentType.Answer -> "answers/$id"
            CommentContentType.Article -> "articles/$id"
            CommentContentType.Pin -> "pins/$id"
            CommentContentType.Question -> "questions/$id"
        }
        return "https://www.zhihu.com/api/v4/comment_v5/$path/comment"
    }

    private fun CommentContentType.toApiType(): String = when (this) {
        CommentContentType.Answer -> "answer"
        CommentContentType.Article -> "article"
        CommentContentType.Pin -> "pin"
        CommentContentType.Question -> "question"
    }
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

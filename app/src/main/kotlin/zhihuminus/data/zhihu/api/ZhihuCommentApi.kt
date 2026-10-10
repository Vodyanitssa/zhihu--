package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.CommentDto
import com.zhihuminus.data.zhihu.dto.CommentsPageDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

open class ZhihuCommentApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 按完整 URL 拉取评论分页数据
     */
    open suspend fun fetchCommentsPage(url: String): CommentsPageDto {
        val json = environment.fetchJson(url, "data[*].content,excerpt,headline,target.author.badge_v2")
            ?: throw IllegalStateException("Failed to fetch comments page")
        return decodeCommentsPage(json)
    }

    /**
     * 获取根评论列表
     */
    open suspend fun getRootComments(
        contentType: String,
        contentId: Long,
        orderBy: String,
        offset: Int,
        limit: Int = 20,
    ): CommentsPageDto {
        val url = "https://www.zhihu.com/api/v4/comment_v5/${contentType}s/$contentId/root_comment" +
            "?order_by=$orderBy" + if (offset > 0) "&offset=$offset&limit=$limit" else ""
        val json = environment.fetchJson(url, "data[*].content,excerpt,headline,target.author.badge_v2")
            ?: throw IllegalStateException("Failed to fetch root comments")
        return decodeCommentsPage(json)
    }

    /**
     * 获取子评论列表
     */
    open suspend fun getChildComments(
        commentId: String,
        offset: Int,
        limit: Int = 20,
    ): CommentsPageDto {
        val url = "https://www.zhihu.com/api/v4/comment_v5/comment/$commentId/child_comment" +
            "?offset=$offset&limit=$limit"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch child comments")
        return decodeCommentsPage(json)
    }

    /**
     * 获取单条评论详情
     */
    open suspend fun getComment(commentId: String): CommentDto {
        val url = "https://www.zhihu.com/api/v4/comment_v5/comment/$commentId"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch comment $commentId")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 发表评论
     */
    open suspend fun submitComment(url: String, body: JsonObject): CommentDto {
        val response = environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        response.raiseForStatus()
        return response.body()
    }

    /**
     * 点赞评论
     */
    open suspend fun likeComment(commentId: String) {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/comments/$commentId/like")
        response.raiseForStatus()
    }

    /**
     * 取消点赞评论
     */
    open suspend fun unlikeComment(commentId: String) {
        val response = environment.deleteSigned("https://www.zhihu.com/api/v4/comments/$commentId/like")
        response.raiseForStatus()
    }

    /**
     * 删除评论
     */
    open suspend fun deleteComment(commentId: String) {
        val response = environment.deleteSigned("https://www.zhihu.com/api/v4/comment_v5/comment/$commentId")
        response.raiseForStatus()
    }

    private fun decodeCommentsPage(json: JsonObject): CommentsPageDto {
        val dataArray = json["data"] as? JsonArray ?: JsonArray(emptyList())
        val comments = dataArray.mapIndexedNotNull { index, element ->
            try {
                ZhihuJson.decodeJson<CommentDto>(element)
            } catch (e: Exception) {
                Log.e("ZhihuCommentApi", "Failed to decode comment at index $index", e)
                null
            }
        }
        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }
        return CommentsPageDto(data = comments, paging = paging)
    }
}

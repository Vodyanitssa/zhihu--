package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.CommentDto
import com.zhihuminus.data.zhihu.dto.CommentsPageDto
import kotlinx.serialization.json.JsonObject

interface ZhihuCommentApi {
    /**
     * 通用评论分页请求（用于 paging.next URL）
     * @param url 完整的评论 API URL
     */
    suspend fun fetchCommentsPage(url: String): CommentsPageDto

    /**
     * 获取根评论列表
     * @param contentType 内容类型: "answers", "articles", "pins"
     * @param contentId 内容 ID
     * @param orderBy 排序: "score" 或 "ts"
     * @param offset 偏移量
     * @param limit 每页数量
     */
    suspend fun getRootComments(
        contentType: String,
        contentId: Long,
        orderBy: String,
        offset: Int,
        limit: Int = 20,
    ): CommentsPageDto

    /**
     * 获取子评论列表
     * @param commentId 父评论 ID
     * @param offset 偏移量
     * @param limit 每页数量
     */
    suspend fun getChildComments(
        commentId: String,
        offset: Int,
        limit: Int = 20,
    ): CommentsPageDto

    /**
     * 获取单条评论详情
     * @param commentId 评论 ID
     */
    suspend fun getComment(commentId: String): CommentDto

    /**
     * 发表评论
     * @param url 评论提交 URL
     * @param body 请求体
     * @return 新评论的 DTO
     */
    suspend fun submitComment(url: String, body: JsonObject): CommentDto

    /**
     * 点赞评论
     */
    suspend fun likeComment(commentId: String)

    /**
     * 取消点赞
     */
    suspend fun unlikeComment(commentId: String)

    /**
     * 删除评论
     */
    suspend fun deleteComment(commentId: String)
}

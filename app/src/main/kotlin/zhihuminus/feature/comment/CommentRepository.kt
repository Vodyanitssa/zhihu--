package com.zhihuminus.feature.comment

import com.zhihuminus.feature.post.PostType

/**
 * 评论排序方式
 */
enum class CommentSortOrder {
    SCORE, // 按热度
    TIME, // 按时间
}

/**
 * 评论数据仓库接口（feature 层定义契约，data 层实现）
 */
interface CommentRepository {
    /**
     * 获取根评论列表
     */
    suspend fun getRootComments(
        type: PostType,
        id: Long,
        orderBy: CommentSortOrder,
        offset: Int,
    ): CommentPage

    /**
     * 获取下一页评论（使用 paging.next URL）
     */
    suspend fun getNextPage(nextUrl: String): CommentPage

    /**
     * 获取子评论列表
     */
    suspend fun getChildComments(commentId: String, offset: Int): CommentPage

    /**
     * 获取单条评论（用于深链锚点解析）
     */
    suspend fun getComment(commentId: String): Comment

    /**
     * 发表评论
     */
    suspend fun submitComment(
        type: PostType,
        id: Long,
        content: String,
        replyToCommentId: String?,
    ): Comment

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

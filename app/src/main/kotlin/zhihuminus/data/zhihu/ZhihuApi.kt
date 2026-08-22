package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.VoteResponse
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonObject

interface ZhihuApi {
    suspend fun getAnswer(answerId: Long): AnswerDto

    suspend fun getArticle(articleId: Long): ArticleDto

    suspend fun getPin(pinId: Long): PinDto

    /**
     * 投票
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票响应
     */
    suspend fun vote(type: String, id: Long, vote: String): VoteResponse

    /**
     * 获取收藏夹列表
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @return 收藏夹列表响应
     */
    suspend fun getCollections(type: String, id: Long): CollectionResponse

    /**
     * 收藏内容到指定收藏夹
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun addToCollection(type: String, id: Long, collectionId: String)

    /**
     * 取消收藏
     * @param type 内容类型: "answer" 或 "article"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    suspend fun removeFromCollection(type: String, id: Long, collectionId: String)

    /**
     * 创建新收藏夹
     * @param title 收藏夹标题
     * @param description 描述
     * @param isPublic 是否公开
     * @return 新创建的收藏夹
     */
    suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection

    // 评论相关

    /**
     * 通用评论分页请求（用于 paging.next URL）
     * @param url 完整的评论 API URL
     */
    suspend fun fetchCommentsPage(url: String): JsonObject

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
    ): JsonObject

    /**
     * 获取子评论列表
     * @param commentId 父评论 ID
     * @param offset 偏移量
     * @param limit 每页数量
     */
    suspend fun getChildComments(commentId: String, offset: Int, limit: Int = 20): JsonObject

    /**
     * 获取单条评论详情
     * @param commentId 评论 ID
     */
    suspend fun getComment(commentId: String): JsonObject

    /**
     * 发表评论
     * @param url 评论提交 URL
     * @param body 请求体
     * @return 新评论的 JSON
     */
    suspend fun submitComment(url: String, body: JsonObject): JsonObject

    /**
     * 点赞评论
     */
    suspend fun likeComment(commentId: String): HttpResponse

    /**
     * 取消点赞
     */
    suspend fun unlikeComment(commentId: String): HttpResponse

    /**
     * 删除评论
     */
    suspend fun deleteComment(commentId: String): HttpResponse
}

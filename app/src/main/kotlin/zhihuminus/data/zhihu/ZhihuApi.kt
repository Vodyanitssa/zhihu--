package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonObject

interface ZhihuApi {
    suspend fun getAnswer(answerId: Long): AnswerDto

    /**
     * 获取问题详情（标题、描述、统计、关注状态、话题）
     */
    suspend fun getQuestion(questionId: Long): QuestionDto

    /**
     * 按完整 URL 拉取一页 feed 条目（问题回答流等），并解析续页游标。
     * @param include feed 字段 include 表达式，空串表示不传（桌面推荐流默认返回全量字段）
     */
    suspend fun fetchFeedPage(
        url: String,
        include: String = FEED_INCLUDE,
    ): FeedPage

    /**
     * 关注/取消关注问题
     */
    suspend fun followQuestion(questionId: Long, follow: Boolean)

    suspend fun getArticle(articleId: Long): ArticleDto

    suspend fun getPin(pinId: Long): PinDto

    /**
     * Pin 点赞
     * @param pinId Pin ID
     * @return 点赞后的赞数
     */
    suspend fun likePin(pinId: Long): Int

    /**
     * Pin 取消点赞
     * @param pinId Pin ID
     * @return 取消点赞后的赞数
     */
    suspend fun unlikePin(pinId: Long): Int

    /**
     * Pin 投票
     * @param pollId 投票 ID
     * @param optionId 选项 ID
     */
    suspend fun submitPinPollVote(pollId: String, optionId: String)

    /**
     * 加载赞同者列表
     * @param url 赞同者 API URL
     * @return 原始 JSON 响应（包含 data 和 paging）
     */
    suspend fun fetchVoters(url: String): JsonObject

    /**
     * 关注用户
     * @param urlToken 用户 urlToken
     */
    suspend fun followMember(urlToken: String)

    /**
     * 取消关注用户
     * @param urlToken 用户 urlToken
     */
    suspend fun unfollowMember(urlToken: String)

    /**
     * 回答投票
     * @param answerId 回答 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票后的赞同数
     */
    suspend fun voteAnswer(answerId: Long, vote: String): Int

    /**
     * 文章投票
     * @param articleId 文章 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票后的赞同数
     */
    suspend fun voteArticle(articleId: Long, vote: String): Int

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

    /**
     * 记录阅读历史（read_history/add）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin"
     */
    suspend fun addHistory(contentToken: String, contentType: String)

    /**
     * 标记已读（lastread/touch）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin"
     */
    suspend fun markAsRead(contentToken: String, contentType: String)

    /**
     * 获取专栏文章列表
     * @param columnId 专栏 ID
     * @param nextUrl 分页续页 URL（为 null 时从第一页开始）
     */
    suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage
}

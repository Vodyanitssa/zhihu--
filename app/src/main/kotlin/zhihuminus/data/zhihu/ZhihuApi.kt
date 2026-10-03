package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemsPageDto
import com.zhihuminus.data.zhihu.dto.CollectionResponseDto
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryPage
import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationOverviewDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePageDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.data.zhihu.dto.ZhihuMeNotificationsDto
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
    suspend fun getCollections(type: String, id: Long): CollectionResponseDto

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
    suspend fun createCollection(title: String, description: String, isPublic: Boolean): CollectionDto

    /**
     * 获取用户收藏夹列表
     * @param urlToken 用户 urlToken
     * @param nextUrl 分页续页 URL
     */
    suspend fun getUserCollections(urlToken: String, nextUrl: String? = null): CollectionResponseDto

    /**
     * 删除收藏夹
     * @param collectionId 收藏夹 ID
     */
    suspend fun deleteCollection(collectionId: String): Boolean

    /**
     * 获取指定收藏夹详情
     * @param collectionId 收藏夹 ID
     */
    suspend fun getCollection(collectionId: String): CollectionDto

    /**
     * 获取收藏夹内容条目
     * @param collectionId 收藏夹 ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param nextUrl 分页续页 URL
     */
    suspend fun getCollectionItems(
        collectionId: String,
        offset: Int? = null,
        limit: Int = 20,
        nextUrl: String? = null,
    ): CollectionItemsPageDto

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
     * 获取一页在线浏览历史记录。
     * @param url 分页 URL（首页或续页）
     */
    suspend fun fetchHistoryPage(url: String): HistoryPage

    /**
     * 批量删除在线浏览历史记录。
     * @param pairs 要删除的记录标识列表
     */
    suspend fun deleteHistoryItems(pairs: List<HistoryDeletePairDto>)

    /**
     * 清空全部在线浏览历史记录。
     */
    suspend fun clearHistory()

    /**
     * 获取专栏文章列表
     * @param columnId 专栏 ID
     * @param nextUrl 分页续页 URL（为 null 时从第一页开始）
     */
    suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage

    // 知乎日报相关

    /**
     * 获取最新一期知乎日报
     */
    suspend fun getDailyLatest(): DailyStoriesResponse

    /**
     * 获取指定日期之前的知乎日报
     * @param date 日期字符串，格式 yyyyMMdd
     */
    suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse

    /**
     * 获取话题详情
     * @param topicId 话题 ID
     */
    suspend fun getTopicDetail(topicId: String): JsonObject

    /**
     * 获取话题 feed 列表（讨论/想法/待回答）
     * @param url 完整的 API URL
     * @param include feed 字段 include 表达式
     */
    suspend fun getTopicFeed(url: String, include: String): JsonObject

    /**
     * 关注/取消关注话题
     * @param topicId 话题 ID
     * @param follow true 关注，false 取消关注
     */
    suspend fun followTopic(topicId: String, follow: Boolean)

    // 通知相关

    /**
     * 获取通知中心概览（分类未读数、邀请卡片、会话列表）
     * @param nextUrl 分页续页 URL，为 null 时从首页加载
     */
    suspend fun getNotificationOverview(nextUrl: String? = null): NotificationOverviewDto

    /**
     * 获取指定分类的时间线通知列表
     * @param entryName 分类标识（comment, like, favlist_me, follow, invite 等）
     * @param nextUrl 分页续页 URL，为 null 时从首页加载
     */
    suspend fun getNotificationTimeline(entryName: String, nextUrl: String? = null): NotificationOverviewDto

    /**
     * 将指定分类的通知全部标记为已读
     * @param entryName 分类标识
     */
    suspend fun markNotificationCategoryRead(entryName: String): Boolean

    // 私信相关

    /**
     * 获取与特定用户的私信会话记录
     * @param peerId 对方用户 ID
     * @param nextUrl 分页续页 URL
     */
    suspend fun getPrivateMessages(peerId: String, nextUrl: String? = null): PrivateMessagePageDto

    /**
     * 获取私信对话方的用户信息
     * @param peerId 对方用户 ID
     */
    suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthorDto

    /**
     * 发送私信
     * @param peerId 接收方用户 ID
     * @param content 私信内容
     */
    suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessageDto

    /**
     * 获取个人未读通知数汇总（用于首页红点）
     */
    suspend fun getMeNotifications(): ZhihuMeNotificationsDto
}

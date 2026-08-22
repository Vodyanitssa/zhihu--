package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.VoteResponse

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
}

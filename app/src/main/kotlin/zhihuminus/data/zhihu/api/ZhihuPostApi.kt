package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import kotlinx.serialization.json.JsonObject

interface ZhihuPostApi {
    suspend fun getAnswer(answerId: Long): AnswerDto

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
}

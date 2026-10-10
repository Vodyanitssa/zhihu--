package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.TopicDetailDto
import com.zhihuminus.data.zhihu.dto.TopicFeedResponseDto

interface ZhihuTopicApi {
    /**
     * 获取话题详情
     * @param topicId 话题 ID
     */
    suspend fun getTopicDetail(topicId: String): TopicDetailDto

    /**
     * 获取话题 feed 列表（讨论/想法/待回答）
     * @param url 完整的 API URL
     * @param include feed 字段 include 表达式
     */
    suspend fun getTopicFeed(url: String, include: String): TopicFeedResponseDto

    /**
     * 关注/取消关注话题
     * @param topicId 话题 ID
     * @param follow true 关注，false 取消关注
     */
    suspend fun followTopic(topicId: String, follow: Boolean)
}

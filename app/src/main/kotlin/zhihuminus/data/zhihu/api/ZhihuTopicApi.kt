package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.TopicDetailDto
import com.zhihuminus.data.zhihu.dto.TopicFeedResponseDto

open class ZhihuTopicApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 获取话题详情
     */
    open suspend fun getTopicDetail(topicId: String): TopicDetailDto {
        val url = "https://www.zhihu.com/api/v5.1/topics/$topicId"
        val include = "name,excerpt,avatar_url,followers_count,questions_count,is_following,topic_id,total_pv,discuss_count"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("话题详情响应为空")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 加载话题 Feed 流（讨论/想法/未回答）
     */
    open suspend fun getTopicFeed(url: String, include: String): TopicFeedResponseDto {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://", "https://"), include)
            ?: throw IllegalStateException("话题内容响应为空")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 关注/取消关注话题
     */
    open suspend fun followTopic(topicId: String, follow: Boolean) {
        val url = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
        val response = if (follow) {
            environment.postSigned(url)
        } else {
            environment.deleteSigned(url)
        }
        response.raiseForStatus()
    }
}

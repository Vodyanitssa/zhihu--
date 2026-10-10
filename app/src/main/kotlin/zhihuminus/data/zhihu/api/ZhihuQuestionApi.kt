package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.QuestionDto

open class ZhihuQuestionApi(
    protected val environment: ZhihuApiEnvironment,
) {
    open suspend fun getQuestion(questionId: Long): QuestionDto {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId"
        val include =
            "read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch question $questionId")
        return ZhihuJson.decodeJson(json)
    }

    open suspend fun followQuestion(questionId: Long, follow: Boolean) {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
        if (follow) {
            environment.postSigned(url)
        } else {
            environment.deleteSigned(url)
        }
    }
}

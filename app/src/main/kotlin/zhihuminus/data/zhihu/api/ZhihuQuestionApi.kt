package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.QuestionDto

interface ZhihuQuestionApi {
    /**
     * 获取问题详情（标题、描述、统计、关注状态、话题）
     */
    suspend fun getQuestion(questionId: Long): QuestionDto

    /**
     * 关注/取消关注问题
     */
    suspend fun followQuestion(questionId: Long, follow: Boolean)
}

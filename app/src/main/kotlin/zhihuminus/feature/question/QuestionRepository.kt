package com.zhihuminus.feature.question

/**
 * 问题页数据仓库。所有网络访问统一经由 `data/zhihu` 层完成。
 */
interface QuestionRepository {
    suspend fun getQuestion(questionId: Long): QuestionDetail

    /** 记录服务端阅读历史（read_history/add）。 */
    suspend fun recordRead(questionId: Long)

    /** 拉取回答流第一页。 */
    suspend fun loadAnswers(
        questionId: Long,
        sort: QuestionSort,
    ): QuestionAnswersPage

    /** 按 [QuestionAnswersPage.nextUrl] 拉取后续页。 */
    suspend fun loadAnswers(nextUrl: String): QuestionAnswersPage

    suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    )
}

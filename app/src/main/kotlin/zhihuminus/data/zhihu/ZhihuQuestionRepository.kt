package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.target
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.feature.question.QuestionAnswersPage
import com.zhihuminus.feature.question.QuestionDetail
import com.zhihuminus.feature.question.QuestionRepository
import com.zhihuminus.feature.question.QuestionSort
import com.zhihuminus.feature.question.QuestionTopic

class ZhihuQuestionRepository(
    private val api: ZhihuApi,
) : QuestionRepository {
    override suspend fun getQuestion(questionId: Long): QuestionDetail = api.getQuestion(questionId).toDomain()

    override suspend fun recordRead(questionId: Long) {
        api.addHistory(contentToken = questionId.toString(), contentType = "question")
    }

    override suspend fun loadAnswers(
        questionId: Long,
        sort: QuestionSort,
    ): QuestionAnswersPage = api.fetchFeedPage(questionFeedsUrl(questionId, sort.apiValue)).toAnswersPage()

    override suspend fun loadAnswers(nextUrl: String): QuestionAnswersPage = api.fetchFeedPage(nextUrl).toAnswersPage()

    override suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    ) {
        api.followQuestion(questionId, follow)
    }

    private fun QuestionDto.toDomain(): QuestionDetail =
        QuestionDetail(
            title = title,
            detailHtml = detail,
            excerpt = excerpt,
            visitCount = visitCount,
            commentCount = commentCount,
            followerCount = followerCount,
            answerCount = answerCount,
            voteupCount = voteupCount,
            isFollowing = relationship.isFollowing,
            topics = topics.map { QuestionTopic(id = it.id, name = it.name) },
        )

    private fun FeedPage.toAnswersPage(): QuestionAnswersPage =
        QuestionAnswersPage(
            items = items.map { it.toAnswerDisplayItem() },
            nextUrl = nextUrl,
            isEnd = isEnd,
        )

    private fun Feed.toAnswerDisplayItem(): FeedDisplayItem {
        val target = this@toAnswerDisplayItem.target
        if (target is Feed.AnswerTarget) {
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                summary = target.excerpt,
                details = target.detailsText,
                feed = this@toAnswerDisplayItem,
                title = "",
            )
        }
        return toDisplayItem()
    }
}

private fun questionFeedsUrl(
    questionId: Long,
    order: String?,
): String =
    buildString {
        append("https://www.zhihu.com/api/v4/questions/")
        append(questionId)
        append("/feeds?limit=20")
        if (!order.isNullOrEmpty()) {
            append("&order=")
            append(order)
        }
    }

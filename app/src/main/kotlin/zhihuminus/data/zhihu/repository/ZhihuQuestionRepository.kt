package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuQuestionApi
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.feature.question.QuestionAnswersPage
import com.zhihuminus.feature.question.QuestionDetail
import com.zhihuminus.feature.question.QuestionRepository
import com.zhihuminus.feature.question.QuestionSort
import com.zhihuminus.feature.question.QuestionTopic

class ZhihuQuestionRepository(
    private val questionApi: ZhihuQuestionApi,
    private val historyApi: ZhihuHistoryApi,
    private val feedApi: ZhihuFeedApi,
    private val postCache: PostContentCache = PostContentCache,
) : QuestionRepository {
    override suspend fun getQuestion(questionId: Long): QuestionDetail = questionApi.getQuestion(questionId).toDomain()

    override suspend fun recordRead(questionId: Long) {
        historyApi.addHistory(contentToken = questionId.toString(), contentType = "question")
    }

    override suspend fun loadAnswers(
        questionId: Long,
        sort: QuestionSort,
    ): QuestionAnswersPage = feedApi.fetchFeedPage(questionFeedsUrl(questionId, sort.apiValue)).toAnswersPage()

    override suspend fun loadAnswers(nextUrl: String): QuestionAnswersPage = feedApi.fetchFeedPage(nextUrl).toAnswersPage()

    override suspend fun followQuestion(
        questionId: Long,
        follow: Boolean,
    ) {
        questionApi.followQuestion(questionId, follow)
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

    private suspend fun FeedPage.toAnswersPage(): QuestionAnswersPage {
        items.forEach { it.target?.let { target -> postCache.putFromFeed(target) } }
        return QuestionAnswersPage(
            items = items.map { it.toAnswerDisplayItem() },
            nextUrl = nextUrl,
            isEnd = isEnd,
        )
    }

    private fun FeedDto.toAnswerDisplayItem(): FeedDisplayItem {
        val target = this@toAnswerDisplayItem.target
        if (target is AnswerTargetDto) {
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                authorBadge = target.author?.badgeV2?.toOfficialBadge(),
                summary = target.excerpt,
                details = target.detailsText,
                feed = this@toAnswerDisplayItem,
                title = "",
                contentTypeLabel = "回答",
                publishTimeSeconds = target.createdTime.takeIf { it > 0 },
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

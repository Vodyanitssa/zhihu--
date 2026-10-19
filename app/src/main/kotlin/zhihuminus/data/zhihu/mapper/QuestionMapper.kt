package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.feature.question.QuestionDetail
import com.zhihuminus.feature.question.QuestionTopic

/**
 * 知乎问题数据模型映射器。
 */
object QuestionMapper {
    fun toQuestionDetail(dto: QuestionDto): QuestionDetail = QuestionDetail(
        title = dto.title,
        detailHtml = dto.detail,
        excerpt = dto.excerpt,
        visitCount = dto.visitCount,
        commentCount = dto.commentCount,
        followerCount = dto.followerCount,
        answerCount = dto.answerCount,
        voteupCount = dto.voteupCount,
        isFollowing = dto.relationship.isFollowing,
        topics = dto.topics.map { QuestionTopic(id = it.id, name = it.name) },
    )

    fun toAnswerDisplayItem(feed: FeedDto): FeedDisplayItem {
        val target = feed.target
        if (target is AnswerTargetDto) {
            val (images, layout) = target.resolveFeedImages()
            return FeedDisplayItem(
                authorName = target.author?.name ?: "未知作者",
                avatarSrc = target.author?.avatarUrl,
                authorBadge = target.author?.badgeV2?.toOfficialBadge(),
                summary = target.excerpt,
                details = target.detailsText,
                feed = feed,
                title = "",
                contentTypeLabel = "回答",
                publishTimeSeconds = target.createdTime.takeIf { it > 0 },
                images = images,
                imageLayout = layout,
            )
        }
        return feed.toDisplayItem()
    }
}

fun QuestionDto.toQuestionDetail(): QuestionDetail = QuestionMapper.toQuestionDetail(this)

fun FeedDto.toAnswerDisplayItem(): FeedDisplayItem = QuestionMapper.toAnswerDisplayItem(this)

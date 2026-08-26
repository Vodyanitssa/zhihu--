package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnswerDto(
    val id: Long,
    val content: String,
    val excerpt: String = "",
    val voteupCount: Int = 0,
    val commentCount: Int = 0,
    val createdTime: Long = 0,
    val updatedTime: Long = 0,
    val author: AuthorDto,
    val question: QuestionDto,
    val ipInfo: String? = null,
    val reaction: ReactionDto? = null,
    val segmentInfos: List<SegmentInfoDto> = emptyList(),
)

/**
 * 问题详情。嵌套在 [AnswerDto.question] 中时只有 id/title 有值；
 * 作为 `/api/v4/questions/{id}` 的响应解码时包含完整字段。
 */
@Serializable
data class QuestionDto(
    val id: Long = 0,
    val title: String = "",
    val detail: String = "",
    val excerpt: String = "",
    val answerCount: Int = 0,
    val visitCount: Int = 0,
    val commentCount: Int = 0,
    val followerCount: Int = 0,
    val voteupCount: Int = 0,
    val relationship: QuestionRelationshipDto = QuestionRelationshipDto(),
    val topics: List<QuestionTopicDto> = emptyList(),
)

@Serializable
data class QuestionRelationshipDto(
    val isFollowing: Boolean = false,
)

@Serializable
data class QuestionTopicDto(
    val id: String = "",
    val name: String = "",
)

@Serializable
data class ReactionDto(
    val relation: RelationDto? = null,
)

@Serializable
data class RelationDto(
    val vote: String = "Neutral",
)

@Serializable
data class SegmentInfoDto(
    val paragraphId: String = "",
    val startOffset: Int = 0,
    val endOffset: Int = 0,
    val segmentContent: String = "",
    val segmentId: String = "",
)

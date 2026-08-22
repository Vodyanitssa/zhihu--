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

@Serializable
data class QuestionDto(
    val id: Long,
    val title: String,
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

package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val id: String,
    val content: String,
    val createdTime: Long = 0,
    val likeCount: Int = 0,
    val author: AuthorDto,
    val childCommentCount: Int = 0,
    val childComments: List<CommentDto> = emptyList(),
)

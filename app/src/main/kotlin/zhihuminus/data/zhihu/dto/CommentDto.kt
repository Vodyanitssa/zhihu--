package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class CommentDto(
    val id: String,
    val type: String = "",
    val resourceType: String = "",
    val url: String = "",
    val content: String,
    val createdTime: Long = 0,
    val isDelete: Boolean = false,
    val collapsed: Boolean = false,
    val reviewing: Boolean = false,
    val replyCommentId: String? = null,
    val replyRootCommentId: String? = null,
    val liked: Boolean = false,
    val likeCount: Int = 0,
    val isAuthor: Boolean = false,
    val canDelete: Boolean = false,
    val isAuthorTop: Boolean = false,
    val author: AuthorDto,
    val replyToAuthor: AuthorDto? = null,
    val authorTag: List<JsonObject> = emptyList(),
    val commentTag: List<CommentTagDto> = emptyList(),
    val childCommentCount: Int = 0,
    val childComments: List<CommentDto> = emptyList(),
)

@Serializable
data class CommentTagDto(
    val type: String = "",
    val text: String = "",
    val color: String = "",
    val nightColor: String = "",
    val hasBorder: Boolean = false,
)

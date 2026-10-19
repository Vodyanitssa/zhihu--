package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.zhihu.dto.AuthorDto
import com.zhihuminus.data.zhihu.dto.CommentDto
import com.zhihuminus.data.zhihu.dto.CommentsPageDto
import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentAuthor
import com.zhihuminus.feature.comment.CommentPage
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

/**
 * 知乎评论及评论分页数据模型映射器。
 */
object CommentMapper {
    fun toCommentPage(dto: CommentsPageDto): CommentPage = CommentPage(
        comments = dto.data.map { toComment(it) },
        isEnd = dto.paging?.hasMore != true,
        nextUrl = dto.paging?.nextUrl,
    )

    fun toComment(dto: CommentDto): Comment = Comment(
        id = dto.id,
        content = dto.content,
        author = toCommentAuthor(dto.author),
        createdAt = dto.createdTime,
        likeCount = dto.likeCount,
        liked = dto.liked,
        canDelete = dto.canDelete,
        isAuthor = dto.isAuthor,
        childCommentCount = dto.childCommentCount,
        childComments = dto.childComments.map { toComment(it) },
        replyToAuthor = dto.replyToAuthor?.let { toCommentAuthor(it) },
        commentTags = dto.commentTag.map { it.text }.filter { it.isNotEmpty() },
        authorTag = dto.authorTag
            .firstOrNull()
            ?.get("text")
            ?.jsonPrimitive
            ?.contentOrNull,
        replyRootCommentId = dto.replyRootCommentId,
    )

    fun toCommentAuthor(dto: AuthorDto): CommentAuthor = CommentAuthor(
        id = dto.id,
        name = dto.name,
        avatarUrl = dto.avatarUrl,
        urlToken = dto.urlToken,
        headline = dto.headline,
    )
}

fun CommentsPageDto.toCommentPage(): CommentPage = CommentMapper.toCommentPage(this)

fun CommentDto.toComment(): Comment = CommentMapper.toComment(this)

fun AuthorDto.toCommentAuthor(): CommentAuthor = CommentMapper.toCommentAuthor(this)

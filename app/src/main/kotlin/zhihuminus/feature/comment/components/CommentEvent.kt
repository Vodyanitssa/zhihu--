package com.zhihuminus.feature.comment.components

import com.zhihuminus.feature.comment.Comment
import com.zhihuminus.feature.comment.CommentSortOrder

sealed interface CommentEvent {
    data object LoadMore : CommentEvent

    data object Refresh : CommentEvent

    data class ChangeSortOrder(
        val order: CommentSortOrder,
    ) : CommentEvent

    data class SubmitComment(
        val text: String,
        val replyToCommentId: String? = null,
    ) : CommentEvent

    data class LikeComment(
        val commentId: String,
    ) : CommentEvent

    data class UnlikeComment(
        val commentId: String,
    ) : CommentEvent

    data class DeleteComment(
        val commentId: String,
    ) : CommentEvent

    data class OpenChildComments(
        val comment: Comment,
    ) : CommentEvent

    data object DismissChildComments : CommentEvent

    data class OpenImage(
        val url: String,
    ) : CommentEvent

    data class OpenLink(
        val url: String,
    ) : CommentEvent
}

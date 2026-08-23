package com.zhihuminus.feature.comment

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

    data class Reply(
        val comment: Comment,
    ) : CommentEvent

    data object DismissReply : CommentEvent
}

sealed interface CommentEffect {
    data class ShowMessage(
        val message: String,
    ) : CommentEffect

    data class OpenImage(
        val url: String,
    ) : CommentEffect

    data class OpenExternalUrl(
        val url: String,
    ) : CommentEffect

    data object ScrollToTop : CommentEffect
}

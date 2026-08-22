package com.zhihuminus.feature.comment

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

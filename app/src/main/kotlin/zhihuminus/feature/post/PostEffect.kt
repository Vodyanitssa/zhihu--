package com.zhihuminus.feature.post

sealed interface PostEffect {
    data class ShareText(
        val text: String,
    ) : PostEffect

    data class CopyLink(
        val link: String,
    ) : PostEffect

    data class ShowMessage(
        val message: String,
    ) : PostEffect

    data class OpenExternalUrl(
        val url: String,
    ) : PostEffect

    data class OpenImage(
        val url: String,
    ) : PostEffect
}

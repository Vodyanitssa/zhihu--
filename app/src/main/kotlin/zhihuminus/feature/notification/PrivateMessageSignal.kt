package com.zhihuminus.feature.notification

data class PrivateMessageUiState(
    val peerId: String = "",
    val peer: NotificationAuthor? = null,
    val messages: List<PrivateMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSending: Boolean = false,
    val isEnd: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface PrivateMessageEvent {
    data object Refresh : PrivateMessageEvent

    data object LoadMore : PrivateMessageEvent

    data class SendMessage(
        val content: String,
    ) : PrivateMessageEvent
}

sealed interface PrivateMessageEffect {
    data class ShowMessage(
        val message: String,
    ) : PrivateMessageEffect

    data object MessageSent : PrivateMessageEffect
}

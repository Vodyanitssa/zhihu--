package com.zhihuminus.feature.notification

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.friendlyErrorMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class PrivateMessageViewModel(
    val peerId: String,
    private val repository: NotificationRepository,
) : ViewModel() {
    var uiState by mutableStateOf(PrivateMessageUiState(peerId = peerId))
        private set

    private val _effect = Channel<PrivateMessageEffect>(capacity = Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var nextUrl: String? = null
    private var loadJob: Job? = null

    init {
        loadData(reset = true)
    }

    fun onEvent(event: PrivateMessageEvent) {
        when (event) {
            is PrivateMessageEvent.Refresh -> loadData(reset = true)
            is PrivateMessageEvent.LoadMore -> loadData(reset = false)
            is PrivateMessageEvent.SendMessage -> sendMessage(event.content)
        }
    }

    private fun loadData(reset: Boolean) {
        if (reset) {
            nextUrl = null
        }
        if (uiState.isLoadingMore && !reset) return
        if (uiState.isEnd && !reset) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                if (reset) {
                    uiState = uiState.copy(
                        isRefreshing = uiState.messages.isNotEmpty(),
                        isLoading = uiState.messages.isEmpty(),
                        errorMessage = null,
                    )
                } else {
                    uiState = uiState.copy(isLoadingMore = true)
                }

                val peerDeferred = if (uiState.peer == null) {
                    async { repository.getPrivateMessagePeer(peerId) }
                } else {
                    null
                }

                val result = repository.getPrivateMessages(peerId, if (reset) null else nextUrl)
                val peer = peerDeferred?.await() ?: uiState.peer

                val newMessages = if (reset) {
                    result.items
                } else {
                    val existingIds = uiState.messages.mapTo(mutableSetOf()) { it.stableId }
                    uiState.messages + result.items.filter { existingIds.add(it.stableId) }
                }

                nextUrl = result.nextUrl
                uiState = uiState.copy(
                    peer = peer,
                    messages = newMessages,
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    isEnd = result.isEnd || result.nextUrl.isNullOrBlank(),
                    errorMessage = null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PrivateMessageVM", "Failed to load private messages for $peerId", e)
                uiState = uiState.copy(
                    isLoading = false,
                    isRefreshing = false,
                    isLoadingMore = false,
                    errorMessage = "加载失败: ${friendlyErrorMessage(e)}",
                )
                if (!reset) {
                    _effect.send(PrivateMessageEffect.ShowMessage("加载更多失败: ${friendlyErrorMessage(e)}"))
                }
            }
        }
    }

    private fun sendMessage(content: String) {
        if (content.isBlank() || uiState.isSending) return
        viewModelScope.launch {
            uiState = uiState.copy(isSending = true)
            try {
                val message = repository.sendPrivateMessage(peerId, content)
                val updatedMessages = if (uiState.messages.none { it.stableId == message.stableId }) {
                    listOf(message) + uiState.messages
                } else {
                    uiState.messages
                }
                uiState = uiState.copy(
                    messages = updatedMessages,
                    isSending = false,
                )
                _effect.send(PrivateMessageEffect.MessageSent)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PrivateMessageVM", "Failed to send private message", e)
                uiState = uiState.copy(isSending = false)
                _effect.send(PrivateMessageEffect.ShowMessage(friendlyErrorMessage(e)))
            }
        }
    }
}

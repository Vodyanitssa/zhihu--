package com.zhihuminus.feature.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.Notification
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun PrivateMessageRoute(
    destination: Notification.Message,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createNotificationRepository()
    }
    val viewModel: PrivateMessageViewModel = viewModel(key = "private_message_${destination.peerId}") {
        PrivateMessageViewModel(
            peerId = destination.peerId,
            repository = repository,
        )
    }
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PrivateMessageEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is PrivateMessageEffect.MessageSent -> {}
            }
        }
    }

    val peerName = viewModel.uiState.peer
        ?.name
        ?.ifBlank { destination.name } ?: destination.name
    val peerAvatar = viewModel.uiState.peer
        ?.avatarUrl
        ?.ifBlank { destination.avatarUrl } ?: destination.avatarUrl

    PrivateMessageScreen(
        state = viewModel.uiState,
        peerName = peerName,
        peerAvatar = peerAvatar,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

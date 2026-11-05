package com.zhihuminus.feature.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person

@Composable
fun PrivateMessageRoute(
    destination: Notification.Message,
    onNavigate: (NavDestination) -> Unit,
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

    val peer = viewModel.uiState.peer
    val peerName = peer?.name?.ifBlank { destination.name } ?: destination.name
    val peerAvatar = peer?.avatarUrl?.ifBlank { destination.avatarUrl } ?: destination.avatarUrl
    val peerId = peer?.id?.takeIf { it.isNotBlank() } ?: destination.peerId

    PrivateMessageScreen(
        state = viewModel.uiState,
        peerName = peerName,
        peerAvatar = peerAvatar,
        onEvent = viewModel::onEvent,
        onPeerClick = {
            onNavigate(
                Person(
                    id = peerId,
                    urlToken = peer?.urlToken.orEmpty(),
                    name = peerName,
                ),
            )
        },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

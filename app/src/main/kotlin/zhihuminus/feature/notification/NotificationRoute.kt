package com.zhihuminus.feature.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.core.state.UnreadNotificationState
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination

@Composable
fun NotificationRoute(
    onNavigateBack: () -> Unit,
    onCategoryClick: (NotificationCategory) -> Unit,
    onInvitationClick: () -> Unit,
    onConversationClick: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createNotificationRepository()
    }
    val viewModel: NotificationViewModel = viewModel {
        NotificationViewModel(repository)
    }
    val userMessages = rememberUserMessageSink()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(lifecycleOwner, viewModel) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onEvent(NotificationEvent.Refresh)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    LaunchedEffect(viewModel.uiState.unreadCount) {
        UnreadNotificationState.update(viewModel.uiState.unreadCount)
    }

    NotificationScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onCategoryClick = onCategoryClick,
        onInvitationClick = onInvitationClick,
        onConversationClick = { notification ->
            notification.navDestination()?.let(onConversationClick)
                ?: userMessages.showShortMessage("暂不支持打开此消息")
        },
        showUnreadBadges = com.zhihuminus.core.settings.LocalAppSettings.current.notification.showUnreadBadge,
        modifier = modifier,
    )
}

package com.zhihuminus.feature.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuNotificationRepository
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun NotificationTimelineRoute(
    entryName: String,
    title: String,
    onNavigateBack: () -> Unit,
    onDestinationClick: (NavDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val settingsStore = rememberNotificationSettingsStore()
    val repository = remember(environment) {
        ZhihuNotificationRepository(ZhihuApiImpl(environment))
    }
    val viewModel: NotificationTimelineViewModel = viewModel(key = "notification_timeline_$entryName") {
        NotificationTimelineViewModel(
            entryName = entryName,
            title = title,
            repository = repository,
            settingsStore = settingsStore,
        )
    }
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is NotificationTimelineEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    NotificationTimelineScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNotificationClick = { notification ->
            notification.navDestination()?.let(onDestinationClick)
                ?: userMessages.showShortMessage("暂不支持打开此通知")
        },
        onQuestionClick = { notification ->
            notification.target?.id?.toLongOrNull()?.let { questionId ->
                onDestinationClick(
                    Question(
                        questionId = questionId,
                        title = notification.target.title.ifBlank {
                            notification.targetSource?.text.orEmpty()
                        },
                    ),
                )
            } ?: userMessages.showShortMessage("无法打开这个问题")
        },
        onAnswerClick = { notification ->
            val destination = notification.target
                ?.myAnswerUrl
                ?.takeIf { it.isNotBlank() }
                ?.let(::resolveContent)
            if (destination != null) {
                onDestinationClick(destination)
            } else {
                userMessages.showShortMessage("正在施工")
            }
        },
        modifier = modifier,
    )
}

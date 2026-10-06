package com.zhihuminus.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuHistoryRepository
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun HistoryRoute(
    onNavigateBack: () -> Unit = {},
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuHistoryRepository(ZhihuApiImpl(environment))
    }
    val viewModel: HistoryViewModel = viewModel {
        HistoryViewModel(repository)
    }
    val userMessages = rememberUserMessageSink()
    val navigator = LocalNavigator.current

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is HistoryEffect.Navigate -> navigator.onNavigate(effect.destination)
            }
        }
    }

    HistoryScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        scrollToTopTrigger = scrollToTopTrigger,
        isActive = isActive,
    )
}

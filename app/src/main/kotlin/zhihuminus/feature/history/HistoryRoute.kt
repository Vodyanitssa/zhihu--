package com.zhihuminus.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuHistoryRepository
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

@Composable
fun HistoryRoute(
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

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HistoryEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    HistoryScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        scrollToTopTrigger = scrollToTopTrigger,
        isActive = isActive,
    )
}

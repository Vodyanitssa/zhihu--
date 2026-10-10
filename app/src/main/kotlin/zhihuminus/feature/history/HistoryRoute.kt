package com.zhihuminus.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.LocalNavigator

@Composable
fun HistoryRoute(
    onNavigateBack: () -> Unit = {},
    isActive: Boolean = true,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createHistoryRepository()
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
        isActive = isActive,
    )
}

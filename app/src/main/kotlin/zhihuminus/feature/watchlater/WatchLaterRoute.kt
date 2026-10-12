package com.zhihuminus.feature.watchlater

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.data.local.LocalWatchLaterRepository
import com.zhihuminus.navigation.LocalNavigator

@Composable
fun WatchLaterRoute(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onNavigateBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        LocalWatchLaterRepository.getInstance(context)
    }
    val viewModel: WatchLaterViewModel = viewModel {
        WatchLaterViewModel(repository)
    }
    val userMessages = rememberUserMessageSink()
    val navigator = LocalNavigator.current
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is WatchLaterEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is WatchLaterEffect.Navigate -> navigator.onNavigate(effect.destination)
            }
        }
    }

    WatchLaterScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        innerPadding = innerPadding,
        listState = listState,
        onNavigateBack = onNavigateBack,
    )
}

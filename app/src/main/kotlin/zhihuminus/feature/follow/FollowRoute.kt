package com.zhihuminus.feature.follow

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuFollowRepository
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.TopLevelReselectAction
import com.zhihuminus.ui.topLevelReselectAction

@Composable
fun FollowRoute(
    innerPadding: PaddingValues,
    scrollToTopTrigger: Int = 0,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val userMessages = rememberUserMessageSink()
    val paginationEnvironment = rememberPaginationEnvironment()

    val repository = remember(paginationEnvironment) {
        ZhihuFollowRepository(
            api = ZhihuApiImpl(paginationEnvironment),
        )
    }

    val viewModel: FollowViewModel = viewModel {
        FollowViewModel(repository = repository)
    }

    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        when (action) {
            TopLevelReselectAction.Refresh -> viewModel.onEvent(FollowEvent.ReselectTop(isAtTop = true))
            TopLevelReselectAction.ScrollToTop -> viewModel.onEvent(FollowEvent.ReselectTop(isAtTop = false))
            null -> {}
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FollowEffect.Navigate -> navigator.onNavigate(effect.destination)
                is FollowEffect.ShowMessage -> userMessages.showMessage(effect.message, UserMessageDuration.Long)
                is FollowEffect.ScrollToTop -> listState.animateScrollToItem(0)
                is FollowEffect.OpenExternalUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    FollowScreen(
        state = viewModel.uiState,
        innerPadding = innerPadding,
        listState = listState,
        onEvent = viewModel::onEvent,
    )
}

package com.zhihuminus.feature.follow

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.HandleTopLevelReselect

@Composable
fun FollowRoute(
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val userMessages = rememberUserMessageSink()
    val paginationEnvironment = rememberPaginationEnvironment()

    val repository = remember(paginationEnvironment) {
        ZhihuRepositoryFactory(paginationEnvironment).createFollowRepository()
    }

    val viewModel: FollowViewModel = viewModel {
        FollowViewModel(repository = repository)
    }

    val listState = rememberLazyListState()
    HandleTopLevelReselect(
        destination = Follow,
        listState = listState,
        onRefresh = { viewModel.onEvent(FollowEvent.Refresh) },
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FollowEffect.Navigate -> navigator.onNavigate(effect.destination)
                is FollowEffect.ShowMessage -> userMessages.showMessage(effect.message, UserMessageDuration.Long)
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

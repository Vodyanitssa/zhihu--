package com.zhihuminus.feature.daily

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuDailyRepository
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.HandleTopLevelReselect

@Composable
fun DailyRoute() {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuDailyRepository(ZhihuApiImpl(environment))
    }
    val viewModel: DailyViewModel = viewModel {
        DailyViewModel(repository)
    }
    val userMessages = rememberUserMessageSink()
    val inAppLinkOpener = rememberInAppLinkOpener()
    val listState = rememberLazyListState()

    HandleTopLevelReselect(
        destination = Daily,
        listState = listState,
        onRefresh = { viewModel.onEvent(DailyEvent.Refresh) },
    )

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DailyEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    DailyScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onStoryClick = { url -> inAppLinkOpener(url) },
        listState = listState,
    )
}

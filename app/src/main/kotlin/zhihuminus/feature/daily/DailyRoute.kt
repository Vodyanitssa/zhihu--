package com.zhihuminus.feature.daily

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuDailyRepository
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

@Composable
fun DailyRoute(
    scrollToTopTrigger: Int = 0,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuDailyRepository(ZhihuApiImpl(environment))
    }
    val viewModel: DailyViewModel = viewModel {
        DailyViewModel(repository)
    }
    val userMessages = rememberUserMessageSink()
    val inAppLinkOpener = rememberInAppLinkOpener()

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
        scrollToTopTrigger = scrollToTopTrigger,
    )
}

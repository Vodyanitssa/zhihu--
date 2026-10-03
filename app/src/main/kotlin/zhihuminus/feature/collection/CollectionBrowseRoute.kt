package com.zhihuminus.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuCollectionRepository
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun CollectionBrowseRoute(
    urlToken: String?,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
    showBackButton: Boolean = false,
    scrollToTopTrigger: Int = 0,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuCollectionRepository(ZhihuApiImpl(environment))
    }
    val viewModel: CollectionBrowseViewModel = viewModel(key = urlToken) {
        CollectionBrowseViewModel(urlToken.orEmpty(), repository)
    }
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CollectionBrowseEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    CollectionBrowseScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onDestinationClick = onDestinationClick,
        showBackButton = showBackButton,
        scrollToTopTrigger = scrollToTopTrigger,
        modifier = modifier,
    )
}

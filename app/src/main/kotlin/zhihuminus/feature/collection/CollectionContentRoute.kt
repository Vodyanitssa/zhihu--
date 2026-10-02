package com.zhihuminus.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuCollectionRepository
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

@Composable
fun CollectionContentRoute(
    collectionId: String,
    onNavigateBack: () -> Unit,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuCollectionRepository(ZhihuApiImpl(environment))
    }
    val viewModel: CollectionContentViewModel = viewModel(key = collectionId) {
        CollectionContentViewModel(collectionId, repository)
    }
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CollectionContentEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    CollectionContentScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    )
}

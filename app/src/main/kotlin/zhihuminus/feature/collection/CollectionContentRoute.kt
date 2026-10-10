package com.zhihuminus.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination

@Composable
fun CollectionContentRoute(
    collectionId: String,
    onNavigateBack: () -> Unit,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createCollectionRepository()
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

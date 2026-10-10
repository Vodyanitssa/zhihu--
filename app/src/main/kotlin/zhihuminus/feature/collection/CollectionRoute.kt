package com.zhihuminus.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun CollectionRoute(
    urlToken: String?,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createCollectionRepository()
    }
    val viewModel: CollectionViewModel = viewModel(key = urlToken) {
        CollectionViewModel(urlToken.orEmpty(), repository)
    }
    val userMessages = rememberUserMessageSink()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CollectionEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
            }
        }
    }

    CollectionScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        showBackButton = showBackButton,
        isActive = isActive,
        modifier = modifier,
    )
}

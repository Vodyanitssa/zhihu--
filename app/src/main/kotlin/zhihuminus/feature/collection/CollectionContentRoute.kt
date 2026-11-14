package com.zhihuminus.feature.collection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination

@Composable
fun CollectionContentRoute(
    collectionId: String,
    isOwnedHint: Boolean? = null,
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
    val account = AccountData.asState().value

    val isOwner = isOwnedHint ?: run {
        val self = account.profile
        val creator = viewModel.uiState.collection?.creator
        when {
            self == null || creator == null -> false
            self.id.isNotBlank() && creator.id == self.id -> true
            !self.urlToken.isNullOrBlank() && creator.id == self.urlToken -> true
            else -> false
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CollectionContentEffect.ShowMessage -> if (effect.isError) userMessages.showError(effect.message) else userMessages.showShortMessage(effect.message)
                is CollectionContentEffect.NavigateBack -> onNavigateBack()
            }
        }
    }

    CollectionContentScreen(
        state = viewModel.uiState,
        isOwner = isOwner,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onDestinationClick = onDestinationClick,
        modifier = modifier,
    )
}

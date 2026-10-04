package com.zhihuminus.feature.search

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuSearchRepository
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Search
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun SearchRoute(
    search: Search,
    onBack: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val settings = rememberSettingsStore()
    val paginationEnvironment = rememberPaginationEnvironment()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val uriHandler = LocalUriHandler.current
    val focusRequester = remember { FocusRequester() }

    val generalListState = rememberLazyListState()
    val peopleListState = rememberLazyListState()
    val topicListState = rememberLazyListState()

    val repository = remember(paginationEnvironment, settings) {
        ZhihuSearchRepository(
            environment = paginationEnvironment,
            settings = settings,
        )
    }

    val viewModel: SearchViewModel = viewModel(key = "search_${search.query}_${search.restrictedMemberHashId}") {
        SearchViewModel(
            initialQuery = search.query,
            restrictedMemberHashId = search.restrictedMemberHashId,
            restrictedMemberName = search.restrictedMemberName,
            repository = repository,
        )
    }

    LaunchedEffect(Unit) {
        if (search.query.isBlank()) {
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SearchEffect.Navigate -> navigator.onNavigate(effect.destination)
                is SearchEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is SearchEffect.OpenExternalUrl -> uriHandler.openUri(effect.url)
                is SearchEffect.ClearFocusAndHideKeyboard -> {
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                }
                is SearchEffect.NavigateBack -> onBack()
            }
        }
    }

    SearchScreen(
        state = viewModel.uiState,
        generalListState = generalListState,
        peopleListState = peopleListState,
        topicListState = topicListState,
        focusRequester = focusRequester,
        onEvent = viewModel::onEvent,
    )
}

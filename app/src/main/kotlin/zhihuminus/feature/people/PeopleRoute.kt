package com.zhihuminus.feature.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImagePreviewOpener
import com.zhihuminus.platform.rememberUserMessageSink
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PeopleRoute(
    person: Person,
    onNavigate: (NavDestination) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createPeopleRepository()
    }
    val viewModel: PeopleViewModel = viewModel(key = "people_${person.userTokenOrId}") {
        PeopleViewModel(person, repository)
    }

    val userMessages = rememberUserMessageSink()
    val inAppLinkOpener = rememberInAppLinkOpener()
    val imagePreviewOpener = rememberImagePreviewOpener()
    val externalUrlOpener = rememberExternalUrlOpener()

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PeopleEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is PeopleEffect.ProfileLoaded -> environment.postHistoryDestination(effect.person)
                is PeopleEffect.Navigate -> onNavigate(effect.destination)
            }
        }
    }

    val initialSelection = remember(person.jumpTo) {
        resolvePeopleInitialSelection(person.jumpTo)
    }

    PeopleScreen(
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onNavigate = onNavigate,
        onLinkClick = { inAppLinkOpener(it) },
        onImagePreview = { imagePreviewOpener(it) },
        onExternalUrl = { externalUrlOpener(it) },
        initialSelection = initialSelection,
        modifier = modifier,
    )
}

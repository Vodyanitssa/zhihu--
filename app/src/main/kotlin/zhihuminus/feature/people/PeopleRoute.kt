package com.zhihuminus.feature.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.platform.rememberExternalUrlOpener
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.ui.components.ShareDialog
import com.zhihuminus.ui.components.getShareText
import com.zhihuminus.ui.components.handleShareAction
import com.zhihuminus.ui.components.rememberImagePreviewOpener
import com.zhihuminus.ui.components.rememberShareActionExecutor
import com.zhihuminus.ui.rememberAccountSettingsAccountState
import kotlinx.coroutines.flow.collectLatest

@Composable
fun PeopleRoute(
    person: Person,
    onNavigate: (NavDestination) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val environment = rememberPaginationEnvironment()
    val account = rememberAccountSettingsAccountState().value
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
    val appSettings = LocalAppSettings.current
    val executeShareAction = rememberShareActionExecutor()
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PeopleEffect.ShowMessage -> if (effect.isError) userMessages.showError(effect.message) else userMessages.showShortMessage(effect.message)
                is PeopleEffect.ProfileLoaded -> environment.postHistoryDestination(effect.person)
                is PeopleEffect.Navigate -> onNavigate(effect.destination)
            }
        }
    }

    val initialSelection = remember(person.jumpTo) {
        resolvePeopleInitialSelection(person.jumpTo)
    }

    val loadedPerson = Person(
        id = viewModel.memberId,
        urlToken = viewModel.tokenOrId,
        name = viewModel.uiState.profile.name
            .ifBlank { person.name },
    )

    val profile = viewModel.uiState.profile
    val isSelf = (account.id.isNotBlank() && profile.id == account.id) ||
        (account.urlToken?.isNotBlank() == true && profile.urlToken == account.urlToken)
    val canMessage = account.login &&
        profile.id.isNotBlank() &&
        profile.id != Person.EMPTY_ID &&
        !isSelf

    PeopleScreen(
        state = viewModel.uiState,
        canMessage = canMessage,
        onEvent = viewModel::onEvent,
        onBack = onNavigateBack,
        onShare = {
            handleShareAction(loadedPerson, appSettings.interaction.shareAction, executeShareAction) {
                showShareDialog = true
            }
        },
        onNavigate = onNavigate,
        onLinkClick = { inAppLinkOpener(it) },
        onImagePreview = { imagePreviewOpener(it) },
        onExternalUrl = { externalUrlOpener(it) },
        initialSelection = initialSelection,
        modifier = modifier,
    )

    getShareText(loadedPerson)?.let { shareText ->
        ShareDialog(
            content = loadedPerson,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
        )
    }
}

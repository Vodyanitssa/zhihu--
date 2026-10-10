package com.zhihuminus.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.state.UnreadNotificationState
import com.zhihuminus.core.state.rememberUnreadNotificationCount
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuHomeRepository
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberAppPrivateDirectory
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.HandleTopLevelReselect
import com.zhihuminus.ui.rememberAccountSettingsAccountState
import kotlinx.io.files.Path

@Composable
fun HomeRoute(
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val userMessages = rememberUserMessageSink()
    val appSettings = LocalAppSettings.current
    val paginationEnvironment = rememberPaginationEnvironment()
    val appPrivateDirectory = rememberAppPrivateDirectory()

    val autoRefreshOnStartup = appSettings.interaction.autoRefreshHomeOnStartup
    val showUnreadBadge = appSettings.notification.showUnreadBadge

    val repository = remember(appPrivateDirectory, paginationEnvironment) {
        val cacheFile = Path(appPrivateDirectory, homeFeedStartupCacheFileName())
        ZhihuHomeRepository(
            api = ZhihuApiImpl(paginationEnvironment),
            environment = paginationEnvironment,
            startupCacheFile = cacheFile,
        )
    }

    val viewModel: HomeViewModel = viewModel {
        HomeViewModel(
            repository = repository,
            autoRefreshOnStartup = autoRefreshOnStartup,
        )
    }

    val account = rememberAccountSettingsAccountState().value
    if (account.login && !account.hasRequiredCookie) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cookie 不完整") },
            text = { Text("当前登录信息缺少必要的 Cookie d_c0，请重新登录。") },
            confirmButton = {
                TextButton(onClick = { paginationEnvironment.requestLogin() }) {
                    Text("重新登录")
                }
            },
        )
    }

    val listState = rememberLazyListState()
    HandleTopLevelReselect(
        destination = Home,
        listState = listState,
        onRefresh = { viewModel.onEvent(HomeEvent.Refresh) },
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, repository) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val count = repository.fetchUnreadNotificationCount()
            UnreadNotificationState.update(count)
            viewModel.onEvent(HomeEvent.UpdateUnreadCount(count))
        }
    }

    val sharedUnreadCount by rememberUnreadNotificationCount()
    LaunchedEffect(sharedUnreadCount) {
        viewModel.onEvent(HomeEvent.UpdateUnreadCount(sharedUnreadCount))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.Navigate -> navigator.onNavigate(effect.destination)
                is HomeEffect.ShowMessage -> userMessages.showMessage(effect.message, UserMessageDuration.Short)
                is HomeEffect.OpenExternalUrl -> uriHandler.openUri(effect.url)
            }
        }
    }

    HomeScreen(
        state = viewModel.uiState,
        account = account,
        showUnreadBadge = showUnreadBadge,
        innerPadding = innerPadding,
        listState = listState,
        onEvent = viewModel::onEvent,
        onRequestLogin = {
            if (!paginationEnvironment.requestLogin()) {
                userMessages.showShortMessage("当前平台暂不支持登录")
            }
        },
    )
}

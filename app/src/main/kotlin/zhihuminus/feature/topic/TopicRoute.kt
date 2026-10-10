package com.zhihuminus.feature.topic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Topic
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.ShareDialog
import com.zhihuminus.ui.components.getShareText
import com.zhihuminus.ui.components.handleShareAction
import com.zhihuminus.ui.components.rememberShareActionExecutor

@Composable
fun TopicRoute(topic: Topic) {
    val environment = rememberPaginationEnvironment()
    val repository = remember(environment) {
        ZhihuRepositoryFactory(environment).createTopicRepository()
    }
    val viewModel: TopicViewModel = viewModel(key = "topic_${topic.id}_${topic.section}") {
        TopicViewModel(
            topicId = topic.id,
            initialName = topic.name,
            repository = repository,
        )
    }
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val appSettings = LocalAppSettings.current
    val executeShareAction = rememberShareActionExecutor()
    var showShareDialog by remember { mutableStateOf(false) }

    LaunchedEffect(topic.section) {
        viewModel.onEvent(TopicEvent.InitializeSection(topic.section))
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TopicEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is TopicEffect.Navigate -> navigator.onNavigate(effect.destination)
            }
        }
    }

    val loadedTopic = topic.copy(name = viewModel.uiState.detail?.name ?: topic.name)

    TopicScreen(
        topic = topic,
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onBack = navigator.onNavigateBack,
        onShare = {
            handleShareAction(loadedTopic, appSettings.interaction.shareAction, executeShareAction) { showShareDialog = true }
        },
    )

    getShareText(loadedTopic)?.let { shareText ->
        ShareDialog(
            content = loadedTopic,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
        )
    }
}

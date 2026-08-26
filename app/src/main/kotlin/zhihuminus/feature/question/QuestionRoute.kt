package com.zhihuminus.feature.question

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Question
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.ArticleAnswerSwitchState
import com.zhihuminus.ui.ArticleHost
import com.zhihuminus.viewmodel.ZhihuApiEnvironment

/**
 * 问题页路由组件：创建 ViewModel、收集副作用、挂载图片预览层。
 *
 * @param articleHost 宿主 Activity（用于把问题记入导航历史）
 * @param articleAnswerSwitchState 回答切换共享状态；点击回答卡片时把来源导航器交接给文章页
 */
@Composable
fun QuestionRoute(
    destination: Question,
    repository: QuestionRepository,
    commentRepository: CommentRepository,
    apiEnvironment: ZhihuApiEnvironment,
    articleHost: ArticleHost?,
    articleAnswerSwitchState: ArticleAnswerSwitchState?,
    onBack: () -> Unit,
    initialCommentId: String? = null,
) {
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    val navigator = LocalNavigator.current
    val imageViewManager = remember { ImageViewManager() }

    val viewModel: QuestionViewModel =
        viewModel(key = "question_${destination.questionId}") {
            QuestionViewModel(
                questionId = destination.questionId,
                initialTitle = destination.title,
                repository = repository,
                apiEnvironment = apiEnvironment,
            )
        }

    // 每次进入页面（含从回答页返回）都刷新问题详情，与旧行为一致
    LaunchedEffect(viewModel) {
        viewModel.loadMeta()
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QuestionEffect.ShowMessage -> userMessages.showShortMessage(effect.message)

                is QuestionEffect.Navigate -> navigator.onNavigate(effect.destination)

                is QuestionEffect.OpenExternalUrl -> openExternalUrl(effect.url)

                is QuestionEffect.ContentOpened ->
                    articleHost?.postHistoryDestination(Question(destination.questionId, effect.title))
            }
        }
    }

    // 详情 AST 只解析一次（见 ViewModel），这里复用节点注册图片预览列表
    LaunchedEffect(viewModel.uiState.contentNodes) {
        imageViewManager.submitImages(
            viewModel.uiState.contentNodes
                .filterIsInstance<ContentNode.Image>()
                .filter { it.url.isNotBlank() }
                .map { it.url },
        )
    }

    CompositionLocalProvider(LocalImageViewManager provides imageViewManager) {
        QuestionScreen(
            destination = destination,
            state = viewModel.uiState,
            commentRepository = commentRepository,
            readingQueueSourceId = "question:${destination.questionId}:answers:${viewModel.uiState.sort.apiValue}",
            initialCommentId = initialCommentId,
            onEvent = viewModel::onEvent,
            onAnswerClick = { item, itemDestination ->
                viewModel.createAnswerNavigatorFor(item)?.let { answerNavigator ->
                    articleAnswerSwitchState?.pendingNavigator = answerNavigator
                }
                itemDestination?.let(navigator.onNavigate)
            },
            onBack = onBack,
        )

        // 放在 QuestionScreen 之后，保证预览始终处于窗口最顶层
        ImageView(
            manager = imageViewManager,
            actions = ImageViewActions(
                onSave = { saveImage(it) },
                onShare = { shareImage(it) },
                onOpenInBrowser = { openExternalUrl(it) },
            ),
        )
    }
}

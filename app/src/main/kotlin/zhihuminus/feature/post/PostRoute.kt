package com.zhihuminus.feature.post

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.core.platform.rememberExternalUrlOpener
import com.zhihuminus.core.platform.rememberImageSaver
import com.zhihuminus.core.platform.rememberImageSharer
import com.zhihuminus.core.platform.rememberPlainTextClipboard
import com.zhihuminus.core.platform.rememberShareText
import com.zhihuminus.core.platform.rememberUserMessageSink
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.PostDestination

@Composable
fun PostRoute(
    destination: PostDestination,
    repository: PostRepository,
    commentRepository: CommentRepository,
    onBack: () -> Unit,
    initialCommentId: String? = null,
) {
    val context = LocalContext.current
    val shareText = rememberShareText()
    val copyToClipboard = rememberPlainTextClipboard()
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    val userMessages = rememberUserMessageSink()

    val imageViewManager = remember { ImageViewManager() }

    val navigator = LocalNavigator.current

    val viewModel: PostViewModel = viewModel {
        PostViewModel(
            application = context.applicationContext as android.app.Application,
            postId = destination.id,
            postType = destination.type,
            repository = repository,
        )
    }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PostEffect.ShareText -> shareText(effect.text)
                is PostEffect.CopyLink -> {
                    copyToClipboard("链接", effect.link)
                    userMessages.showShortMessage("链接已复制")
                }

                is PostEffect.ShowMessage -> {
                    userMessages.showShortMessage(effect.message)
                }

                is PostEffect.OpenExternalUrl -> openExternalUrl(effect.url)
                is PostEffect.Navigate -> navigator.onNavigate(effect.destination)
            }
        }
    }

    CompositionLocalProvider(LocalImageViewManager provides imageViewManager) {
        PostScreen(
            uiState = viewModel.uiState,
            commentRepository = commentRepository,
            initialCommentId = initialCommentId,
            onEvent = viewModel::onEvent,
            onBack = onBack,
        )

        // 放在 PostScreen 之后，保证预览始终处于窗口最顶层
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

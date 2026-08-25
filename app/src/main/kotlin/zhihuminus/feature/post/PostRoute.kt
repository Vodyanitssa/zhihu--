package com.zhihuminus.feature.post

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberPlainTextClipboard
import com.zhihuminus.platform.rememberShareText

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
                    Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show()
                }

                is PostEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
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

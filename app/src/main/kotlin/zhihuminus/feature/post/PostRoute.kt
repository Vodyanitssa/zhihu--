package com.zhihuminus.feature.post

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImagePreviewOpener
import com.zhihuminus.platform.rememberPlainTextClipboard
import com.zhihuminus.platform.rememberShareText

@Composable
fun PostRoute(
    destination: PostDestination,
    repository: PostRepository,
    commentRepository: CommentRepository,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val shareText = rememberShareText()
    val copyToClipboard = rememberPlainTextClipboard()
    val openExternalUrl = rememberExternalUrlOpener()
    val openImagePreview = rememberImagePreviewOpener()

    val viewModel: PostViewModel = viewModel {
        PostViewModel(
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
                is PostEffect.OpenImage -> openImagePreview(effect.url)
            }
        }
    }

    PostScreen(
        uiState = viewModel.uiState,
        bottomBarState = viewModel.bottomBarState,
        collections = viewModel.collections,
        commentRepository = commentRepository,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )
}

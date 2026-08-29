package com.zhihuminus.feature.column

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberUserMessageSink

@Composable
fun ColumnRoute(
    columnId: String,
    repository: ColumnRepository,
    onBack: () -> Unit,
) {
    val userMessages = rememberUserMessageSink()
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    val navigator = LocalNavigator.current
    val imageViewManager = remember { ImageViewManager() }

    val viewModel: ColumnViewModel =
        viewModel(key = "column_$columnId") {
            ColumnViewModel(
                columnId = columnId,
                repository = repository,
            )
        }

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ColumnEffect.ShowMessage -> userMessages.showShortMessage(effect.message)
                is ColumnEffect.Navigate -> navigator.onNavigate(effect.destination)
            }
        }
    }

    ColumnScreen(
        columnId = columnId,
        state = viewModel.uiState,
        onEvent = viewModel::onEvent,
        onBack = onBack,
    )

    ImageView(
        manager = imageViewManager,
        actions = ImageViewActions(
            onSave = { saveImage(it) },
            onShare = { shareImage(it) },
            onOpenInBrowser = { openExternalUrl(it) },
        ),
    )
}

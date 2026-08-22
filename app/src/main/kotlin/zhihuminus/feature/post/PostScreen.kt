package com.zhihuminus.feature.post

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuminus.data.Collection
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentRoute
import com.zhihuminus.feature.post.components.PostActionsMenu
import com.zhihuminus.feature.post.components.PostBottomBar
import com.zhihuminus.feature.post.components.PostBottomBarState
import com.zhihuminus.feature.post.components.PostContent
import com.zhihuminus.feature.post.components.PostEvent
import com.zhihuminus.feature.post.components.PostHeader
import com.zhihuminus.ui.components.CollectionDialogComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    uiState: PostUiState,
    bottomBarState: PostBottomBarState,
    collections: List<Collection>,
    commentRepository: CommentRepository,
    onEvent: (PostEvent) -> Unit,
    onBack: () -> Unit,
) {
    var showCollectionDialog by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showActionsMenu by androidx.compose.runtime.remember { mutableStateOf(false) }
    var showComments by androidx.compose.runtime.remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState) {
                        is PostUiState.Success -> when (state.post.type) {
                            PostType.Answer -> state.post.title.ifBlank { "回答" }
                            PostType.Article -> state.post.title
                            PostType.Pin -> "想法"
                        }

                        else -> "加载中"
                    }
                    Text(title, maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            if (uiState is PostUiState.Success) {
                PostBottomBar(
                    postType = uiState.post.type,
                    state = bottomBarState,
                    onEvent = { event ->
                        when (event) {
                            is PostEvent.ShowCollectionDialog -> {
                                showCollectionDialog = true
                            }

                            is PostEvent.ShowMoreMenu -> {
                                showActionsMenu = true
                            }

                            is PostEvent.Comment -> {
                                showComments = true
                            }

                            else -> onEvent(event)
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is PostUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PostUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = state.error.message ?: "未知错误",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is PostUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    PostHeader(
                        author = state.post.author,
                        createdAt = state.post.createdAt,
                        updatedAt = state.post.updatedAt,
                        ipInfo = state.post.ipInfo,
                    )
                    PostContent(
                        post = state.post,
                    )
                }

                // Collection Dialog
                CollectionDialogComponent(
                    showDialog = showCollectionDialog,
                    onDismiss = { showCollectionDialog = false },
                    collections = collections,
                    onLoadCollections = { onEvent(PostEvent.Refresh) },
                    onToggleFavorite = { collection ->
                        onEvent(PostEvent.ToggleCollection(collection))
                        showCollectionDialog = false
                    },
                    onCreateCollection = { title, description, isPublic ->
                        onEvent(PostEvent.CreateCollection(title, description, isPublic))
                    },
                )

                // Actions Menu
                PostActionsMenu(
                    post = state.post,
                    showMenu = showActionsMenu,
                    onDismissRequest = { showActionsMenu = false },
                    onShare = {
                        onEvent(PostEvent.Share)
                    },
                    onCopyLink = {
                        onEvent(PostEvent.CopyLink)
                    },
                    onExport = { onEvent(PostEvent.Export) },
                )

                // Comments
                CommentRoute(
                    showComments = showComments,
                    onDismiss = { showComments = false },
                    contentType = state.post.type,
                    contentId = state.post.id,
                    repository = commentRepository,
                )
            }
        }
    }
}

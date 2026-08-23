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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuminus.data.Collection
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentRoute
import com.zhihuminus.feature.post.components.PostActionsMenu
import com.zhihuminus.feature.post.components.PostBottomBar
import com.zhihuminus.feature.post.components.PostBottomBarState
import com.zhihuminus.feature.post.components.PostContent
import com.zhihuminus.feature.post.components.PostHeader
import com.zhihuminus.ui.components.CollectionDialogComponent

sealed interface PostLoadState {
    data object Loading : PostLoadState

    data class Success(
        val post: Post,
    ) : PostLoadState

    data class Error(
        val message: String?,
    ) : PostLoadState
}

data class PostUiState(
    val loadState: PostLoadState = PostLoadState.Loading,
    val bottomBarState: PostBottomBarState = PostBottomBarState(),
    val collections: List<Collection> = emptyList(),
    val isCollected: Boolean = false,
    val showCollectionDialog: Boolean = false,
    val showActionsMenu: Boolean = false,
    val showComments: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    uiState: PostUiState,
    commentRepository: CommentRepository,
    onEvent: (PostEvent) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when (val state = uiState.loadState) {
                        is PostLoadState.Success -> when (state.post.type) {
                            PostType.Answer -> state.post.title.ifBlank { "回答" }
                            PostType.Article -> state.post.title
                            PostType.Pin -> "${state.post.author.name}的想法"
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
            if (uiState.loadState is PostLoadState.Success) {
                PostBottomBar(
                    postType = uiState.loadState.post.type,
                    state = uiState.bottomBarState,
                    onEvent = onEvent,
                )
            }
        },
    ) { paddingValues ->
        when (val state = uiState.loadState) {
            is PostLoadState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is PostLoadState.Error -> {
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
                            text = state.message ?: "未知错误",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is PostLoadState.Success -> {
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
                        voteCount = state.post.voteCount,
                        isFollowing = state.post.author.isFollowing,
                        onFollowClick = { onEvent(PostEvent.FollowAuthor) },
                    )
                    PostContent(
                        post = state.post,
                        onEvent = onEvent,
                    )
                }

                // Collection Dialog
                CollectionDialogComponent(
                    showDialog = uiState.showCollectionDialog,
                    onDismiss = { onEvent(PostEvent.DismissCollectionDialog) },
                    collections = uiState.collections,
                    onLoadCollections = { onEvent(PostEvent.RefreshCollections) },
                    onToggleFavorite = { collection ->
                        onEvent(PostEvent.ToggleCollection(collection))
                    },
                    onCreateCollection = { title, description, isPublic ->
                        onEvent(PostEvent.CreateCollection(title, description, isPublic))
                    },
                )

                // Actions Menu
                PostActionsMenu(
                    post = state.post,
                    showMenu = uiState.showActionsMenu,
                    onDismissRequest = { onEvent(PostEvent.DismissActionsMenu) },
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
                    showComments = uiState.showComments,
                    onDismiss = { onEvent(PostEvent.DismissComments) },
                    contentType = state.post.type,
                    contentId = state.post.id,
                    repository = commentRepository,
                )
            }
        }
    }
}

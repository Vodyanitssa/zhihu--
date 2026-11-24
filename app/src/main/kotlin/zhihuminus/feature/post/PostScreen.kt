package com.zhihuminus.feature.post

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.zhihuminus.core.content.extractAllImageUrls
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.core.error.AppError
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.collection.components.CollectionDialogComponent
import com.zhihuminus.feature.comment.CommentContentType
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentRoute
import com.zhihuminus.feature.post.components.PostActionsMenu
import com.zhihuminus.feature.post.components.PostBottomBar
import com.zhihuminus.feature.post.components.PostBottomBarState
import com.zhihuminus.feature.post.components.PostContent
import com.zhihuminus.feature.post.components.PostExportDialog
import com.zhihuminus.feature.post.components.PostHeader
import com.zhihuminus.feature.post.components.PostScrollToTopButton
import com.zhihuminus.navigation.link.rememberInAppLinkOpener
import com.zhihuminus.ui.components.EndorsementChipList
import com.zhihuminus.ui.components.ErrorFullPage
import com.zhihuminus.ui.components.ScrollAwareTopBarTitle
import com.zhihuminus.ui.components.VerticalReadingProgressBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt

sealed interface PostLoadState {
    data object Loading : PostLoadState

    data class Success(
        val post: Post,
    ) : PostLoadState

    data class Error(
        val error: AppError,
    ) : PostLoadState
}

data class PostUiState(
    val postType: PostType = PostType.Answer,
    val loadState: PostLoadState = PostLoadState.Loading,
    val bottomBarState: PostBottomBarState = PostBottomBarState(),
    val collections: List<Collection> = emptyList(),
    val isCollected: Boolean = false,
    val showCollectionDialog: Boolean = false,
    val showActionsMenu: Boolean = false,
    val showComments: Boolean = false,
    val isExporting: Boolean = false,
    /** 上次离开时的阅读进度（0..1），null 表示尚未读取到或没有记录 */
    val savedReadingProgress: Float? = null,
)

private const val READING_PROGRESS_HIDE_WINDOW_MILLIS = 200L
private const val READING_PROGRESS_FADE_IN_MILLIS = 180

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    uiState: PostUiState,
    commentRepository: CommentRepository,
    onEvent: (PostEvent) -> Unit,
    onBack: () -> Unit,
    initialCommentId: String? = null,
    onReadingProgressChanged: (Float) -> Unit,
) {
    var showExportDialog by remember { mutableStateOf(false) }
    val openLink = rememberInAppLinkOpener()
    val scrollState = rememberScrollState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var isTransitionFinished by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var hasRestoredReadingProgress by rememberSaveable { mutableStateOf(false) }
    val needsRestore = (uiState.savedReadingProgress ?: 0f) > PostViewModel.MIN_VISIBLE_READING_PROGRESS &&
        !hasRestoredReadingProgress
    val contentAlpha by animateFloatAsState(
        targetValue = if (needsRestore) 0f else 1f,
        animationSpec = tween(durationMillis = READING_PROGRESS_FADE_IN_MILLIS),
        label = "postContentAlpha",
    )
    val reportReadingProgress = rememberUpdatedState(onReadingProgressChanged)

    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            isTransitionFinished = true
        }
    }

    // 只恢复一次：正文渲染且布局完成后按比例定位，隐藏窗口内跟随图片加载的高度变化，随后淡入
    val loadedPostId = (uiState.loadState as? PostLoadState.Success)?.post?.id
    LaunchedEffect(uiState.savedReadingProgress, isTransitionFinished, loadedPostId) {
        val target = uiState.savedReadingProgress
            ?.takeIf { it > PostViewModel.MIN_VISIBLE_READING_PROGRESS }
            ?: return@LaunchedEffect
        if (!isTransitionFinished || hasRestoredReadingProgress || loadedPostId == null) return@LaunchedEffect
        snapshotFlow { scrollState.maxValue }.first { it != Int.MAX_VALUE }
        scrollState.scrollTo((target * scrollState.maxValue).roundToInt())
        withTimeoutOrNull(READING_PROGRESS_HIDE_WINDOW_MILLIS) {
            snapshotFlow { scrollState.maxValue to scrollState.isScrollInProgress }
                .takeWhile { !it.second }
                .collect { (max, _) ->
                    if (max > 0 && max != Int.MAX_VALUE) {
                        scrollState.scrollTo((target * max).roundToInt())
                    }
                }
        }
        hasRestoredReadingProgress = true
    }

    // 导航返回（onDispose）与切后台（ON_STOP）都记录一次阅读进度
    DisposableEffect(lifecycleOwner) {
        fun reportProgress() {
            val max = scrollState.maxValue
            if (max <= 0 || max == Int.MAX_VALUE) return
            reportReadingProgress.value(scrollState.value.toFloat() / max)
        }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) reportProgress()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            reportProgress()
        }
    }

    // 深链锚点仅驱动初始打开一次；此后 showComments 是唯一事实来源，dismiss 后才能真正关闭
    LaunchedEffect(initialCommentId) {
        if (initialCommentId != null) {
            onEvent(PostEvent.Comment)
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val (title, placeholder) = when (val loadState = uiState.loadState) {
                            is PostLoadState.Success -> when (loadState.post.type) {
                                PostType.Answer -> loadState.post.title.ifBlank { "回答" } to "回答"
                                PostType.Article -> loadState.post.title to "文章"
                                PostType.Pin -> "${loadState.post.author.name}的想法" to "想法"
                            }

                            is PostLoadState.Error -> when (uiState.postType) {
                                PostType.Answer -> "回答" to "回答"
                                PostType.Article -> "文章" to "文章"
                                PostType.Pin -> "想法" to "想法"
                            }

                            is PostLoadState.Loading -> "加载中" to "加载中"
                        }
                        ScrollAwareTopBarTitle(state = scrollState, title = title, placeholder = placeholder)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            },
            bottomBar = {
                if (uiState.loadState is PostLoadState.Success && isTransitionFinished) {
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                        )
                    }
                }

                is PostLoadState.Error -> {
                    ErrorFullPage(
                        error = state.error,
                        onRetry = { onEvent(PostEvent.Refresh) },
                        modifier = Modifier.padding(paddingValues),
                    )
                }

                is PostLoadState.Success -> {
                    val imageViewManager = LocalImageViewManager.current
                    LaunchedEffect(state.post.content, state.post.coverUrl, isTransitionFinished) {
                        if (isTransitionFinished) {
                            val contentImages = state.post.content.extractAllImageUrls()
                            val allImages = listOfNotNull(state.post.coverUrl) + contentImages
                            imageViewManager?.submitImages(allImages)
                        }
                    }
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .graphicsLayer { alpha = contentAlpha },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                        ) {
                            PostHeader(
                                title = state.post.title,
                                author = state.post.author,
                                createdAt = state.post.createdAt,
                                updatedAt = state.post.updatedAt,
                                ipInfo = state.post.ipInfo,
                                voteCount = state.post.voteCount,
                                isFollowing = state.post.author.isFollowing == true,
                                questionId = state.post.questionId,
                                coverUrl = state.post.coverUrl,
                                onCoverClick = { url -> imageViewManager?.show(url) },
                                onFollowClick = { onEvent(PostEvent.FollowAuthor) },
                            )
                            if (state.post.type == PostType.Answer && state.post.endorsements.isNotEmpty()) {
                                EndorsementChipList(
                                    endorsements = state.post.endorsements,
                                    onEndorsementClick = openLink,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 10.dp),
                                )
                            }
                            if (isTransitionFinished) {
                                PostContent(
                                    post = state.post,
                                    onEvent = onEvent,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        strokeWidth = 2.5.dp,
                                    )
                                }
                            }
                        }

                        if (isTransitionFinished) {
                            PostScrollToTopButton(
                                scrollState = scrollState,
                                containerWidth = maxWidth,
                                containerHeight = maxHeight,
                                positionKey = state.post.id,
                                modifier = Modifier.align(Alignment.TopStart),
                            )
                        }
                    }

                    if (contentAlpha < 1f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = paddingValues.calculateTopPadding())
                                .graphicsLayer { alpha = 1f - contentAlpha },
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.5.dp,
                            )
                        }
                    }

                    if (isTransitionFinished) {
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
                            onExport = {
                                onEvent(PostEvent.DismissActionsMenu)
                                showExportDialog = true
                            },
                            onAddToWatchLater = {
                                onEvent(PostEvent.AddToWatchLater)
                            },
                        )

                        // Export Dialog
                        PostExportDialog(
                            showDialog = showExportDialog,
                            isExporting = uiState.isExporting,
                            onDismiss = { showExportDialog = false },
                            onExportImage = { onEvent(PostEvent.Export) },
                            onExportMarkdown = { onEvent(PostEvent.ExportMarkdown) },
                        )

                        // Comments
                        CommentRoute(
                            showComments = uiState.showComments,
                            onDismiss = { onEvent(PostEvent.DismissComments) },
                            contentType = state.post.type.toCommentContentType(),
                            contentId = state.post.id,
                            repository = commentRepository,
                            initialCommentId = initialCommentId,
                        )
                    }
                }
            }
        }

        if (isTransitionFinished && !needsRestore) {
            VerticalReadingProgressBar(
                scrollState = scrollState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .systemBarsPadding(),
                thumbColor = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun PostType.toCommentContentType(): CommentContentType = when (this) {
    PostType.Answer -> CommentContentType.Answer
    PostType.Article -> CommentContentType.Article
    PostType.Pin -> CommentContentType.Pin
}

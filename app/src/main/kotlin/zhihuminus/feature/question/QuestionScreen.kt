package com.zhihuminus.feature.question

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.comment.CommentContentType
import com.zhihuminus.feature.comment.CommentRepository
import com.zhihuminus.feature.comment.CommentRoute
import com.zhihuminus.feature.question.components.QuestionBodyHeader
import com.zhihuminus.feature.question.components.QuestionHeaderSection
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.ui.components.ScrollAwareTopBarTitle
import com.zhihuminus.ui.components.ShareDialog
import com.zhihuminus.ui.components.getShareText
import com.zhihuminus.ui.components.handleShareAction
import com.zhihuminus.ui.components.rememberShareActionExecutor

/**
 * 问题详情页（纯状态 UI）。
 *
 * 顶部展示问题标题、描述、关注状态和统计信息，主体是该问题下回答的信息流列表。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionScreen(
    destination: Question,
    state: QuestionUiState,
    commentRepository: CommentRepository,
    readingQueueSourceId: String?,
    initialCommentId: String?,
    onEvent: (QuestionEvent) -> Unit,
    onAnswerClick: (item: FeedDisplayItem, destination: NavDestination?) -> Unit,
    onBack: () -> Unit,
) {
    val settings = rememberSettingsStore()
    val executeShareAction = rememberShareActionExecutor()
    val listState = rememberLazyListState()
    val pullToRefreshState = rememberPullToRefreshState()
    val shareText = getShareText(destination, state.title)

    var showShareDialog by rememberSaveable { mutableStateOf(false) }
    var showComments by rememberSaveable(destination.questionId) { mutableStateOf(initialCommentId != null) }
    // 深链锚点：宿主暂存的评论 ID，透传给评论组件做定位
    var pendingCommentId by rememberSaveable(destination.questionId) { mutableStateOf(initialCommentId) }
    var isDetailExpanded by rememberSaveable(destination.questionId) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            QuestionTopBar(
                title = state.title,
                listState = listState,
                onNavigateBack = onBack,
                onOpenLog = { onEvent(QuestionEvent.OpenHistoryLog) },
                onShare = {
                    if (shareText != null) {
                        handleShareAction(destination, settings, executeShareAction) { showShareDialog = true }
                    }
                },
                canShare = shareText != null,
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(QuestionEvent.Refresh) },
            indicator = {
                PullToRefreshDefaults.Indicator(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = innerPadding.calculateTopPadding()),
                    isRefreshing = state.isRefreshing,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    state = pullToRefreshState,
                )
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
        ) {
            PaginatedList(
                items = state.answers,
                onLoadMore = { onEvent(QuestionEvent.LoadMore) },
                isEnd = { state.isAnswersEnded },
                key = { it.stableKey },
                listState = listState,
                modifier = Modifier.padding(innerPadding),
                footer = if (state.isRefreshing) null else ProgressIndicatorFooter,
                topContent = {
                    item {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            val detail = state.detail
                            QuestionHeaderSection(
                                title = state.title,
                                visitCount = detail?.visitCount ?: 0,
                                commentCount = detail?.commentCount ?: 0,
                                followerCount = detail?.followerCount ?: 0,
                                onShowComments = { showComments = true },
                            )
                            if (detail != null) {
                                QuestionBodyHeader(
                                    questionId = destination.questionId,
                                    detail = detail,
                                    contentNodes = state.contentNodes,
                                    allowDetailCollapse = state.allowDetailCollapse,
                                    isExpanded = isDetailExpanded,
                                    onToggleExpanded = { isDetailExpanded = !isDetailExpanded },
                                    currentSort = state.sort,
                                    onEvent = onEvent,
                                )
                            }
                        }
                    }
                },
            ) { item ->
                FeedCard(
                    item = item,
                    readingQueueSourceId = readingQueueSourceId,
                ) { clickedItem, itemDestination ->
                    onAnswerClick(clickedItem, itemDestination)
                }
            }
        }
    }

    CommentRoute(
        showComments = showComments || pendingCommentId != null,
        onDismiss = {
            pendingCommentId = null
            showComments = false
        },
        contentType = CommentContentType.Question,
        contentId = destination.questionId,
        repository = commentRepository,
        initialCommentId = pendingCommentId,
    )

    if (shareText != null) {
        ShareDialog(
            content = destination,
            shareText = shareText,
            showDialog = showShareDialog,
            onDismissRequest = { showShareDialog = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestionTopBar(
    title: String,
    listState: LazyListState,
    onNavigateBack: () -> Unit,
    onOpenLog: () -> Unit,
    onShare: () -> Unit,
    canShare: Boolean,
) {
    TopAppBar(
        title = {
            ScrollAwareTopBarTitle(state = listState, title = title, placeholder = "问题")
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
            }
        },
        actions = {
            IconButton(onClick = onOpenLog) {
                Icon(Icons.Filled.History, contentDescription = "日志")
            }
            IconButton(
                onClick = onShare,
                enabled = canShare,
            ) {
                Icon(Icons.Filled.Share, contentDescription = "分享")
            }
        },
    )
}

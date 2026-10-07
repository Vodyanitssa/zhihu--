package com.zhihuminus.feature.comment

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.feature.comment.components.ChildCommentSheet
import com.zhihuminus.feature.comment.components.CommentInputOverlay
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberUserMessageSink

/**
 * 评论路由组件，负责创建 ViewModel、处理副作用、展示单 Sheet 评论系统。
 *
 * 采用单 ModalBottomSheet + 内部导航架构：
 * - 根评论与子评论在同一 Sheet 内部通过 AnimatedContent 滑动切换，杜绝多层 Sheet 嵌套
 * - 评论输入框作为同一 Sheet 顶层的 CommentInputOverlay，无 Dialog 窗口遮罩冲突
 *
 * @param showComments 是否显示评论
 * @param onDismiss 关闭评论
 * @param contentType 内容类型
 * @param contentId 内容 ID
 * @param repository 评论仓库
 * @param initialCommentId 深链锚点评论 ID（可选）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentRoute(
    showComments: Boolean,
    onDismiss: () -> Unit,
    contentType: CommentContentType,
    contentId: Long,
    repository: CommentRepository,
    initialCommentId: String? = null,
) {
    if (!showComments) return

    val viewModel: CommentViewModel = viewModel(key = "comments_${contentType}_$contentId") {
        CommentViewModel(
            contentType = contentType,
            contentId = contentId,
            repository = repository,
            initialCommentId = initialCommentId,
        )
    }

    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    val userMessages = rememberUserMessageSink()

    val imageViewManager = remember {
        object : ImageViewManager() {
            override fun show(url: String) {
                if (url.isNotBlank()) {
                    submitImages(listOf(url))
                    super.show(url)
                }
            }
        }
    }

    // 处理副作用
    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CommentEffect.ShowMessage -> {
                    userMessages.showShortMessage(effect.message)
                }

                is CommentEffect.OpenExternalUrl -> openExternalUrl(effect.url)
                is CommentEffect.ScrollToTop -> {
                    // 由 CommentScreen 内部处理
                }
            }
        }
    }

    val density = LocalDensity.current
    val positionalThresholdPx = remember(density) { with(density) { 220.dp.toPx() } }
    val velocityThresholdPx = remember(density) { with(density) { 1800.dp.toPx() } }

    val rootSheetState = rememberSaveable(
        saver = SheetState.Saver(
            skipPartiallyExpanded = true,
            positionalThreshold = { positionalThresholdPx },
            velocityThreshold = { velocityThresholdPx },
            confirmValueChange = { true },
            skipHiddenState = false,
        ),
    ) {
        SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { positionalThresholdPx },
            velocityThreshold = { velocityThresholdPx },
            initialValue = SheetValue.Hidden,
        )
    }
    val rootListState = rememberLazyListState()

    // 隔离列表快速滑动到顶部的向下惯性速度，防止在长列表浏览回顶时因动量穿透误触关闭
    val listScrollShield = remember {
        object : NestedScrollConnection {
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                if (available.y > 0) available else Velocity.Zero
        }
    }

    // 评论草稿：sheet 关闭后保留，重开时恢复（按内容维度隔离）
    var commentDraft by rememberSaveable(contentType, contentId) { mutableStateOf("") }

    CompositionLocalProvider(LocalImageViewManager provides imageViewManager) {
        val activeParentId = viewModel.uiState.activeParentId

        // 单 Sheet 弹窗：根评论、子评论与输入浮层统一承载在此 Sheet 内
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.onEvent(CommentEvent.DismissChildComments)
                viewModel.onEvent(CommentEvent.DismissInput)
                onDismiss()
            },
            modifier = Modifier.statusBarsPadding(),
            sheetState = rootSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = null,
            contentWindowInsets = {
                BottomSheetDefaults.windowInsets.only(
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                )
            },
        ) {
            // 返回按键处理：
            // 1. 若处于输入状态，优先关闭输入浮层
            // 2. 若在子评论中，返回根评论
            PlatformBackHandler(enabled = viewModel.uiState.isInputActive) {
                viewModel.onEvent(CommentEvent.DismissInput)
            }
            PlatformBackHandler(enabled = !viewModel.uiState.isInputActive && activeParentId != null) {
                viewModel.onEvent(CommentEvent.DismissChildComments)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                // 页面主体内容（Header + 列表）
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = activeParentId != null,
                            enter = fadeIn() + slideInHorizontally { -it / 2 },
                            exit = fadeOut() + slideOutHorizontally { -it / 2 },
                            modifier = Modifier.align(Alignment.CenterStart),
                        ) {
                            IconButton(
                                onClick = { viewModel.onEvent(CommentEvent.DismissChildComments) },
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "返回评论",
                                )
                            }
                        }

                        Text(
                            text = if (activeParentId != null) "回复" else "评论",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // 评论列表（单 Sheet 内部的内容平滑切换：根评论 <-> 子评论）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .nestedScroll(listScrollShield),
                    ) {
                        AnimatedContent(
                            targetState = activeParentId,
                            transitionSpec = {
                                if (targetState != null) {
                                    // 进入子评论：从右向左滑入
                                    (slideInHorizontally { it } + fadeIn()).togetherWith(
                                        slideOutHorizontally { -it / 3 } + fadeOut(),
                                    )
                                } else {
                                    // 返回根评论：从左向右滑回
                                    (slideInHorizontally { -it / 3 } + fadeIn()).togetherWith(
                                        slideOutHorizontally { it } + fadeOut(),
                                    )
                                }
                            },
                            label = "commentSheetNavigation",
                        ) { parentId ->
                            if (parentId == null) {
                                CommentScreen(
                                    uiState = viewModel.uiState,
                                    onEvent = viewModel::onEvent,
                                    listState = rootListState,
                                    commentInput = commentDraft,
                                )
                            } else {
                                val parentItem = viewModel.uiState.items.find { it.comment.id == parentId }
                                if (parentItem != null) {
                                    ChildCommentSheet(
                                        parentComment = parentItem.comment,
                                        childComments = parentItem.children?.map { it.comment } ?: emptyList(),
                                        isLoading = !parentItem.childrenComplete,
                                        isEnd = viewModel.isChildEnd,
                                        onLoadMore = { viewModel.loadMoreChildComments() },
                                        onEvent = viewModel::onEvent,
                                        highlightCommentId = viewModel.uiState.anchorTargetId
                                            ?.takeIf { parentId == viewModel.uiState.anchorRootId },
                                    )
                                }
                            }
                        }
                    }
                }

                // 评论输入浮层（置于同一个 Sheet 内的最顶层，绝不被任何部件遮挡，无系统 Dialog Window 干扰）
                if (viewModel.uiState.isInputActive) {
                    CommentInputOverlay(
                        onDismiss = { viewModel.onEvent(CommentEvent.DismissInput) },
                        onEvent = viewModel::onEvent,
                        replyToComment = viewModel.uiState.replyToComment,
                        initialDraft = commentDraft,
                        onDraftChange = { commentDraft = it },
                    )
                }
            }
        }
    }

    if (imageViewManager.isShowing) {
        Dialog(
            onDismissRequest = { imageViewManager.dismiss() },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false,
            ),
        ) {
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
}

/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.materialkolor.ktx.harmonize
import com.zhihuminus.R
import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.renderer.LocalImageViewManager
import com.zhihuminus.core.content.renderer.RenderContentNodes
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.feature.imageview.ImageView
import com.zhihuminus.feature.imageview.ImageViewActions
import com.zhihuminus.feature.imageview.ImageViewManager
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.ArticleType
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Question
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.article.ArticleActionsMenu
import com.zhihuminus.ui.article.rememberArticleAnswerNavigationState
import com.zhihuminus.ui.article.rememberBottomBarAvoidingBringIntoViewSpec
import com.zhihuminus.ui.article.voteUpNeutralContentDuo3
import com.zhihuminus.ui.components.AuthorBadge
import com.zhihuminus.ui.components.CollectionDialogComponent
import com.zhihuminus.ui.components.CommentScreenComponent
import com.zhihuminus.ui.components.ExportDialogComponent
import com.zhihuminus.ui.components.VerticalReadingProgressBar
import com.zhihuminus.ui.components.VotersSheet
import com.zhihuminus.util.formatCompactCount
import com.zhihuminus.util.smoothGradient
import com.zhihuminus.viewmodel.ArticleViewModel
import com.zhihuminus.viewmodel.PaginationEnvironment
import com.zhihuminus.viewmodel.addReadHistory
import com.zhihuminus.viewmodel.formatArticleDateTime
import com.zhihuminus.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.flow.collectLatest

/**
 * 文章标题组件。
 */
@Composable
private fun ArticleTitle(
    title: String,
    articleType: ArticleType,
    onNavigateToQuestion: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = modifier.let {
            if (articleType == ArticleType.Answer) {
                it.clickable { onNavigateToQuestion() }
            } else {
                it
            }
        },
    )
}

/**
 * 文章信息组件：作者、发布时间、赞同人数等。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleInfo(
    viewModel: ArticleViewModel,
    articleType: ArticleType,
    onNavigateToPerson: () -> Unit,
    onShowVoters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 作者行
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPerson() },
        ) {
            if (viewModel.authorAvatarSrc.isNotEmpty()) {
                AsyncImage(
                    model = viewModel.authorAvatarSrc,
                    contentDescription = "作者头像",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = viewModel.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (viewModel.authorBadge != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AuthorBadge(badge = viewModel.authorBadge)
                    }
                }
                if (viewModel.authorBio.isNotEmpty()) {
                    Text(
                        text = viewModel.authorBio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 发布时间
        Text(
            "发布于 " + formatArticleDateTime(viewModel.createdAt),
            color = Color.Gray,
            fontSize = 11.sp,
        )
        if (viewModel.createdAt != viewModel.updatedAt) {
            Text(
                "编辑于 " + formatArticleDateTime(viewModel.updatedAt),
                color = Color.Gray,
                fontSize = 11.sp,
            )
        }

        // 赞同人数
        val contentLabel = when (articleType) {
            ArticleType.Answer -> "回答"
            ArticleType.Article -> "文章"
        }
        if (viewModel.votersTotal > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            val text = viewModel.votersSocialText.ifBlank {
                "${formatCompactCount(viewModel.votersTotal)} 人赞同了该$contentLabel"
            }
            val votersTextModifier = if (articleType == ArticleType.Answer) {
                Modifier.clickable { onShowVoters() }
            } else {
                Modifier
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = votersTextModifier,
            )
        }
    }
}

/**
 * 文章正文内容组件。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArticleContentBody(
    viewModel: ArticleViewModel,
    articleType: ArticleType,
    contentNodes: List<ContentNode>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // 话题标签（仅文章）
        if (articleType == ArticleType.Article && viewModel.topics.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                viewModel.topics.forEach { topic ->
                    androidx.compose.material3.FilterChip(
                        selected = false,
                        onClick = { /* 导航到话题 */ },
                        label = { Text("# ${topic.name}") },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 认可标签
        val endorsements = viewModel.endorsements
        if (endorsements.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                endorsements.forEach { endorsement ->
                    AnswerEndorsementChip(endorsement)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 正文内容
        RenderContentNodes(nodes = contentNodes)
    }
}

/**
 * 文章底栏组件：赞同/反对、收藏、评论、更多菜单。
 */
@Composable
private fun ArticleBottomBar(
    viewModel: ArticleViewModel,
    environment: PaginationEnvironment,
    onShowCollectionDialog: () -> Unit,
    onShowComments: () -> Unit,
    onShowActionsMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .padding(
                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp,
            ).padding(horizontal = 16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 赞同/反对按钮
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 赞同
            AnimatedVisibility(
                visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Up,
            ) {
                val upBgColor by animateColorAsState(
                    targetValue = if (viewModel.voteUpState == VoteUpState.Up) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                )
                val upContentColor by animateColorAsState(
                    targetValue = if (viewModel.voteUpState == VoteUpState.Up) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(upBgColor)
                        .clickable {
                            viewModel.toggleVoteUp(
                                environment,
                                if (viewModel.voteUpState == VoteUpState.Up) VoteUpState.Neutral else VoteUpState.Up,
                            )
                        }.padding(6.dp, 8.dp, 12.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_vote_up_24dp),
                        contentDescription = "赞同",
                        tint = upContentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = viewModel.voteUpCount.toString(),
                        color = upContentColor,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Neutral) {
                Spacer(modifier = Modifier.width(4.dp))
            }

            // 反对
            AnimatedVisibility(
                visible = viewModel.voteUpState == VoteUpState.Neutral || viewModel.voteUpState == VoteUpState.Down,
            ) {
                val downBgColor by animateColorAsState(
                    targetValue = if (viewModel.voteUpState == VoteUpState.Down) voteUpNeutralContentDuo3() else MaterialTheme.colorScheme.surfaceContainer,
                )
                val downContentColor by animateColorAsState(
                    targetValue = if (viewModel.voteUpState == VoteUpState.Down) Color.White else MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(downBgColor)
                        .clickable {
                            viewModel.toggleVoteUp(
                                environment,
                                if (viewModel.voteUpState == VoteUpState.Down) VoteUpState.Neutral else VoteUpState.Down,
                            )
                        }.padding(6.dp, 8.dp, 8.dp, 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AnimatedVisibility(visible = viewModel.voteUpState != VoteUpState.Down) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_vote_down_24dp),
                        contentDescription = "反对",
                        tint = downContentColor,
                        modifier = Modifier.size(24.dp),
                    )
                    AnimatedVisibility(visible = viewModel.voteUpState == VoteUpState.Down) {
                        Row {
                            Text(
                                text = "反对",
                                color = downContentColor,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        // 操作按钮：收藏、评论、更多
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = onShowCollectionDialog,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (viewModel.isFavorited) {
                        Color(0xFFF57C00).harmonize(MaterialTheme.colorScheme.primary)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    contentColor = if (viewModel.isFavorited) {
                        Color.White.copy(alpha = 0.87f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                ),
            ) {
                Icon(
                    if (viewModel.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = "收藏",
                )
            }

            Button(
                onClick = onShowComments,
                contentPadding = PaddingValues(start = 8.dp, end = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${viewModel.commentCount}", style = MaterialTheme.typography.titleMedium)
            }

            IconButton(
                onClick = onShowActionsMenu,
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
            }
        }
    }
}

/**
 * 文章/回答详情页。
 *
 * 页面负责加载知乎回答或专栏文章，展示标题、作者、正文、附件视频、评论入口、分享/复制/朗读/浏览器打开等底部操作，
 * 正文主路径使用 Compose Markdown 渲染。
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalLayoutApi::class,
)
@Composable
fun ArticleScreen(
    article: Article,
    viewModel: ArticleViewModel,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val articleHost = rememberArticleHost()

    val scrollState = rememberScrollState()
    val articleSettings = rememberArticleScreenSettingsState()
    val userMessages = rememberUserMessageSink()
    val density = LocalDensity.current
    var showComments by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVoters by rememberSaveable(article.type, article.id) { mutableStateOf(false) }

    // 图片查看管理器。
    val imageViewManager = remember { ImageViewManager() }
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()

    val contentNodes = AstParser.parseContent(viewModel.content)
    // 从文章 AST 中提取图片列表，提交给 manager。
    LaunchedEffect(viewModel.content) {
        imageViewManager.submitImages(
            contentNodes
                .filterIsInstance<ContentNode.Image>()
                .filter { it.url.isNotBlank() }
                .map { it.url },
        )
    }

    val sharedData = if (article.type == ArticleType.Answer) {
        environment.articleAnswerSwitchState()
    } else {
        null
    }
    val answerNavigationState = rememberArticleAnswerNavigationState(
        switchState = sharedData,
        viewModel = viewModel,
        navigator = navigator,
        navController = articleHost?.articleNavController,
        answerSwitchMode = articleSettings.answerSwitchMode,
        readingQueueSourceId = article.readingQueueSourceId,
    )
    val hapticFeedback = LocalHapticFeedback.current

    // 计算底栏高度用于滚动行为
    val bottomBarHeightPx = remember { mutableStateOf(0f) }
    val articleBringIntoViewSpec = rememberBottomBarAvoidingBringIntoViewSpec(bottomBarHeightPx.value)

    LaunchedEffect(Unit) {
        environment.addReadHistory(
            contentToken = article.id.toString(),
            contentTypeName = article.type.name.lowercase(),
        )
    }

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.value }.collectLatest { currentScroll ->
            if (viewModel.rememberedScrollYSync) {
                viewModel.rememberedScrollY = currentScroll
            }
            if (currentScroll == viewModel.rememberedScrollY && scrollState.maxValue != Int.MAX_VALUE) {
                viewModel.rememberedScrollYSync = true
            }
        }
    }

    LaunchedEffect(article.id) {
        answerNavigationState.prepareArticle()
        viewModel.loadArticle(environment)
        viewModel.loadCollections(environment)
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun MainContent() {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                // 简单顶栏：返回按钮始终显示
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                            bottom = 8.dp,
                        ).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (articleHost != null) {
                                articleHost.articleNavController.popBackStack()
                            } else {
                                navigator.onNavigateBack()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            bottomBar = {
                // 底栏始终显示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { bottomBarHeightPx.value = it.height.toFloat() },
                ) {
                    ArticleBottomBar(
                        viewModel = viewModel,
                        environment = environment,
                        onShowCollectionDialog = { showCollectionDialog = true },
                        onShowComments = { showComments = true },
                        onShowActionsMenu = { showActionsMenu = true },
                    )
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalBringIntoViewSpec provides articleBringIntoViewSpec,
                LocalImageViewManager provides imageViewManager,
            ) {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(innerPadding)
                            .padding(top = 8.dp),
                    ) {
                        // 标题
                        ArticleTitle(
                            title = viewModel.title,
                            articleType = article.type,
                            onNavigateToQuestion = {
                                navigator.onNavigate(
                                    Question(viewModel.questionId, viewModel.title),
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 文章信息
                        ArticleInfo(
                            viewModel = viewModel,
                            articleType = article.type,
                            onNavigateToPerson = {
                                navigator.onNavigate(
                                    com.zhihuminus.navigation.Person(
                                        id = viewModel.authorId,
                                        urlToken = viewModel.authorUrlToken,
                                        name = viewModel.authorName,
                                    ),
                                )
                            },
                            onShowVoters = {
                                showVoters = true
                                if (viewModel.voters.isEmpty()) {
                                    viewModel.loadMoreVoters(environment, reset = true)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 文章正文
                        if (viewModel.content.isNotEmpty() || viewModel.attachment != null) {
                            ArticleContentBody(
                                viewModel = viewModel,
                                articleType = article.type,
                                contentNodes = contentNodes,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 80.dp),
                            )
                        }
                    }
                    // 状态栏渐变遮罩。
                    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(statusBarHeight + 16.dp)
                            .background(
                                Brush.verticalGradient(smoothGradient(surfaceColor, 0.8f)),
                            ),
                    ) {}
                }
            }
        }
    }

    val nav = answerNavigationState.answerNavigator
    val progressBarTopPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
    val progressBarBottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 96.dp

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 渲染主内容（移除了 overscroll 跳转逻辑）
        MainContent()

        VerticalReadingProgressBar(
            scrollState = scrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(
                    top = progressBarTopPadding,
                    bottom = progressBarBottomPadding,
                    end = 2.dp,
                ),
        )

        // 跳转按钮（保留手动跳转功能）
        if (article.type == ArticleType.Answer && articleSettings.buttonSkipAnswer) {
            val isAtTop by remember(scrollState) {
                derivedStateOf { scrollState.value == 0 }
            }
            var fabClickCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(fabClickCount) {
                if (fabClickCount > 0) {
                    answerNavigationState.navigateToNext()
                    fabClickCount = 0
                }
            }
        }

        // 图片预览 overlay。
        ImageView(
            manager = imageViewManager,
            actions = ImageViewActions(
                onSave = { saveImage(it) },
                onShare = { shareImage(it) },
                onOpenInBrowser = { openExternalUrl(it) },
            ),
        )
    }

    // 全屏菜单
    ArticleActionsMenu(
        article = article,
        viewModel = viewModel,
        answerQueueFallbackProvider = sharedData?.navigator?.let { answerNavigator ->
            { limit -> answerNavigator.remainingAnswersSnapshot(article.id, limit) }
        },
        showMenu = showActionsMenu,
        onDismissRequest = { showActionsMenu = false },
        onExportRequest = { showExportDialog = true },
    )

    PlatformBackHandler(showActionsMenu) {
        showActionsMenu = false
    }

    // 收藏夹对话框
    CollectionDialogComponent(
        showDialog = showCollectionDialog,
        onDismiss = { showCollectionDialog = false },
        collections = viewModel.collections,
        onLoadCollections = { viewModel.loadCollections(environment) },
        onToggleFavorite = { collection ->
            viewModel.toggleFavorite(collection.id, collection.isFavorited, environment)
        },
        onCreateCollection = { title, description, isPublic ->
            viewModel.createNewCollection(environment, title, description, isPublic)
        },
    )

    // 评论
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = article,
        isZhPlusAuthorContent = article.type == ArticleType.Answer &&
            viewModel.authorId == DataHolder.ZH_PLUS_AUTHOR_USER_ID,
    )

    // 投票者列表
    VotersSheet(
        show = showVoters,
        title = "${formatCompactCount(viewModel.votersTotal)} 人赞同了该回答",
        voters = viewModel.voters,
        isLoading = viewModel.votersLoading,
        errorMessage = viewModel.votersError,
        canLoadMore = viewModel.votersNextUrl != null,
        onDismissRequest = { showVoters = false },
        onLoadMore = { viewModel.loadMoreVoters(environment) },
        onRetry = { viewModel.loadMoreVoters(environment, reset = viewModel.voters.isEmpty()) },
        onNavigate = { person ->
            showVoters = false
            navigator.onNavigate(person)
        },
    )

    // 导出对话框
    ExportDialogComponent(
        showDialog = showExportDialog,
        onDismiss = { showExportDialog = false },
        onExportImage = { includeComments, onComplete ->
            viewModel.exportImage(environment, includeComments, onComplete)
        },
    )
}

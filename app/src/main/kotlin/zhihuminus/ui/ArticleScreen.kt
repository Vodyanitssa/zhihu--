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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.materialkolor.ktx.harmonize
import com.zhihuminus.R
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.VoteUpState
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.ArticleType
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Topic
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.platform.rememberExternalUrlOpener
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.renderer.AstParser
import com.zhihuminus.renderer.ContentNode
import com.zhihuminus.renderer.RenderContentNodes
import com.zhihuminus.ui.article.ArticleActionsMenu
import com.zhihuminus.ui.article.CachedAnswerPreview
import com.zhihuminus.ui.article.rememberArticleAnswerNavigationState
import com.zhihuminus.ui.article.rememberArticleBottomBarState
import com.zhihuminus.ui.article.rememberArticleTopBarState
import com.zhihuminus.ui.article.rememberBottomBarAvoidingBringIntoViewSpec
import com.zhihuminus.ui.article.voteUpNeutralContentDuo3
import com.zhihuminus.ui.components.AnswerHorizontalOverscroll
import com.zhihuminus.ui.components.AnswerVerticalOverscroll
import com.zhihuminus.ui.components.AuthorBadge
import com.zhihuminus.ui.components.CollectionDialogComponent
import com.zhihuminus.ui.components.CommentScreenComponent
import com.zhihuminus.ui.components.ExportDialogComponent
import com.zhihuminus.ui.components.VerticalReadingProgressBar
import com.zhihuminus.ui.components.VotersSheet
import com.zhihuminus.ui.components.ZhihuTwoRowsTopAppBar
import com.zhihuminus.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.zhihuminus.ui.image.ImagePreview
import com.zhihuminus.ui.image.ImagePreviewActions
import com.zhihuminus.util.formatCompactCount
import com.zhihuminus.util.smoothGradient
import com.zhihuminus.viewmodel.ArticleViewModel
import com.zhihuminus.viewmodel.addReadHistory
import com.zhihuminus.viewmodel.formatArticleDateTime
import com.zhihuminus.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

private const val SCROLL_THRESHOLD = 10 // 滑动阈值，单位为dp
private val ScrollThresholdDp = SCROLL_THRESHOLD.dp

/**
 * 文章/回答详情页。
 *
 * 页面负责加载知乎回答或专栏文章，展示标题、作者、正文、附件视频、评论入口、分享/复制/朗读/浏览器打开等底部操作，
 * 正文主路径使用 Compose Markdown 渲染。回答页还承载同题回答切换手势和对应转场状态，因此改动时要同时关注
 * `answerSwitchMode`、`buttonSkipAnswer`、`autoHideArticleBottomBar`、`titleAutoHide`
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalFoundationApi::class,
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
    val backStackEntry by articleHost?.articleNavController?.currentBackStackEntryAsState()
        ?: remember { mutableStateOf(null) }

    val scrollState = rememberScrollState()
    val articleSettings = rememberArticleScreenSettingsState()
    val userMessages = rememberUserMessageSink()
    val density = LocalDensity.current
    val effectiveScrollMaxValue by remember(0) {
        derivedStateOf {
            if (scrollState.maxValue == Int.MAX_VALUE) {
                Int.MAX_VALUE
            } else {
                (scrollState.maxValue).coerceAtLeast(0)
            }
        }
    }
    var showComments by rememberSaveable(article.type, article.id) { mutableStateOf(false) }
    var showCollectionDialog by remember { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showVoters by rememberSaveable(article.type, article.id) { mutableStateOf(false) }

    // 图片预览状态。
    // 使用 -1 表示“未在预览”。点击图片时直接记录其在 articleImages 中的 index，
    // 避免通过 URL 反向查找带来的问题（多张图片可能使用相同 URL）。
    var imagePreviewIndex by remember { mutableIntStateOf(-1) }
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()

    val contentNodes = AstParser.ParseContent(viewModel.content)
    // 从文章 AST 中提取图片列表，用于 gallery 预览。
    // 顺序与 Renderer 中的 imageIndex 计数严格保持一致。
    val articleImages = remember(viewModel.content) {
        contentNodes
            .filterIsInstance<ContentNode.Image>()
            .filter { it.url.isNotBlank() }
            .map { ImagePreview(url = it.url) }
    }

    val topBarState = rememberArticleTopBarState(
        scrollState = scrollState,
        autoHide = articleSettings.isTitleAutoHide,
    )
    val bottomBarState = rememberArticleBottomBarState(
        scrollState = scrollState,
        autoHide = articleSettings.autoHideArticleBottomBar,
        scrollDeltaThreshold = with(density) { ScrollThresholdDp.toPx() },
        showSlot = backStackEntry?.hasRoute(Article::class) == true || articleHost == null,
        navigationBarHeightPx = density.run {
            WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
                .toPx()
                .coerceAtLeast(0f)
        },
    )
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

    val articleBringIntoViewSpec = rememberBottomBarAvoidingBringIntoViewSpec(
        bottomBarState.obscuredHeightPx,
    )
    LaunchedEffect(article.id) {
        answerNavigationState.prepareArticle()
        viewModel.loadArticle(environment)
        viewModel.loadCollections(environment)
    }

    LaunchedEffect(article.type, article.id, viewModel.content) {
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent() {
        val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
        // 记录历史最大滚动范围，避免顶栏展开/收起时 maxValue 短暂变化导致 scrollBehavior 抖动。
        var scrollStateMaxValue by remember { mutableIntStateOf(0) }
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.maxValue }.collectLatest { maxValue ->
                if (maxValue != Int.MAX_VALUE) {
                    scrollStateMaxValue = max(maxValue, scrollStateMaxValue)
                }
            }
        }
        Scaffold(
            modifier = Modifier
                .fillMaxSize(),
            topBar =
                {
                    Box(
                        modifier = Modifier
                            .onSizeChanged {
                                topBarState.heightPx = it.height.toFloat()
                            }.graphicsLayer {
                                translationY = topBarState.offset.value
                                alpha =
                                    if (topBarState.heightPx > 0f) 1f + (topBarState.offset.value / topBarState.heightPx) else 1f
                            },
                    ) {
                        ZhihuTwoRowsTopAppBar(
                            navigationIcon = {
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
                            },
                            actions = {
                                IconButton(
                                    onClick = { showActionsMenu = true },
                                ) {
                                    Icon(
                                        Icons.Filled.MoreVert,
                                        contentDescription = "更多选项",
                                    )
                                }
                            },
                            title = { expanded ->
                                Text(
                                    text = viewModel.title,
                                    modifier = Modifier
                                        .padding(if (expanded) PaddingValues(end = 16.dp) else PaddingValues())
                                        .let {
                                            if (article.type == ArticleType.Answer) {
                                                it.clickable {
                                                    navigator.onNavigate(
                                                        Question(
                                                            viewModel.questionId,
                                                            viewModel.title,
                                                        ),
                                                    )
                                                }
                                            } else {
                                                it
                                            }
                                        },
                                    maxLines = if (expanded) Int.MAX_VALUE else 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                            subtitle = { expanded ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(
                                            if (expanded) {
                                                PaddingValues(vertical = 16.dp)
                                            } else {
                                                PaddingValues(
                                                    top = 2.dp,
                                                    bottom = 8.dp,
                                                )
                                            },
                                        ).padding(end = 16.dp)
                                        .fillMaxWidth()
                                        .clickable {
                                            navigator.onNavigate(
                                                com.zhihuminus.navigation.Person(
                                                    id = viewModel.authorId,
                                                    urlToken = viewModel.authorUrlToken,
                                                    name = viewModel.authorName,
                                                ),
                                            )
                                        },
                                ) {
                                    if (viewModel.authorAvatarSrc.isNotEmpty()) {
                                        AsyncImage(
                                            model = viewModel.authorAvatarSrc,
                                            contentDescription = "作者头像",
                                            modifier = Modifier
                                                .size(if (expanded) 40.dp else 20.dp)
                                                .clip(CircleShape),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(if (expanded) 40.dp else 20.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceVariant),
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(if (expanded) 8.dp else 4.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = viewModel.authorName,
                                                style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                                                color = if (expanded) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false),
                                            )
                                            if (viewModel.authorBadge != null) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                AuthorBadge(
                                                    badge = viewModel.authorBadge,
                                                    compact = !expanded,
                                                )
                                            }
                                        }
                                        if (viewModel.authorBio.isNotEmpty() && expanded) {
                                            Text(
                                                text = viewModel.authorBio,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            },
                            scrollBehavior = if (scrollStateMaxValue > 0) scrollBehavior else null,
                            colors = TopAppBarDefaults.topAppBarColors().copy(
                                scrolledContainerColor = if (MaterialTheme.colorScheme.surfaceContainer != MaterialTheme.colorScheme.background) {
                                    MaterialTheme.colorScheme.surfaceContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                            ),
                        )
                    }
                },
            bottomBar = @Composable {
                // 防止在导航动画和预测性返回手势过程中，底部操作栏闪烁。
                @Composable
                fun ActionBarContent() {
                    // ── 药丸式动画投票与操作区 ──────────────────────────
                    Row(
                        modifier = Modifier
                            .padding(
                                bottom = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 16.dp,
                            ).padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
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

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(end = 4.dp),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            IconButton(
                                onClick = { showCollectionDialog = true },
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
                                onClick = { showComments = true },
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
                        }
                    }
                }

                Column {
                    if (bottomBarState.showSlot) {
                        Box(
                            modifier = Modifier
                                .onSizeChanged { bottomBarState.heightPx = it.height.toFloat() }
                                .graphicsLayer {
                                    translationY = bottomBarState.offset.value
                                    alpha = if (bottomBarState.heightPx > 0f) {
                                        1f - (bottomBarState.offset.value / bottomBarState.heightPx)
                                    } else {
                                        1f
                                    }
                                },
                        ) {
                            ActionBarContent()
                        }
                    }
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(LocalBringIntoViewSpec provides articleBringIntoViewSpec) {
                Box {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .verticalScroll(scrollState)
                            .padding(innerPadding)
                            .padding(top = 8.dp),
                    ) {
                        @Suppress("UnusedReceiverParameter") // 确保竖式布局
                        @Composable
                        fun ColumnScope.DateTexts() {
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
                        }

                        @Composable
                        fun ArticleVotersSocialCredit() {
                            val contentLabel = when (article.type) {
                                ArticleType.Answer -> "回答"
                                ArticleType.Article -> "文章"
                            }
                            val hasVotersSocialCredit = viewModel.votersTotal > 0
                            if (!hasVotersSocialCredit) return
                            Spacer(modifier = Modifier.height(8.dp))
                            val text = viewModel.votersSocialText.ifBlank {
                                "${formatCompactCount(viewModel.votersTotal)} 人赞同了该$contentLabel"
                            }
                            val votersTextModifier = if (article.type == ArticleType.Answer) {
                                Modifier.clickable {
                                    showVoters = true
                                    if (viewModel.voters.isEmpty()) {
                                        viewModel.loadMoreVoters(environment, reset = true)
                                    }
                                }
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

                        if (viewModel.content.isNotEmpty() || viewModel.attachment != null) {
                            if (article.type == ArticleType.Article && viewModel.topics.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    viewModel.topics.forEach { topic ->
                                        androidx.compose.material3.FilterChip(
                                            selected = false,
                                            onClick = { navigator.onNavigate(Topic(topic.id, topic.name)) },
                                            label = { Text("# ${topic.name}") },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                            val hasPinnedDate = articleSettings.pinAnswerDate
                            val hasSocialCredit = viewModel.votersTotal > 0
                            val endorsements = viewModel.endorsements
                            val hasEndorsements = endorsements.isNotEmpty()
                            if (hasPinnedDate || hasSocialCredit || hasEndorsements) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalAlignment = Alignment.Start,
                                ) {
                                    if (hasPinnedDate) {
                                        DateTexts()
                                    }
                                    ArticleVotersSocialCredit()
                                    if (hasEndorsements) {
                                        if (hasPinnedDate || hasSocialCredit) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            endorsements.forEach { endorsement ->
                                                AnswerEndorsementChip(endorsement)
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            RenderContentNodes(
                                nodes = contentNodes,
                                onImageClick = { _, index ->
                                    imagePreviewIndex = index
                                },
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
        // 根据模式渲染
        when (article.type) {
            ArticleType.Answer if articleSettings.answerSwitchMode == "vertical" -> {
                AnswerVerticalOverscroll(
                    previousAnswer = nav?.previousAnswer,
                    nextAnswer = nav?.nextAnswer,
                    onNavigatePrevious = answerNavigationState::navigateToPrevious,
                    onNavigateNext = answerNavigationState::navigateToNext,
                    isAtTop = { scrollState.value == 0 },
                    isAtBottom = { scrollState.value >= effectiveScrollMaxValue },
                    scrollState = scrollState,
                    answerSwitchSensitivity = articleSettings.answerSwitchSensitivity,
                ) {
                    MainContent()
                }
            }

            ArticleType.Answer if articleSettings.answerSwitchMode == "horizontal" -> {
                AnswerHorizontalOverscroll(
                    canGoPrevious = nav?.previousAnswer != null,
                    canGoNext = nav?.nextAnswer != null,
                    onNavigatePrevious = answerNavigationState::navigateToPrevious,
                    onNavigateNext = answerNavigationState::navigateToNext,
                    previousContent = nav?.previousAnswer?.let { cached ->
                        { CachedAnswerPreview(cached) }
                    },
                    nextContent = nav?.nextAnswer?.let { cached ->
                        { CachedAnswerPreview(cached) }
                    },
                    answerSwitchSensitivity = articleSettings.answerSwitchSensitivity,
                ) {
                    MainContent()
                }
            }

            else -> {
                MainContent()
            }
        }

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

        // 跳转按钮需要压在问题区和回答区之上。
        if (article.type == ArticleType.Answer && articleSettings.buttonSkipAnswer) {
            val isAtTop by remember(scrollState) {
                derivedStateOf { scrollState.value == 0 }
            }
            val showSkipButton = !articleSettings.autoHideSkipAnswerButton || bottomBarState.isScrollingUp || isAtTop
            val skipButtonAlpha by animateFloatAsState(
                targetValue = if (showSkipButton) 1f else 0f,
                animationSpec = tween(200),
                label = "skipButtonAlpha",
            )
            var fabClickCount by remember { mutableIntStateOf(0) }
            LaunchedEffect(fabClickCount) {
                if (fabClickCount > 0) {
                    delay(350.milliseconds)
                    if (showSkipButton) {
                        answerNavigationState.navigateToNext()
                    }
                    fabClickCount = 0
                }
            }
        }

        // 图片预览 overlay。
        // 位于主内容之上，独立管理预览状态，不影响文章重组。
        if (imagePreviewIndex >= 0 && articleImages.isNotEmpty()) {
            ImagePreview(
                images = articleImages,
                initialIndex = imagePreviewIndex,
                actions = ImagePreviewActions(
                    onSave = { saveImage(it) },
                    onShare = { shareImage(it) },
                    onOpenInBrowser = { openExternalUrl(it) },
                ),
                onDismiss = { imagePreviewIndex = -1 },
            )
        }
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

    PlatformBackHandler(imagePreviewIndex >= 0) {
        imagePreviewIndex = -1
    }

    // 使用新的收藏夹对话框组件
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

    CommentScreenComponent(
        showComments = showComments,
        onDismiss = { showComments = false },
        content = article,
        isZhPlusAuthorContent = article.type == ArticleType.Answer &&
            viewModel.authorId == DataHolder.ZH_PLUS_AUTHOR_USER_ID,
    )
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
        onExportHtml = { includeAppAttribution, onComplete ->
            viewModel.exportToHtml(environment, includeAppAttribution, onComplete)
        },
        onExportImage = { includeAppAttribution, onComplete ->
            viewModel.exportToImage(environment, includeAppAttribution, onComplete)
        },
        onExportMarkdown = {
            viewModel.exportToClipboard(environment)
        },
        onExportImageWithComments = { commentCount, includeAppAttribution, onComplete ->
            viewModel.exportToImageWithComments(environment, commentCount, includeAppAttribution, onComplete)
        },
    )
}

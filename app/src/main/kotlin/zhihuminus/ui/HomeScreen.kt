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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MarkUnreadChatAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zhihuminus.data.Feed
import com.zhihuminus.data.ZHIHU_ME_URL
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.ZhihuMeNotifications
import com.zhihuminus.data.target
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.WritePin
import com.zhihuminus.notification.rememberNotificationSettingsStore
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberAppPrivateDirectory
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.FeedPullToRefresh
import com.zhihuminus.ui.components.MyModalBottomSheet
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.ui.subscreens.DEFAULT_FAB_OPACITY
import com.zhihuminus.ui.subscreens.PREF_FAB_OPACITY
import com.zhihuminus.viewmodel.feed.BaseFeedViewModel
import com.zhihuminus.viewmodel.feed.HomeFeedViewModel
import com.zhihuminus.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

const val PREFERENCE_NAME = "com.zhihuminus_preferences"

/**
 * 首页信息流页面。
 *
 * 页面顶部承载搜索、账号入口等高频操作，主体是可分页的推荐信息流，底部可按设置显示可拖动刷新 FAB。
 * 设计上首页同时响应推荐算法、账号未读通知数等状态，因此 UI 改动时要同时检查
 * `recommendationMode`、`showRefreshFab` 和账号面板相关路径。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
) {
    val navigator = LocalNavigator.current
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = true)
    val settings = rememberSettingsStore()
    val appPrivateDirectory = rememberAppPrivateDirectory()
    val notificationSettings = rememberNotificationSettingsStore()
    val userMessages = rememberUserMessageSink()
    val lifecycleOwner = LocalLifecycleOwner.current

    val autoRefreshOnStartup = settings.getBoolean(AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY, true)
    val showUnreadBadge = notificationSettings.getUnreadBadgeEnabled()
    var showAccountBottomSheet by remember { mutableStateOf(false) }
    var showCreateMenu by remember { mutableStateOf(false) }
    val createMenuBlurRadius by animateDpAsState(
        targetValue = if (showCreateMenu) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 200),
        label = "createMenuBlurRadius",
    )

    val startupCacheFile = remember(appPrivateDirectory) {
        Path(appPrivateDirectory, homeFeedStartupCacheFileName())
    }

    val account = rememberAccountSettingsAccountState().value
    if (account.login && !account.hasRequiredCookie) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Cookie 不完整") },
            text = { Text("当前登录信息缺少必要的 Cookie d_c0，请重新登录。") },
            confirmButton = {
                TextButton(onClick = { paginationEnvironment.requestLogin() }) {
                    Text("重新登录")
                }
            },
        )
    }

    val viewModel: BaseFeedViewModel = viewModel { HomeFeedViewModel() }
    val readingQueueSourceId = "home:WEB"

    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        when (
            topLevelReselectAction(
                triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
                isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
            )
        ) {
            TopLevelReselectAction.Refresh -> viewModel.refresh(paginationEnvironment)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> {}
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    // 未读通知数
    var unreadCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            unreadCount = paginationEnvironment
                .fetchJson(ZHIHU_ME_URL, "")
                ?.let { ZhihuJson.decodeJson<ZhihuMeNotifications>(it) }
                ?.totalCount ?: 0
        } catch (_: Exception) {
            // 忽略错误
        }
    }

    val latestLoadedDisplayItems = viewModel.latestLoadedDisplayItems.value
    LaunchedEffect(latestLoadedDisplayItems) {
        if (latestLoadedDisplayItems.isNotEmpty()) {
            encodeHomeFeedStartupSnapshot(latestLoadedDisplayItems)?.let { serialized ->
                withContext(Dispatchers.Default) {
                    runCatching {
                        SystemFileSystem.sink(startupCacheFile).buffered().use { it.writeString(serialized) }
                    }
                }
            }
        }
    }

    // 初始加载
    LaunchedEffect(account.login, autoRefreshOnStartup) {
        if (!account.login &&
            settings.getBoolean("loginForRecommendation", true)
        ) {
            if (!paginationEnvironment.requestLogin()) {
                userMessages.showShortMessage("当前平台暂不支持登录")
            }
        } else if (viewModel.displayItems.isEmpty()) {
            val cachedItems = if (autoRefreshOnStartup) {
                emptyList()
            } else {
                withContext(Dispatchers.Default) {
                    runCatching {
                        if (SystemFileSystem.exists(startupCacheFile)) {
                            SystemFileSystem.source(startupCacheFile).buffered().use { source ->
                                decodeHomeFeedStartupSnapshot(source.readString())
                            }
                        } else {
                            emptyList()
                        }
                    }.getOrDefault(emptyList())
                }
            }
            if (viewModel.displayItems.isEmpty() && cachedItems.isNotEmpty()) {
                viewModel.addDisplayItems(cachedItems)
            } else if (viewModel.displayItems.isEmpty()) {
                // 只在第一次加载时刷新，这样可以避免在返回时刷新
                viewModel.refresh(paginationEnvironment)
            }
        }
    }

    // 显示错误信息
    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .blur(createMenuBlurRadius),
            topBar = {
                Box {
                    Surface(
                        modifier = Modifier
                            .height(
                                WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp + 32.dp,
                            ).fillMaxWidth(),
                    ) { }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                            .padding(16.dp, 8.dp, 16.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            onClick = {
                                navigator.onNavigate(
                                    Search(query = ""),
                                )
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(start = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "搜索",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "搜索",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f),
                                )

                                IconButton(
                                    onClick = { showAccountBottomSheet = true },
                                    modifier = Modifier
                                        .size(64.dp),
                                ) {
                                    Box(Modifier.padding(12.dp)) {
                                        BadgedBox(
                                            badge = {
                                                if (showUnreadBadge && unreadCount > 0) {
                                                    Badge { }
                                                }
                                            },
                                        ) {
                                            val avatarUrl = account.avatarUrl
                                            if (avatarUrl != null) {
                                                AsyncImage(
                                                    model = avatarUrl,
                                                    contentDescription = "账号",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .border(
                                                            0.5.dp,
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                            CircleShape,
                                                        ).clip(CircleShape),
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Default.AccountCircle,
                                                    contentDescription = "账号",
                                                    tint = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.size(40.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
        ) { scaffoldPadding ->
            if (showAccountBottomSheet) {
                MyModalBottomSheet(
                    onDismissRequest = { showAccountBottomSheet = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    AccountSettingScreen(
                        innerPadding = PaddingValues(0.dp),
                        unreadCount = unreadCount,
                        showUnreadBadge = showUnreadBadge,
                        onDismissRequest = { showAccountBottomSheet = false },
                    )
                }
            }

            FeedPullToRefresh(viewModel, PaddingValues(top = scaffoldPadding.calculateTopPadding())) {
                PaginatedList(
                    items = viewModel.displayItems,
                    listState = listState,
                    modifier = Modifier,
                    contentPadding = PaddingValues(
                        top = scaffoldPadding.calculateTopPadding() + 8.dp,
                        bottom = innerPadding.calculateBottomPadding(),
                    ),
                    onLoadMore = { viewModel.loadMore(paginationEnvironment) },
                    footer = ProgressIndicatorFooter,
                    key = { item -> item.stableKey },
                ) { item ->
                    FeedCard(
                        item,
                        readingQueueSourceId = readingQueueSourceId,
                        thumbnailUrl = when (val target = item.feed?.target) {
                            is Feed.AnswerTarget -> target.thumbnail
                            else -> null
                        },
                    ) { clickedItem, destination ->
                        val feed = clickedItem.feed
                        if (feed != null) {
//                            DataHolder.putFeed(feed)
                            (viewModel as? HomeFeedViewModel)
                                ?.onUiContentClick(paginationEnvironment, feed, clickedItem)
                        }
                        if (destination != null) {
                            navigator.onNavigate(destination)
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showCreateMenu,
            enter = fadeIn(animationSpec = tween(durationMillis = 120)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.16f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showCreateMenu = false
                    },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            horizontalAlignment = Alignment.End,
        ) {
            AnimatedVisibility(
                visible = showCreateMenu,
                enter = fadeIn(animationSpec = tween(durationMillis = 120)) +
                        scaleIn(
                            initialScale = 0.92f,
                            transformOrigin = TransformOrigin(1f, 1f),
                            animationSpec = tween(durationMillis = 180),
                        ) +
                        slideInVertically(animationSpec = tween(durationMillis = 180)) { it / 8 },
                exit = fadeOut(animationSpec = tween(durationMillis = 90)) +
                        scaleOut(
                            targetScale = 0.96f,
                            transformOrigin = TransformOrigin(1f, 1f),
                            animationSpec = tween(durationMillis = 120),
                        ) +
                        slideOutVertically(animationSpec = tween(durationMillis = 120)) { it / 8 },
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        modifier = Modifier
                            .width(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 6.dp,
                    ) {
                        Column {
                            DropdownMenuItem(
                                modifier = Modifier,
                                text = { Text("提问题") },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Default.HelpOutline, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    userMessages.showShortMessage("正在施工")
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier,
                                text = { Text("写回答") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    userMessages.showShortMessage("正在施工")
                                },
                            )
                            DropdownMenuItem(
                                modifier = Modifier,
                                text = { Text("发想法") },
                                leadingIcon = {
                                    Icon(Icons.Default.MarkUnreadChatAlt, contentDescription = null)
                                },
                                onClick = {
                                    showCreateMenu = false
                                    navigator.onNavigate(WritePin())
                                },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            val createFabOpacity = remember(settings) {
                settings.getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY).coerceIn(10, 100) / 100f
            }
            FloatingActionButton(
                modifier = Modifier,
                onClick = { showCreateMenu = !showCreateMenu },
                shape = CircleShape,
                containerColor = FloatingActionButtonDefaults.containerColor.copy(alpha = createFabOpacity),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = createFabOpacity),
                elevation = if (createFabOpacity < 1f) {
                    FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                } else {
                    FloatingActionButtonDefaults.elevation()
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "创作")
            }
        }
    }
}

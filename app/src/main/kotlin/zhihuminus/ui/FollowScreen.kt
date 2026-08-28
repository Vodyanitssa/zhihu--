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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Person
import com.zhihuminus.platform.UserMessageDuration
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.FeedPullToRefresh
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.viewmodel.feed.FollowViewModel
import com.zhihuminus.viewmodel.feed.RecentMomentsViewModel
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

/**
 * 关注顶层页的生产入口。
 *
 * 顶部为已关注的人列表（[FollowingUsersRow]），下方是关注动态卡片流；接收主壳传入的
 * [scrollToTopTrigger] 和 [innerPadding] 以配合 tab 重选回到顶部、系统栏和底部栏留白。
 */
@Composable
fun FollowScreen(
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
) {
    val viewModel: FollowViewModel = viewModel { FollowViewModel() }
    val readingQueueSourceId = "follow:dynamic"
    val environment = rememberPaginationEnvironment()
    val userMessages = rememberUserMessageSink()
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger) {
        val action = topLevelReselectAction(
            triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
            isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
        )
        when (action) {
            TopLevelReselectAction.Refresh -> viewModel.refresh(environment)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> {}
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            userMessages.showMessage(it, UserMessageDuration.Long)
        }
    }

    Column(
        modifier = Modifier.padding(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding(),
        ),
    ) {
        FeedPullToRefresh(viewModel, environment) {
            PaginatedList(
                items = viewModel.displayItems,
                listState = listState,
                modifier = Modifier,
                onLoadMore = { viewModel.loadMore(environment) },
                topContent = {
                    item {
                        FollowingUsersRow()
                    }
                },
                footer = ProgressIndicatorFooter,
            ) { item ->
                FeedCard(
                    item = item,
                    readingQueueSourceId = readingQueueSourceId,
                    modifier = Modifier,
                    showSourceLabel = true,
                )
            }
        }
    }
}

/** 已关注人条目的自然高度（8dp 上下 contentPadding + 4dp 条目 padding + 56dp 头像 + 4dp 间隔 + 18dp 名字）。 */
private val FollowingUsersRowHeight = 102.dp

@Composable
fun FollowingUsersRow() {
    val navigator = LocalNavigator.current
    val viewModel: RecentMomentsViewModel = viewModel { RecentMomentsViewModel() }
    val environment = rememberPaginationEnvironment()

    LaunchedEffect(Unit) {
        viewModel.load(environment)
    }

    // 固定高度占位：首帧即占据最终尺寸，避免数据到达后首个列表项变高导致 LazyColumn 滚动锚定把整行顶出视口。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(FollowingUsersRowHeight),
    ) {
        when {
            viewModel.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = viewModel.errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            viewModel.users.isNotEmpty() -> {
                LazyRow(
                    modifier = Modifier,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(viewModel.users) { user ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    navigator.onNavigate(
                                        Person(
                                            id = user.actor.id,
                                            urlToken = user.actor.urlToken,
                                            name = user.actor.name,
                                            jumpTo = "动态",
                                        ),
                                    )
                                }.padding(vertical = 4.dp),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (user.unreadCount > 0) {
                                        Badge()
                                    }
                                },
                            ) {
                                AsyncImage(
                                    model = user.actor.avatarUrl,
                                    contentDescription = user.actor.name,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = user.actor.name,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.size(width = 60.dp, height = 18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

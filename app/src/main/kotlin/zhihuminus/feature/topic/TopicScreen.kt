package com.zhihuminus.feature.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.feature.topic.components.TopicHeader
import com.zhihuminus.feature.topic.components.TopicIntroduction
import com.zhihuminus.navigation.Topic
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicScreen(
    topic: Topic,
    state: TopicUiState,
    onEvent: (TopicEvent) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
) {
    var isIntroductionExpanded by rememberSaveable(topic.id) { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier,
        topBar = {
            TopAppBar(
                title = { Text(state.detail?.name?.ifBlank { topic.name } ?: topic.name.ifBlank { "话题" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        modifier = Modifier,
                        onClick = onShare,
                    ) { Icon(Icons.Default.Share, contentDescription = "分享") }
                },
            )
        },
    ) { padding ->
        PaginatedList(
            items = state.items,
            onLoadMore = { onEvent(TopicEvent.LoadMore) },
            isEnd = { state.isEnd },
            key = FeedDisplayItem::stableKey,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            footer = { listState ->
                if (state.errorMessage == null) {
                    ProgressIndicatorFooter(listState)
                } else {
                    TextButton(
                        onClick = { onEvent(TopicEvent.Retry) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                    ) { Text("加载失败：${state.errorMessage}，点击重试") }
                }
            },
            topContent = {
                item {
                    TopicHeader(
                        detail = state.detail,
                        detailErrorMessage = state.detailErrorMessage,
                        topicName = topic.name,
                        isFollowingChanging = state.isFollowingChanging,
                        onRetryDetail = { onEvent(TopicEvent.Follow(state.detail?.isFollowing != true)) },
                        onFollowingChange = { following -> onEvent(TopicEvent.Follow(following)) },
                    )
                    state.detail?.excerpt?.takeIf(String::isNotBlank)?.let { introduction ->
                        TopicIntroduction(
                            introduction = introduction,
                            isExpanded = isIntroductionExpanded,
                            onExpandedChange = { isIntroductionExpanded = it },
                        )
                    }
                    PrimaryTabRow(selectedTabIndex = state.selectedTab.ordinal) {
                        TopicFeedTab.entries.forEach { tab ->
                            Tab(
                                selected = state.selectedTab == tab,
                                onClick = { onEvent(TopicEvent.SelectTab(tab)) },
                                text = { Text(tab.title) },
                            )
                        }
                    }
                    if (state.selectedTab == TopicFeedTab.Discussion) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopicDiscussionSort.entries.forEach { sort ->
                                FilterChip(
                                    selected = state.discussionSort == sort,
                                    onClick = { onEvent(TopicEvent.SelectDiscussionSort(sort)) },
                                    label = { Text(sort.title) },
                                )
                            }
                        }
                    }
                    if (state.selectedTab == TopicFeedTab.Ideas) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            TopicIdeasSort.entries.forEach { sort ->
                                FilterChip(
                                    selected = state.ideasSort == sort,
                                    onClick = { onEvent(TopicEvent.SelectIdeasSort(sort)) },
                                    label = { Text(sort.title) },
                                )
                            }
                        }
                    }
                }
            },
        ) { item ->
            FeedCard(item = item, modifier = Modifier)
        }
    }
}

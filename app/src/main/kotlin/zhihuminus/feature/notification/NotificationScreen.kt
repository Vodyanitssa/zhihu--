package com.zhihuminus.feature.notification

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MarkChatRead
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.notification.components.NotificationCategoryRow
import com.zhihuminus.feature.notification.components.NotificationConversationRow
import com.zhihuminus.feature.notification.components.NotificationInvitationRow
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    state: NotificationUiState,
    onEvent: (NotificationEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onCategoryClick: (NotificationCategory) -> Unit,
    onInvitationClick: () -> Unit,
    onConversationClick: (NotificationTimelineItem) -> Unit,
    showUnreadBadges: Boolean,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("消息") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        IconButton(onClick = { onEvent(NotificationEvent.MarkAllAsRead) }) {
                            Icon(Icons.Default.MarkChatRead, contentDescription = "已读")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(NotificationEvent.Refresh) },
            modifier = Modifier.padding(paddingValues),
        ) {
            PaginatedList(
                items = state.items,
                onLoadMore = { onEvent(NotificationEvent.LoadMore) },
                isEnd = { state.isEnd },
                modifier = Modifier.fillMaxSize(),
                footer = if (state.isRefreshing) null else ProgressIndicatorFooter,
                key = { it.stableId },
                topContent = {
                    item(key = "notification_categories") {
                        NotificationCategoryRow(
                            unreadCounts = state.unreadCounts,
                            showUnreadBadges = showUnreadBadges,
                            onCategoryClick = onCategoryClick,
                        )
                    }
                    state.invitation?.let { invitation ->
                        item(key = "notification_invitation") {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                            NotificationInvitationRow(
                                invitation = invitation,
                                showUnreadBadge = showUnreadBadges,
                                onClick = onInvitationClick,
                            )
                        }
                    }
                    item(key = "notification_messages_divider") {
                        HorizontalDivider(
                            thickness = 8.dp,
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                        )
                    }
                },
            ) { notification ->
                NotificationConversationRow(
                    notification = notification,
                    showUnreadBadge = showUnreadBadges,
                    onClick = { onConversationClick(notification) },
                )
            }
        }
    }
}

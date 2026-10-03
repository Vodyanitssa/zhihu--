package com.zhihuminus.feature.notification

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.notification.components.InvitationAnswerItem
import com.zhihuminus.feature.notification.components.NotificationItemView
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationTimelineScreen(
    state: NotificationTimelineUiState,
    onEvent: (NotificationTimelineEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNotificationClick: (NotificationTimelineItem) -> Unit,
    onQuestionClick: (NotificationTimelineItem) -> Unit,
    onAnswerClick: (NotificationTimelineItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val invitations = state.entryName == "invite"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            onRefresh = { onEvent(NotificationTimelineEvent.Refresh) },
            modifier = Modifier.padding(paddingValues),
        ) {
            PaginatedList(
                items = state.items,
                onLoadMore = { onEvent(NotificationTimelineEvent.LoadMore) },
                isEnd = { state.isEnd },
                modifier = Modifier.fillMaxSize(),
                footer = if (state.isRefreshing) null else ProgressIndicatorFooter,
                key = { it.stableId },
            ) { notification ->
                when {
                    notification.type == "empty" -> {
                        Text(
                            text = notification.emptyInfo?.text.orEmpty(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }

                    invitations -> InvitationAnswerItem(
                        notification = notification,
                        onQuestionClick = { onQuestionClick(notification) },
                        onAnswerClick = { onAnswerClick(notification) },
                    )

                    else -> {
                        NotificationItemView(
                            notification = notification,
                            onClick = { onNotificationClick(notification) },
                        )
                    }
                }
            }
        }
    }
}

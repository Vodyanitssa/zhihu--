package com.zhihuminus.feature.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.zhihuminus.feature.collection.components.CollectionCard
import com.zhihuminus.feature.collection.components.CollectionDeleteDialog
import com.zhihuminus.feature.collection.components.CreateCollectionDialog
import com.zhihuminus.ui.TopLevelReselectAction
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.ui.topLevelReselectAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    state: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    onCollectionClick: (Collection) -> Unit,
    onNavigateBack: () -> Unit,
    showBackButton: Boolean = true,
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }

    LaunchedEffect(scrollToTopTrigger) {
        when (
            topLevelReselectAction(
                triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
                isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
            )
        ) {
            TopLevelReselectAction.Refresh -> onEvent(CollectionEvent.Refresh)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> Unit
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    LaunchedEffect(isActive) {
        if (isActive && state.collections.isEmpty()) {
            onEvent(CollectionEvent.Refresh)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "我的收藏夹")
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(CollectionEvent.OpenCreateDialog) },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "新建收藏夹")
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(CollectionEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (state.collections.isEmpty() && state.isEnd) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有收藏夹")
                }
            } else {
                PaginatedList(
                    items = state.collections,
                    onLoadMore = { onEvent(CollectionEvent.LoadMore) },
                    isEnd = { state.isEnd },
                    listState = listState,
                    modifier = Modifier.fillMaxSize(),
                    footer = ProgressIndicatorFooter,
                ) { collection ->
                    CollectionCard(
                        collection = collection,
                        onClick = { onCollectionClick(collection) },
                        onDeleteClick = { onEvent(CollectionEvent.RequestDelete(collection)) },
                        canDelete = !state.isDeleting,
                    )
                }
            }
        }
    }

    CreateCollectionDialog(
        showDialog = state.showCreateDialog,
        onDismiss = { onEvent(CollectionEvent.DismissCreateDialog) },
        onConfirm = { title, description, isPublic ->
            onEvent(CollectionEvent.CreateCollection(title, description, isPublic))
        },
        isSubmitting = state.isCreating,
        errorMessage = state.createError,
    )

    CollectionDeleteDialog(
        collection = state.collectionPendingDeletion,
        isDeleting = state.isDeleting,
        errorMessage = state.deleteError,
        onDismiss = { onEvent(CollectionEvent.DismissDeleteDialog) },
        onConfirm = { onEvent(CollectionEvent.ConfirmDelete) },
    )
}

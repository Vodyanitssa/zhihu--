package com.zhihuminus.feature.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import com.zhihuminus.feature.collection.components.CollectionContentBody
import com.zhihuminus.feature.collection.components.CollectionDeleteDialog
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.ui.TopLevelReselectAction
import com.zhihuminus.ui.topLevelReselectAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBrowseScreen(
    state: CollectionBrowseUiState,
    onEvent: (CollectionBrowseEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
    showBackButton: Boolean = false,
    scrollToTopTrigger: Int = 0,
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
            TopLevelReselectAction.Refresh -> onEvent(CollectionBrowseEvent.Refresh)
            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> Unit
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.selectedCollection?.title ?: "收藏夹",
                        modifier = Modifier,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { onEvent(CollectionBrowseEvent.SetFolderMenuExpanded(true)) },
                            enabled = state.collections.isNotEmpty(),
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = "切换收藏夹")
                        }
                        DropdownMenu(
                            expanded = state.folderMenuExpanded,
                            onDismissRequest = { onEvent(CollectionBrowseEvent.SetFolderMenuExpanded(false)) },
                        ) {
                            state.collections.forEach { collection ->
                                DropdownMenuItem(
                                    text = { Text(collection.title) },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (collection.id == state.selectedCollectionId) {
                                                Icon(Icons.Filled.Check, contentDescription = null)
                                            }
                                            if (!collection.isDefault) {
                                                IconButton(
                                                    onClick = {
                                                        onEvent(CollectionBrowseEvent.RequestDelete(collection))
                                                    },
                                                    enabled = !state.isDeleting,
                                                ) {
                                                    Icon(
                                                        Icons.Filled.Delete,
                                                        contentDescription = "删除${collection.title}",
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        onEvent(CollectionBrowseEvent.SelectCollection(collection.id))
                                    },
                                )
                            }
                        }
                    }
                    IconToggleButton(
                        checked = state.randomMode,
                        onCheckedChange = { enabled ->
                            onEvent(CollectionBrowseEvent.ToggleRandomMode(enabled))
                        },
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = if (state.randomMode) {
                                Icons.Filled.Shuffle
                            } else {
                                Icons.AutoMirrored.Filled.FormatListBulleted
                            },
                            contentDescription = if (state.randomMode) {
                                "当前为随机模式，点击切换为顺序模式"
                            } else {
                                "当前为顺序模式，点击切换为随机模式"
                            },
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.collections.isEmpty() && state.isEnd -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有收藏夹")
                }
            }

            state.isLoadingCollections && state.collections.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = { onEvent(CollectionBrowseEvent.Refresh) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (state.items.isEmpty() && state.isEnd) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("这个收藏夹是空的")
                        }
                    } else {
                        CollectionContentBody(
                            items = state.items,
                            collection = state.selectedCollection,
                            onLoadMore = { onEvent(CollectionBrowseEvent.LoadMore) },
                            isEnd = state.isEnd,
                            onDestinationClick = onDestinationClick,
                            listState = listState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }

    CollectionDeleteDialog(
        collection = state.collectionPendingDeletion,
        isDeleting = state.isDeleting,
        errorMessage = state.deleteError,
        onDismiss = { onEvent(CollectionBrowseEvent.DismissDeleteDialog) },
        onConfirm = { onEvent(CollectionBrowseEvent.ConfirmDelete) },
    )
}

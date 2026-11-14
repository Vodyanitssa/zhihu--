package com.zhihuminus.feature.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.zhihuminus.feature.collection.components.CollectionContentBody
import com.zhihuminus.feature.collection.components.CollectionDeleteDialog
import com.zhihuminus.feature.collection.components.CollectionItemRemoveDialog
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.ui.components.ErrorFullPage
import com.zhihuminus.ui.components.ScrollAwareTopBarTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentScreen(
    state: CollectionContentUiState,
    isOwner: Boolean,
    onEvent: (CollectionContentEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onDestinationClick: ((NavDestination?) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    var isDescriptionExpanded by rememberSaveable(state.collectionId) { mutableStateOf(false) }
    var isMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    ScrollAwareTopBarTitle(
                        state = listState,
                        title = state.collection?.title?.takeIf { it.isNotBlank() } ?: "收藏夹",
                        placeholder = "收藏夹",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    val collection = state.collection
                    if (isOwner && collection != null && !collection.isDefault) {
                        Box {
                            IconButton(onClick = { isMenuExpanded = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = isMenuExpanded,
                                onDismissRequest = { isMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "删除收藏夹",
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        isMenuExpanded = false
                                        onEvent(CollectionContentEvent.RequestDelete)
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val fullPageError = if (state.items.isEmpty() && state.collection == null && !state.isRefreshing) {
            state.errorMessage
        } else {
            null
        }

        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(CollectionContentEvent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (fullPageError != null) {
                ErrorFullPage(
                    error = fullPageError,
                    onRetry = { onEvent(CollectionContentEvent.Refresh) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CollectionContentBody(
                    state = state,
                    isOwner = isOwner,
                    listState = listState,
                    isDescriptionExpanded = isDescriptionExpanded,
                    onDescriptionExpandedChange = { isDescriptionExpanded = it },
                    onRetryDetail = { onEvent(CollectionContentEvent.RetryDetail) },
                    onRefresh = { onEvent(CollectionContentEvent.Refresh) },
                    onLoadMore = { onEvent(CollectionContentEvent.LoadMore) },
                    onDestinationClick = onDestinationClick,
                    onRequestRemoveItem = { onEvent(CollectionContentEvent.RequestRemoveItem(it)) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    CollectionDeleteDialog(
        collection = state.collection.takeIf { state.showDeleteDialog },
        isDeleting = state.isDeleting,
        errorMessage = state.deleteError,
        onDismiss = { onEvent(CollectionContentEvent.DismissDeleteDialog) },
        onConfirm = { onEvent(CollectionContentEvent.ConfirmDelete) },
    )

    CollectionItemRemoveDialog(
        target = state.itemPendingRemoval,
        isRemoving = state.isRemovingItem,
        errorMessage = state.removeError,
        onDismiss = { onEvent(CollectionContentEvent.DismissRemoveItemDialog) },
        onConfirm = { onEvent(CollectionContentEvent.ConfirmRemoveItem) },
    )
}

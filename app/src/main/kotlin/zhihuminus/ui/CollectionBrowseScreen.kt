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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.data.Collection
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.viewmodel.CollectionContentViewModel
import com.zhihuminus.viewmodel.CollectionsViewModel
import com.zhihuminus.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * 收藏直达浏览页：进入后直接展示某个收藏夹的内容瀑布流，
 * 右上角文件夹图标在页内切换当前展示的收藏夹，不跳转到新页面。
 *
 * 默认选中策略见 [pickDefaultCollectionId]（优先默认收藏夹，否则第一个）。
 *
 * @param testCollections 测试注入的收藏夹列表；非 null 时跳过收藏夹列表的网络拉取，便于仪器测试。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionBrowseScreen(
    urlToken: String?,
    showBackButton: Boolean = true,
    scrollToTopTrigger: Int = 0,
    isActive: Boolean = true,
    testCollections: List<Collection>? = null,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val useTestCollections = testCollections != null || urlToken == null
    val collectionsViewModel: CollectionsViewModel = viewModel(key = urlToken) {
        CollectionsViewModel(urlToken.orEmpty())
    }
    val collections = testCollections ?: collectionsViewModel.allData
    val userMessages = rememberUserMessageSink()
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var cachedScrollToTopTrigger by remember { mutableIntStateOf(scrollToTopTrigger) }
    var randomSeed by rememberSaveable { mutableIntStateOf(Random.nextInt()) }
    var selectedCollectionId by rememberSaveable { mutableStateOf<String?>(null) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var randomMode by rememberSaveable { mutableStateOf(false) }
    var refreshCollectionsOnNextActivation by rememberSaveable { mutableStateOf(true) }
    var collectionPendingDeletion by remember { mutableStateOf<Collection?>(null) }

    LaunchedEffect(isActive) {
        if (
            shouldRefreshCollectionDataOnActivation(
                isActive = isActive,
                useTestCollections = useTestCollections,
                refreshOnNextActivation = refreshCollectionsOnNextActivation,
            )
        ) {
            collectionsViewModel.refresh(environment)
        }
        refreshCollectionsOnNextActivation = !isActive
    }
    LaunchedEffect(collections.map { it.id }, collectionsViewModel.isLoading) {
        if (collections.isNotEmpty() && collections.none { it.id == selectedCollectionId }) {
            selectedCollectionId = pickDefaultCollectionId(collections)
        } else if (collections.isEmpty() && (useTestCollections || collectionsViewModel.isEnd)) {
            selectedCollectionId = null
        }
    }

    val selectedCollection = collections.firstOrNull { it.id == selectedCollectionId }
    val contentViewModel: CollectionContentViewModel? = selectedCollectionId?.let { collectionId ->
        viewModel(key = collectionId) { CollectionContentViewModel(collectionId) }
    }
    val selectedCollectionItemCount = selectedCollection
        ?.itemCount
        ?.takeIf { it > 0 }
        ?: contentViewModel?.collection?.itemCount
        ?: 0
    val sourceDisplayItems = contentViewModel?.displayItems?.toList().orEmpty()
    val orderedDisplayItems = remember(
        sourceDisplayItems,
        randomMode,
        randomSeed,
        contentViewModel,
    ) {
        val items = orderCollectionItems(
            items = sourceDisplayItems,
            randomMode = randomMode,
            randomSeed = randomSeed,
            previousRandomOrderKeys = contentViewModel?.retainedRandomOrderKeys.orEmpty(),
        )
        if (randomMode) {
            contentViewModel?.retainRandomDisplayOrder(items.map { it.stableKey })
        }
        items
    }
    LaunchedEffect(contentViewModel, isActive, randomMode, randomSeed, selectedCollectionItemCount) {
        if (isActive && !useTestCollections && contentViewModel != null) {
            if (randomMode) {
                contentViewModel.refreshRandom(
                    environment = environment,
                    itemCount = selectedCollectionItemCount,
                    randomSeed = randomSeed,
                )
            } else {
                contentViewModel.refresh(environment)
            }
        }
    }
    LaunchedEffect(scrollToTopTrigger) {
        when (
            topLevelReselectAction(
                triggerDelta = scrollToTopTrigger - cachedScrollToTopTrigger,
                isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0,
            )
        ) {
            TopLevelReselectAction.Refresh -> {
                if (!useTestCollections) {
                    collectionsViewModel.refresh(environment)
                    if (randomMode) {
                        randomSeed = Random.nextInt()
                    } else {
                        contentViewModel?.refresh(environment)
                    }
                }
            }

            TopLevelReselectAction.ScrollToTop -> listState.animateScrollToItem(0)
            null -> Unit
        }
        cachedScrollToTopTrigger = scrollToTopTrigger
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = contentViewModel?.title ?: "收藏夹",
                        modifier = Modifier,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(
                            onClick = navigator.onNavigateBack,
                            modifier = Modifier,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { folderMenuExpanded = true },
                            enabled = collections.isNotEmpty(),
                            modifier = Modifier,
                        ) {
                            Icon(Icons.Filled.Folder, contentDescription = "切换收藏夹")
                        }
                        DropdownMenu(
                            expanded = folderMenuExpanded,
                            onDismissRequest = { folderMenuExpanded = false },
                            modifier = Modifier,
                        ) {
                            collections.forEach { collection ->
                                DropdownMenuItem(
                                    text = { Text(collection.title) },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (collection.id == selectedCollectionId) {
                                                Icon(Icons.Filled.Check, contentDescription = null)
                                            }
                                            if (!collection.isDefault) {
                                                IconButton(
                                                    onClick = {
                                                        collectionsViewModel.clearMutationErrors()
                                                        collectionPendingDeletion = collection
                                                        folderMenuExpanded = false
                                                    },
                                                    enabled = collectionsViewModel.deletingCollectionId == null,
                                                    modifier = Modifier,
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
                                        selectedCollectionId = collection.id
                                        folderMenuExpanded = false
                                    },
                                    modifier = Modifier,
                                )
                            }
                        }
                    }
                    IconToggleButton(
                        checked = randomMode,
                        onCheckedChange = { enabled ->
                            randomMode = enabled
                            if (enabled) randomSeed = Random.nextInt()
                            userMessages.showShortMessage(if (enabled) "已切换为随机模式" else "已切换为顺序模式")
                        },
                        modifier = Modifier,
                        colors = IconButtonDefaults.iconToggleButtonColors(
                            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Icon(
                            imageVector = if (randomMode) {
                                Icons.Filled.Shuffle
                            } else {
                                Icons.AutoMirrored.Filled.FormatListBulleted
                            },
                            contentDescription = if (randomMode) {
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
            collections.isEmpty() && (useTestCollections || collectionsViewModel.isEnd) -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("还没有收藏夹", modifier = Modifier)
                }
            }

            contentViewModel == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier)
                }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = collectionsViewModel.isLoading || contentViewModel.isLoading,
                    onRefresh = {
                        if (!useTestCollections) {
                            collectionsViewModel.refresh(environment)
                            if (randomMode) {
                                randomSeed = Random.nextInt()
                            } else {
                                contentViewModel.refresh(environment)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (contentViewModel.allData.isEmpty() && contentViewModel.isEnd) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "这个收藏夹是空的",
                                modifier = Modifier,
                            )
                        }
                    } else {
                        CollectionContentBody(
                            viewModel = contentViewModel,
                            environment = environment,
                            collectionId = selectedCollectionId.orEmpty(),
                            modifier = Modifier.fillMaxSize(),
                            listState = listState,
                            displayItems = orderedDisplayItems,
                        )
                    }
                }
            }
        }
    }

    collectionPendingDeletion?.let { collection ->
        val isDeleting = collectionsViewModel.deletingCollectionId != null
        AlertDialog(
            modifier = Modifier,
            onDismissRequest = {
                if (!isDeleting) {
                    collectionPendingDeletion = null
                }
            },
            title = { Text("删除收藏夹") },
            text = {
                Column {
                    Text("删除后无法恢复，确认删除收藏夹“${collection.title}”吗？")
                    collectionsViewModel.deleteCollectionError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            if (collectionsViewModel.deleteCollection(environment, collection)) {
                                if (selectedCollectionId == collection.id) {
                                    selectedCollectionId = pickDefaultCollectionId(
                                        collections.filterNot { it.id == collection.id },
                                    )
                                }
                                collectionsViewModel.refresh(environment)
                                collectionPendingDeletion = null
                                userMessages.showShortMessage("收藏夹已删除")
                            }
                        }
                    },
                    enabled = !isDeleting,
                    modifier = Modifier,
                ) {
                    Text(if (isDeleting) "删除中…" else "删除")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { collectionPendingDeletion = null },
                    enabled = !isDeleting,
                ) {
                    Text("取消")
                }
            },
        )
    }
}

/** 挑选默认展示的收藏夹 id：优先默认收藏夹，否则取第一个；列表为空返回 null。 */
internal fun pickDefaultCollectionId(collections: List<Collection>): String? =
    collections.firstOrNull { it.isDefault }?.id ?: collections.firstOrNull()?.id

internal fun shouldRefreshCollectionDataOnActivation(
    isActive: Boolean,
    useTestCollections: Boolean,
    refreshOnNextActivation: Boolean = true,
): Boolean = isActive && !useTestCollections && refreshOnNextActivation

internal fun orderCollectionItems(
    items: List<FeedDisplayItem>,
    randomMode: Boolean,
    randomSeed: Int,
    previousRandomOrderKeys: List<String> = emptyList(),
): List<FeedDisplayItem> = if (randomMode) {
    val itemsByKey = items.associateBy { it.stableKey }
    val retainedKeys = previousRandomOrderKeys.distinct().filter(itemsByKey::containsKey)
    val retainedKeySet = retainedKeys.toSet()
    val newItems = items
        .distinctBy { it.stableKey }
        .filterNot { it.stableKey in retainedKeySet }
        .map { item ->
            val rank = item.stableKey.fold(COLLECTION_RANDOM_ORDER_OFFSET_BASIS xor randomSeed.toLong()) { hash, char ->
                (hash xor char.code.toLong()) * COLLECTION_RANDOM_ORDER_PRIME
            }
            item to rank
        }.sortedWith(
            compareBy<Pair<FeedDisplayItem, Long>>(
                { it.second },
                { it.first.stableKey },
            ),
        ).map { it.first }
    retainedKeys.mapNotNull(itemsByKey::get) + newItems
} else {
    items
}

private const val COLLECTION_RANDOM_ORDER_OFFSET_BASIS = -3750763034362895579L
private const val COLLECTION_RANDOM_ORDER_PRIME = 1099511628211L

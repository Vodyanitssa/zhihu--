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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastJoinToString
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.ArticleType
import com.zhihuminus.navigation.CollectionAnswerNavigator
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import com.zhihuminus.viewmodel.CollectionContentViewModel
import com.zhihuminus.viewmodel.PaginationEnvironment
import com.zhihuminus.viewmodel.formatArticleDateTime
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionContentScreen(
    collectionId: String,
) {
    val navigator = LocalNavigator.current
    val screenViewModel = viewModel { CollectionContentViewModel(collectionId) }
    val environment = rememberPaginationEnvironment(allowGuestAccess = false)
    val listState = rememberLazyListState()

    LaunchedEffect(screenViewModel) {
        if (screenViewModel.allData.isEmpty()) {
            screenViewModel.refresh(environment)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = screenViewModel.title,
                        modifier = Modifier,
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier,
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        CollectionContentBody(
            viewModel = screenViewModel,
            environment = environment,
            collectionId = collectionId,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            listState = listState,
        )
    }
}

@Composable
internal fun CollectionContentBody(
    viewModel: CollectionContentViewModel,
    environment: PaginationEnvironment,
    collectionId: String,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    displayItems: List<FeedDisplayItem> = viewModel.displayItems,
) {
    val navigator = LocalNavigator.current
    val sharedData = environment.articleAnswerSwitchState()
    val readingQueueSourceId = "collection:$collectionId:contents"

    val visibleCollectionItems = displayItems.mapNotNull { displayItem ->
        val sourceIndex = viewModel.displayItems.indexOf(displayItem)
        viewModel.allData.getOrNull(sourceIndex)
    }

    PaginatedList(
        items = displayItems,
        onLoadMore = { viewModel.loadMore(environment) },
        isEnd = { viewModel.isEnd },
        listState = listState,
        modifier = modifier,
        footer = ProgressIndicatorFooter,
        topContent = {
            item(0) {
                Text(
                    text = listOfNotNull(
                        viewModel.collection?.itemCount?.let { "$it 条收藏" },
                        viewModel.collection?.likeCount?.let { "$it 个赞同" },
                        viewModel.collection?.commentCount?.let { "$it 条评论" },
                        viewModel.collection?.updatedTime?.let { "${formatArticleDateTime(it)} 更新" },
                    ).fastJoinToString(" · "),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) { item ->
        FeedCard(
            item = item,
            readingQueueSourceId = readingQueueSourceId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) { _, destination ->
            if (destination is Article && destination.type == ArticleType.Answer && sharedData != null) {
                val index = displayItems.indexOf(item)
                val nextItems = if (index >= 0) visibleCollectionItems.drop(index + 1) else emptyList()
                val previousItems = if (index > 0) visibleCollectionItems.take(index).reversed() else emptyList()
                sharedData.pendingNavigator = CollectionAnswerNavigator(
                    collectionId = collectionId,
                    collectionTitle = viewModel.title,
                    initialNextItems = nextItems,
                    initialPreviousItems = previousItems,
                    initialNextUrl = viewModel.nextPageUrl,
                    environment = environment,
                )
            }
            destination?.let(navigator.onNavigate)
        }
    }
}

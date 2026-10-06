package com.zhihuminus.feature.people.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.people.PeopleCreationTab
import com.zhihuminus.feature.people.PeopleEvent
import com.zhihuminus.feature.people.PeopleUiState
import com.zhihuminus.feature.people.webUrl
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.withReadingQueueSource
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleCreationsTab(
    state: PeopleUiState,
    pagerState: PagerState,
    readingQueueSourceId: String?,
    onEvent: (PeopleEvent) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 8.dp,
            modifier = Modifier,
        ) {
            PeopleCreationTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(tab.title) },
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            when (page) {
                0 -> {
                    // 回答
                    Column(modifier = Modifier.fillMaxSize()) {
                        PeopleSortBar(
                            currentSort = state.answersSort,
                            onSortChange = { onEvent(PeopleEvent.ChangeAnswersSort(it)) },
                        )
                        PaginatedList(
                            items = state.answersState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Answers)) },
                            isEnd = { state.answersState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.stableKey },
                        ) { item ->
                            FeedCard(
                                item = item,
                                modifier = Modifier,
                                horizontalPadding = 4.dp,
                                onClick = {
                                    item.navDestination?.withReadingQueueSource(readingQueueSourceId)?.let(onNavigate)
                                },
                            )
                        }
                    }
                }

                1 -> {
                    // 文章
                    Column(modifier = Modifier.fillMaxSize()) {
                        PeopleSortBar(
                            currentSort = state.articlesSort,
                            onSortChange = { onEvent(PeopleEvent.ChangeArticlesSort(it)) },
                        )
                        PaginatedList(
                            items = state.articlesState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Articles)) },
                            isEnd = { state.articlesState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.stableKey },
                        ) { item ->
                            FeedCard(
                                item = item,
                                modifier = Modifier,
                                horizontalPadding = 4.dp,
                                onClick = {
                                    item.navDestination?.withReadingQueueSource(readingQueueSourceId)?.let(onNavigate)
                                },
                            )
                        }
                    }
                }

                2 -> {
                    // 想法
                    PaginatedList(
                        items = state.pinsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Pins)) },
                        isEnd = { state.pinsState.isEnd },
                        footer = ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.stableKey },
                    ) { item ->
                        FeedCard(
                            item = item,
                            modifier = Modifier,
                            horizontalPadding = 4.dp,
                            onClick = {
                                item.navDestination?.withReadingQueueSource(readingQueueSourceId)?.let(onNavigate)
                            },
                        )
                    }
                }

                3 -> {
                    // 专栏
                    PaginatedList(
                        items = state.columnsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Columns)) },
                        isEnd = { state.columnsState.isEnd },
                        footer = ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { column ->
                        PeopleColumnListItem(
                            column = column,
                            onClick = { onLinkClick(column.webUrl()) },
                        )
                    }
                }

                4 -> {
                    // 提问
                    PaginatedList(
                        items = state.questionsState.items,
                        onLoadMore = { onEvent(PeopleEvent.LoadMoreCreation(PeopleCreationTab.Questions)) },
                        isEnd = { state.questionsState.isEnd },
                        footer = ProgressIndicatorFooter,
                        modifier = Modifier.fillMaxSize(),
                        key = { it.id },
                    ) { question ->
                        PeopleQuestionListItem(
                            question = question,
                            onClick = { onNavigate(Question(question.id, question.title)) },
                        )
                    }
                }
            }
        }
    }
}

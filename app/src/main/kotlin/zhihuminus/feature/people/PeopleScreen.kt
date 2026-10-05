package com.zhihuminus.feature.people

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.people.components.PeopleCollectionListItem
import com.zhihuminus.feature.people.components.PeopleColumnListItem
import com.zhihuminus.feature.people.components.PeopleFollowingSubscriptionsTab
import com.zhihuminus.feature.people.components.PeopleQuestionListItem
import com.zhihuminus.feature.people.components.PeopleSortBar
import com.zhihuminus.feature.people.components.PeopleUserInfoHeader
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.Question
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.PeopleListItem
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import kotlinx.coroutines.launch
import com.zhihuminus.navigation.Search as SearchDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onEvent: (PeopleEvent) -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onLinkClick: (String) -> Unit,
    onImagePreview: (String) -> Unit,
    onExternalUrl: (String) -> Unit,
    initialPage: Int = 2,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { PeopleTab.entries.size },
    )

    LaunchedEffect(pagerState.currentPage) {
        onEvent(PeopleEvent.TabSelected(pagerState.currentPage))
    }

    val readingQueueSourceId = when (pagerState.currentPage) {
        0 -> "people:${state.profile.userTokenOrId}:answers:${state.answersSort}"
        1 -> "people:${state.profile.userTokenOrId}:articles:${state.articlesSort}"
        2 -> "people:${state.profile.userTokenOrId}:activities:created"
        5 -> "people:${state.profile.userTokenOrId}:pins"
        else -> null
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            Box {
                TopAppBar(
                    title = {
                        PeopleUserInfoHeader(
                            profile = state.profile,
                            onFollowToggle = { onEvent(PeopleEvent.ToggleFollow) },
                            onBlockToggle = { onEvent(PeopleEvent.ToggleBlock) },
                            onStatClick = { page ->
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(page)
                                }
                            },
                            onAvatarClick = onImagePreview,
                            onExternalUrlClick = onExternalUrl,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors().copy(
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    scrollBehavior = scrollBehavior,
                    expandedHeight = 240.dp,
                )
                if (state.profile.id.isNotBlank() && state.profile.id != Person.EMPTY_ID) {
                    IconButton(
                        onClick = {
                            onNavigate(
                                SearchDestination(
                                    restrictedMemberHashId = state.profile.id,
                                    restrictedMemberName = state.profile.name,
                                ),
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 32.dp, end = 8.dp),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "搜索 TA 的创作")
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier,
            ) {
                PeopleTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier,
                    ) {
                        Text(
                            text = tab.title,
                            modifier = Modifier.padding(16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
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
                                onLoadMore = { onEvent(PeopleEvent.LoadMore(0)) },
                                isEnd = { state.answersState.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier.fillMaxSize(),
                                key = { it.stableKey },
                            ) { item ->
                                FeedCard(
                                    item = item,
                                    readingQueueSourceId = readingQueueSourceId,
                                    modifier = Modifier,
                                    horizontalPadding = 4.dp,
                                ) { _, destination ->
                                    destination?.let(onNavigate)
                                }
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
                                onLoadMore = { onEvent(PeopleEvent.LoadMore(1)) },
                                isEnd = { state.articlesState.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier.fillMaxSize(),
                                key = { it.stableKey },
                            ) { item ->
                                FeedCard(
                                    item = item,
                                    readingQueueSourceId = readingQueueSourceId,
                                    modifier = Modifier,
                                    horizontalPadding = 4.dp,
                                ) { _, destination ->
                                    destination?.let(onNavigate)
                                }
                            }
                        }
                    }

                    2 -> {
                        // 动态
                        PaginatedList(
                            items = state.activitiesState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(2)) },
                            isEnd = { state.activitiesState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.stableKey },
                        ) { item ->
                            FeedCard(
                                item = item,
                                readingQueueSourceId = readingQueueSourceId,
                                modifier = Modifier,
                                horizontalPadding = 4.dp,
                            )
                        }
                    }

                    3 -> {
                        // 收藏
                        PaginatedList(
                            items = state.collectionsState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(3)) },
                            isEnd = { state.collectionsState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.id },
                        ) { collection ->
                            PeopleCollectionListItem(
                                collection = collection,
                                onClick = { onNavigate(CollectionContent(collection.id)) },
                            )
                        }
                    }

                    4 -> {
                        // 提问
                        PaginatedList(
                            items = state.questionsState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(4)) },
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

                    5 -> {
                        // 想法（已与回答、文章统一为 Post 流，复用 FeedCard）
                        PaginatedList(
                            items = state.pinsState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(5)) },
                            isEnd = { state.pinsState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.stableKey },
                        ) { item ->
                            FeedCard(
                                item = item,
                                readingQueueSourceId = readingQueueSourceId,
                                modifier = Modifier,
                                horizontalPadding = 4.dp,
                            ) { _, destination ->
                                destination?.let(onNavigate)
                            }
                        }
                    }

                    6 -> {
                        // 专栏
                        PaginatedList(
                            items = state.columnsState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(6)) },
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

                    7 -> {
                        // 粉丝
                        PaginatedList(
                            items = state.followersState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(7)) },
                            isEnd = { state.followersState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.urlToken ?: it.id },
                        ) { people ->
                            PeopleListItem(
                                people = people,
                                isFollowing = people.isFollowing,
                                isChangingFollowing = people.id in state.changingItemFollowIds,
                                onClick = {
                                    onNavigate(
                                        Person(
                                            id = people.id,
                                            name = people.name,
                                            urlToken = people.urlToken ?: "",
                                        ),
                                    )
                                },
                                onToggleFollow = {
                                    onEvent(PeopleEvent.ToggleItemFollow(people))
                                },
                            )
                        }
                    }

                    8 -> {
                        // 关注
                        PaginatedList(
                            items = state.followingState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMore(8)) },
                            isEnd = { state.followingState.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier.fillMaxSize(),
                            key = { it.urlToken ?: it.id },
                        ) { people ->
                            PeopleListItem(
                                people = people,
                                isFollowing = people.isFollowing,
                                isChangingFollowing = people.id in state.changingItemFollowIds,
                                onClick = {
                                    onNavigate(
                                        Person(
                                            id = people.id,
                                            name = people.name,
                                            urlToken = people.urlToken ?: "",
                                        ),
                                    )
                                },
                                onToggleFollow = {
                                    onEvent(PeopleEvent.ToggleItemFollow(people))
                                },
                            )
                        }
                    }

                    9 -> {
                        // 关注订阅
                        PeopleFollowingSubscriptionsTab(
                            selectedIndex = state.selectedSubscriptionTab,
                            onTabSelect = { onEvent(PeopleEvent.SubscriptionTabSelected(it)) },
                            onLoadMore = { onEvent(PeopleEvent.LoadMoreSubscription(it)) },
                            columnsState = state.followingColumnsState,
                            topicsState = state.followingTopicsState,
                            questionsState = state.followingQuestionsState,
                            collectionsState = state.followingCollectionsState,
                            onColumnClick = { onLinkClick(it.webUrl()) },
                            onTopicClick = { onNavigate(com.zhihuminus.navigation.Topic(it.id, it.name)) },
                            onQuestionClick = {
                                it.id.toLongOrNull()?.let { qId ->
                                    onNavigate(Question(qId, it.title))
                                }
                            },
                            onCollectionClick = { onNavigate(CollectionContent(it.id)) },
                            modifier = Modifier,
                        )
                    }
                }
            }
        }
    }
}

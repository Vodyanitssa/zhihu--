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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.people.components.PeopleCollectionListItem
import com.zhihuminus.feature.people.components.PeopleCreationsTab
import com.zhihuminus.feature.people.components.PeopleFollowingSubscriptionsTab
import com.zhihuminus.feature.people.components.PeopleUserInfoHeader
import com.zhihuminus.feature.people.components.PeopleUserListSheet
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.withReadingQueueSource
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
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
    initialSelection: PeopleInitialSelection = PeopleInitialSelection(),
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val primaryPagerState = rememberPagerState(
        initialPage = initialSelection.primaryTab.ordinal,
        pageCount = { PeoplePrimaryTab.entries.size },
    )
    val creationPagerState = rememberPagerState(
        initialPage = initialSelection.creationTab.ordinal,
        pageCount = { PeopleCreationTab.entries.size },
    )
    var activeUserListType by remember { mutableStateOf(initialSelection.initialUserListType) }

    LaunchedEffect(primaryPagerState.currentPage) {
        val tab = PeoplePrimaryTab.entries[primaryPagerState.currentPage]
        onEvent(PeopleEvent.PrimaryTabSelected(tab))
    }

    LaunchedEffect(creationPagerState.currentPage) {
        val tab = PeopleCreationTab.entries[creationPagerState.currentPage]
        onEvent(PeopleEvent.CreationTabSelected(tab))
    }

    LaunchedEffect(activeUserListType) {
        activeUserListType?.let { onEvent(PeopleEvent.LoadUserListIfNeeded(it)) }
    }

    val readingQueueSourceId = when (PeoplePrimaryTab.entries[primaryPagerState.currentPage]) {
        PeoplePrimaryTab.Creations -> when (PeopleCreationTab.entries[creationPagerState.currentPage]) {
            PeopleCreationTab.Answers -> "people:${state.profile.userTokenOrId}:answers:${state.answersSort}"
            PeopleCreationTab.Articles -> "people:${state.profile.userTokenOrId}:articles:${state.articlesSort}"
            PeopleCreationTab.Pins -> "people:${state.profile.userTokenOrId}:pins"
            else -> null
        }
        PeoplePrimaryTab.Activities -> "people:${state.profile.userTokenOrId}:activities:created"
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
                            onFollowingClick = { activeUserListType = PeopleUserListType.Following },
                            onFollowersClick = { activeUserListType = PeopleUserListType.Followers },
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
                selectedTabIndex = primaryPagerState.currentPage,
                edgePadding = 8.dp,
                modifier = Modifier,
            ) {
                PeoplePrimaryTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = primaryPagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                primaryPagerState.animateScrollToPage(index)
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
                state = primaryPagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> {
                        // 创作 (包含二级导航: 回答、文章、想法、专栏、提问)
                        PeopleCreationsTab(
                            state = state,
                            pagerState = creationPagerState,
                            readingQueueSourceId = readingQueueSourceId,
                            onEvent = onEvent,
                            onNavigate = onNavigate,
                            onLinkClick = onLinkClick,
                        )
                    }

                    1 -> {
                        // 动态
                        PaginatedList(
                            items = state.activitiesState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMorePrimary(PeoplePrimaryTab.Activities)) },
                            isEnd = { state.activitiesState.isEnd },
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

                    2 -> {
                        // 收藏
                        PaginatedList(
                            items = state.collectionsState.items,
                            onLoadMore = { onEvent(PeopleEvent.LoadMorePrimary(PeoplePrimaryTab.Collections)) },
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

                    3 -> {
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

    activeUserListType?.let { type ->
        val title = when (type) {
            PeopleUserListType.Following -> "关注 (${state.profile.followingCount})"
            PeopleUserListType.Followers -> "粉丝 (${state.profile.followerCount})"
        }
        val listState = when (type) {
            PeopleUserListType.Following -> state.followingState
            PeopleUserListType.Followers -> state.followersState
        }
        PeopleUserListSheet(
            title = title,
            state = listState,
            changingItemFollowIds = state.changingItemFollowIds,
            onLoadMore = { onEvent(PeopleEvent.LoadMoreUserList(type)) },
            onToggleFollow = { onEvent(PeopleEvent.ToggleItemFollow(it)) },
            onPersonClick = {
                onNavigate(
                    Person(
                        id = it.id,
                        name = it.name,
                        urlToken = it.urlToken ?: "",
                    ),
                )
            },
            onDismiss = { activeUserListType = null },
        )
    }
}

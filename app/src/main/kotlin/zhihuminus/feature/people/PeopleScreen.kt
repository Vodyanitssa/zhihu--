package com.zhihuminus.feature.people

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.people.components.EmptyHint
import com.zhihuminus.feature.people.components.PeopleCollectionListItem
import com.zhihuminus.feature.people.components.PeopleCreationsTab
import com.zhihuminus.feature.people.components.PeopleFollowingSubscriptionsTab
import com.zhihuminus.feature.people.components.PeopleUserInfoHeader
import com.zhihuminus.feature.people.components.PeopleUserListSheet
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.Question
import com.zhihuminus.ui.components.FeedCard
import com.zhihuminus.ui.components.PaginatedList
import com.zhihuminus.ui.components.ProgressIndicatorFooter
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import com.zhihuminus.navigation.Search as SearchDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    state: PeopleUiState,
    onEvent: (PeopleEvent) -> Unit,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
    onLinkClick: (String) -> Unit,
    onImagePreview: (String) -> Unit,
    onExternalUrl: (String) -> Unit,
    initialSelection: PeopleInitialSelection = PeopleInitialSelection(),
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val primaryPagerState = rememberPagerState(
        initialPage = initialSelection.primaryTab.ordinal,
        pageCount = { PeoplePrimaryTab.entries.size },
    )
    var activeUserListType by remember { mutableStateOf(initialSelection.initialUserListType) }
    var menuExpanded by remember { mutableStateOf(false) }

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput) {
                    flingJob?.cancel()
                }
                val delta = available.y
                if (delta < 0f && headerHeightPx > 0f && headerOffsetPx > -headerHeightPx) {
                    val oldOffset = headerOffsetPx
                    val newOffset = (oldOffset + delta).coerceIn(-headerHeightPx, 0f)
                    headerOffsetPx = newOffset
                    return Offset(0f, newOffset - oldOffset)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f && headerHeightPx > 0f && headerOffsetPx < 0f) {
                    val oldOffset = headerOffsetPx
                    val newOffset = (oldOffset + delta).coerceIn(-headerHeightPx, 0f)
                    headerOffsetPx = newOffset
                    return Offset(0f, newOffset - oldOffset)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val deltaV = available.y
                if (deltaV > 0f && headerHeightPx > 0f && headerOffsetPx < 0f) {
                    val startOffset = headerOffsetPx
                    val anim = Animatable(startOffset)
                    flingJob?.cancel()
                    flingJob = coroutineScope.launch {
                        anim.animateTo(
                            targetValue = 0f,
                            initialVelocity = deltaV,
                        ) {
                            headerOffsetPx = value
                        }
                    }
                    return Velocity(0f, deltaV)
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val isCollapsed by remember {
        derivedStateOf {
            headerHeightPx > 0f && (headerHeightPx + headerOffsetPx) < 40f
        }
    }

    LaunchedEffect(primaryPagerState.currentPage) {
        val tab = PeoplePrimaryTab.entries[primaryPagerState.currentPage]
        onEvent(PeopleEvent.PrimaryTabSelected(tab))
    }

    LaunchedEffect(activeUserListType) {
        activeUserListType?.let { onEvent(PeopleEvent.LoadUserListIfNeeded(it)) }
    }

    val isCurrentTabRefreshing = when (PeoplePrimaryTab.entries[primaryPagerState.currentPage]) {
        PeoplePrimaryTab.Creations -> when (state.selectedCreationTab) {
            PeopleCreationTab.Answers -> state.answersState.isRefreshing
            PeopleCreationTab.Articles -> state.articlesState.isRefreshing
            PeopleCreationTab.Pins -> state.pinsState.isRefreshing
            PeopleCreationTab.Columns -> state.columnsState.isRefreshing
            PeopleCreationTab.Questions -> state.questionsState.isRefreshing
        }
        PeoplePrimaryTab.Activities -> state.activitiesState.isRefreshing
        PeoplePrimaryTab.Collections -> state.collectionsState.isRefreshing
        PeoplePrimaryTab.FollowingSubscriptions -> when (state.selectedSubscriptionTab) {
            0 -> state.followingColumnsState.isRefreshing
            1 -> state.followingTopicsState.isRefreshing
            2 -> state.followingQuestionsState.isRefreshing
            3 -> state.followingCollectionsState.isRefreshing
            else -> false
        }
    }
    val isRefreshing = state.isRefreshingProfile || isCurrentTabRefreshing

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    AnimatedVisibility(
                        visible = isCollapsed,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.profile.avatarUrl.isNotBlank()) {
                                AsyncImage(
                                    model = state.profile.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .size(28.dp)
                                        .clip(CircleShape),
                                )
                            }
                            Text(
                                text = state.profile.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                actions = {
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
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "搜索 TA 的创作")
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("分享主页") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.profile.isBlocking) "移出黑名单" else "加入黑名单") },
                                leadingIcon = {
                                    Icon(
                                        if (state.profile.isBlocking) Icons.Default.Check else Icons.Default.Block,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEvent(PeopleEvent.ToggleBlock)
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                onEvent(PeopleEvent.RefreshProfile)
                val currentTab = PeoplePrimaryTab.entries[primaryPagerState.currentPage]
                if (currentTab == PeoplePrimaryTab.Creations) {
                    onEvent(PeopleEvent.RefreshCreation(state.selectedCreationTab))
                } else {
                    onEvent(PeopleEvent.RefreshPrimary(currentTab))
                }
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .nestedScroll(nestedScrollConnection)
                    .fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (headerHeightPx > 0f) {
                                Modifier.height(
                                    with(density) {
                                        (headerHeightPx + headerOffsetPx).coerceAtLeast(0f).toDp()
                                    },
                                )
                            } else {
                                Modifier.wrapContentHeight()
                            },
                        ).clipToBounds(),
                ) {
                    PeopleUserInfoHeader(
                        profile = state.profile,
                        onFollowToggle = { onEvent(PeopleEvent.ToggleFollow) },
                        onFollowingClick = { activeUserListType = PeopleUserListType.Following },
                        onFollowersClick = { activeUserListType = PeopleUserListType.Followers },
                        onAvatarClick = onImagePreview,
                        onExternalUrlClick = onExternalUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .onSizeChanged { size ->
                                if (size.height > 0) {
                                    headerHeightPx = size.height.toFloat()
                                }
                            },
                    )
                }

                PrimaryTabRow(
                    selectedTabIndex = primaryPagerState.currentPage,
                    modifier = Modifier.fillMaxWidth(),
                    divider = {},
                ) {
                    PeoplePrimaryTab.entries.forEachIndexed { index, tab ->
                        val isSelected = primaryPagerState.currentPage == index
                        Tab(
                            selected = isSelected,
                            onClick = {
                                coroutineScope.launch {
                                    primaryPagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                )
                            },
                        )
                    }
                }

                HorizontalPager(
                    state = primaryPagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { page ->
                    when (page) {
                        0 -> {
                            PeopleCreationsTab(
                                state = state,
                                onEvent = onEvent,
                                onNavigate = onNavigate,
                                onLinkClick = onLinkClick,
                            )
                        }

                        1 -> {
                            if (state.activitiesState.items.isEmpty() && state.activitiesState.hasLoaded && !state.activitiesState.isLoading) {
                                EmptyHint(message = "暂无动态")
                            } else {
                                PaginatedList(
                                    items = state.activitiesState.items,
                                    onLoadMore = { onEvent(PeopleEvent.LoadMorePrimary(PeoplePrimaryTab.Activities)) },
                                    isEnd = { state.activitiesState.isEnd },
                                    footer = if (state.isRefreshingProfile || state.activitiesState.isRefreshing) null else ProgressIndicatorFooter,
                                    modifier = Modifier.fillMaxSize(),
                                    key = { it.stableKey },
                                ) { item ->
                                    FeedCard(
                                        item = item,
                                        modifier = Modifier,
                                        showSourceLabel = true,
                                        onClick = {
                                            item.navDestination?.let(onNavigate)
                                        },
                                    )
                                }
                            }
                        }

                        2 -> {
                            if (state.collectionsState.items.isEmpty() && state.collectionsState.hasLoaded && !state.collectionsState.isLoading) {
                                EmptyHint(message = "暂无收藏")
                            } else {
                                PaginatedList(
                                    items = state.collectionsState.items,
                                    onLoadMore = { onEvent(PeopleEvent.LoadMorePrimary(PeoplePrimaryTab.Collections)) },
                                    isEnd = { state.collectionsState.isEnd },
                                    footer = if (state.isRefreshingProfile || state.collectionsState.isRefreshing) null else ProgressIndicatorFooter,
                                    modifier = Modifier.fillMaxSize(),
                                    key = { it.id },
                                ) { collection ->
                                    PeopleCollectionListItem(
                                        collection = collection,
                                        onClick = { onNavigate(CollectionContent(collection.id)) },
                                    )
                                }
                            }
                        }

                        3 -> {
                            PeopleFollowingSubscriptionsTab(
                                selectedIndex = state.selectedSubscriptionTab,
                                onTabSelect = { onEvent(PeopleEvent.SubscriptionTabSelected(it)) },
                                onLoadMore = { onEvent(PeopleEvent.LoadMoreSubscription(it)) },
                                columnsState = state.followingColumnsState,
                                topicsState = state.followingTopicsState,
                                questionsState = state.followingQuestionsState,
                                collectionsState = state.followingCollectionsState,
                                onColumnClick = { onLinkClick(it.webUrl) },
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
                        urlToken = it.urlToken,
                    ),
                )
            },
            onDismiss = { activeUserListType = null },
        )
    }
}

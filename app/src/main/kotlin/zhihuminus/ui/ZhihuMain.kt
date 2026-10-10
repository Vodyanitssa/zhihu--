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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.zhihuminus.feature.collection.CollectionContentRoute
import com.zhihuminus.feature.collection.CollectionRoute
import com.zhihuminus.feature.daily.DailyRoute
import com.zhihuminus.feature.follow.FollowRoute
import com.zhihuminus.feature.history.HistoryRoute
import com.zhihuminus.feature.home.HomeRoute
import com.zhihuminus.feature.notification.NotificationRoute
import com.zhihuminus.feature.notification.NotificationTimelineRoute
import com.zhihuminus.feature.notification.PrivateMessageRoute
import com.zhihuminus.feature.people.PeopleRoute
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.search.SearchRoute
import com.zhihuminus.feature.topic.TopicRoute
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.Column
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Navigator
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.PostTypeNavType
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.navigation.Topic
import com.zhihuminus.ui.components.LocalFeedCardConfig
import com.zhihuminus.ui.components.LocalTopLevelReselectFlow
import com.zhihuminus.ui.components.rememberFeedCardConfig
import com.zhihuminus.ui.subscreens.AppearanceSettingsScreen
import com.zhihuminus.ui.subscreens.NotificationSettingsScreen
import com.zhihuminus.ui.subscreens.OpenSourceLicensesScreen
import com.zhihuminus.ui.subscreens.SettingsSearchScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.milliseconds

private sealed class MainTabPage(
    val bottomDestination: TopLevelDestination,
    val key: String,
) {
    data object HomePage : MainTabPage(Home, "home")

    data object FollowPage : MainTabPage(Follow, "follow")

    data object DailyPage : MainTabPage(Daily, "daily")
}

/**
 * Zhihu++ 的共享应用主壳。
 *
 * 这个 composable 是顶层体验的唯一所有者：渲染可配置底部导航栏，承载仅可通过底栏点击/深链接切换的主 tab 页面，
 * 向子页面提供 [LocalNavigator]，
 * 并注册跨平台共享的 typed [NavDestination] route。设计上把顶层 tab 收在 [MainTabs] 内部，而不是把每个 tab
 * 都作为独立 NavHost 页面 push，这样 tab 重选、回到顶部、顶/底栏自动隐藏和持久化 tab 选择都能使用同一套状态模型。
 *
 * 用户可见的主壳设置通过 [preferenceState] 流入。设置页退出时只 reload 这份状态，不重建 NavHost，从而在应用底栏和主题相关变更时
 * 保留已加载页面、返回栈和滚动位置。
 */
@Suppress("RestrictedApi")
@Composable
fun ZhihuMain(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    mainTabNavigationTarget: TopLevelDestination?,
    navigate: (NavDestination) -> Unit,
    consumeMainTabNavigationTarget: (TopLevelDestination) -> Unit,
    preferenceState: ZhihuMainPreferenceState,
    isDarkTheme: Boolean,
    postContent: @Composable (PostDestination, NavBackStackEntry) -> Unit = { _, _ -> },
    questionContent: @Composable (Question, NavBackStackEntry) -> Unit = { _, _ -> },
    columnContent: @Composable (Column, NavBackStackEntry) -> Unit = { _, _ -> },
) {
    val bottomPadding = ScaffoldDefaults.contentWindowInsets.asPaddingValues().calculateBottomPadding()
    val tapToScrollToTopEnabled = preferenceState.tapToScrollToTopEnabled
    val autoHideBottomBar = preferenceState.autoHideBottomBar
    val startDestination = preferenceState.startDestination
    var isReadingPlayerExpandedByUser by remember { mutableStateOf(false) }

    val navEntry by navController.currentBackStackEntryAsState()
    val showMainNavigation = navEntry?.destination?.hasRoute<MainTabs>() == true
    val isOnReadingDetail = navEntry?.destination?.hasRoute<PostDestination>() == true ||
        navEntry?.destination?.hasRoute<Question>() == true ||
        // Pin is now PostDestination with PostType.Pin, covered by hasRoute<PostDestination>() above
        navEntry?.destination?.hasRoute<PostDestination>() == true
    val shouldCompactPlayerOnBackgroundInteraction by rememberUpdatedState(
        isReadingPlayerExpandedByUser && !isOnReadingDetail,
    )

    val reselectFlow = remember { MutableSharedFlow<TopLevelDestination>(extraBufferCapacity = 64) }
    // 滚动时自动隐藏底部导航栏
    var isBottomBarVisible by remember { mutableStateOf(true) }
    val bottomBarScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                when {
                    available.y < -3f -> isBottomBarVisible = false
                    available.y > 3f -> isBottomBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val bottomBarItems = remember {
        listOf(
            Triple(Home, "主页", Icons.Filled.Home),
            Triple(Follow, "关注", Icons.Filled.Group),
            Triple(Daily, "日报", Icons.Filled.Newspaper),
        )
    }

    val mainTabPages = remember {
        listOf(
            MainTabPage.HomePage,
            MainTabPage.FollowPage,
            MainTabPage.DailyPage,
        )
    }

    fun pageIndexForDestination(destination: TopLevelDestination): Int = when (destination) {
        Home -> 0
        Follow -> 1
        Daily -> 2
        else -> when (startDestination) {
            Follow -> 1
            Daily -> 2
            else -> 0
        }
    }

    var currentTabIndex by rememberSaveable {
        mutableIntStateOf(pageIndexForDestination(startDestination))
    }

    var currentMainTabDestination by remember { mutableStateOf(startDestination) }

    fun navigateTopLevel(destination: TopLevelDestination) {
        currentTabIndex = pageIndexForDestination(destination)
    }

    LaunchedEffect(currentTabIndex) {
        mainTabPages.getOrNull(currentTabIndex)?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
        }
    }

    BackHandler(currentTabIndex != 0) {
        currentTabIndex = 0
    }

    LaunchedEffect(mainTabNavigationTarget) {
        mainTabNavigationTarget?.let { destination ->
            // 平台适配层会把旧的顶层 route 请求映射到 MainTabs。这里消费该请求，
            // 让 deeplink 等调用方仍能选中 Home/Follow 等 tab，而不是把旧 route 压入返回栈。
            currentTabIndex = pageIndexForDestination(destination)
            consumeMainTabNavigationTarget(destination)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(bottomBarScrollConnection),
            bottomBar = {
                if (navEntry != null) {
                    // 页面切换时重置底部导航栏可见状态
                    LaunchedEffect(navEntry) { isBottomBarVisible = true }
                    val currentBottomDestination = mainTabPages
                        .getOrNull(currentTabIndex)
                        ?.bottomDestination
                    AnimatedVisibility(
                        visible = showMainNavigation && (!autoHideBottomBar || isBottomBarVisible),
                        enter = slideInVertically(tween(200)) { it },
                        exit = slideOutVertically(tween(200)) { it },
                    ) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.height(
                                64.dp + bottomPadding,
                            ),
                        ) {
                            @Composable
                            fun Item(
                                destination: TopLevelDestination,
                                label: String,
                                icon: ImageVector,
                            ) {
                                NavigationBarItem(
                                    selected = currentBottomDestination == destination,
                                    onClick = {
                                        isReadingPlayerExpandedByUser = false
                                        if (currentBottomDestination != destination) {
                                            navigateTopLevel(destination)
                                        } else if (tapToScrollToTopEnabled) {
                                            reselectFlow.tryEmit(destination)
                                        }
                                    },
                                    label = { Text(label) },
                                    alwaysShowLabel = true,
                                    colors = if (!isDarkTheme) {
                                        NavigationBarItemDefaults.colors().copy(
                                            selectedIndicatorColor =
                                                MaterialTheme.colorScheme.secondaryContainer
                                                    .copy(alpha = 0.92f)
                                                    .compositeOver(MaterialTheme.colorScheme.secondary),
                                        )
                                    } else {
                                        NavigationBarItemDefaults.colors()
                                    },
                                    icon = {
                                        Icon(icon, contentDescription = label)
                                    },
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }

                            bottomBarItems.forEach { item ->
                                Item(item.first, item.second, item.third)
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            CompositionLocalProvider(
                LocalNavigator provides Navigator(
                    onNavigate = { destination ->
                        navigate(destination)
                    },
                    onNavigateBack = navController::popBackStack,
                    onNavigateTopLevel = ::navigateTopLevel,
                ),
                LocalFeedCardConfig provides rememberFeedCardConfig(),
                LocalTopLevelReselectFlow provides reselectFlow,
            ) {
                NavHost(
                    navController,
                    modifier = Modifier.pointerInput(Unit) {
                        while (true) {
                            awaitPointerEventScope {
                                awaitFirstDown(
                                    requireUnconsumed = false,
                                    pass = PointerEventPass.Initial,
                                )
                                while (
                                    awaitPointerEvent(PointerEventPass.Final)
                                        .changes
                                        .any { it.pressed }
                                ) {
                                    // 等手势完成后再重组，避免取消同一次背景点击或滚动。
                                }
                            }
                            if (shouldCompactPlayerOnBackgroundInteraction) {
                                delay(100.milliseconds)
                                isReadingPlayerExpandedByUser = false
                            }
                        }
                    },
                    startDestination = MainTabs,
                    enterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { it } + fadeIn(animationSpec = tween(durationMillis = 250))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { -it / 4 } + fadeOut(animationSpec = tween(durationMillis = 200, easing = LinearEasing), targetAlpha = 0.5f)
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { -it / 4 } + fadeIn(animationSpec = tween(durationMillis = 250))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
                        ) { it } + fadeOut(animationSpec = tween(durationMillis = 250))
                    },
                ) {
                    composable<MainTabs> {
                        MainTabsContent(
                            currentTabIndex = currentTabIndex,
                            pages = mainTabPages,
                            innerPadding = innerPadding,
                        )
                    }
                    composable<Question> { navEntry ->
                        val question: Question = navEntry.toRoute()
                        questionContent(question, navEntry)
                    }
                    composable<Topic> { navEntry ->
                        TopicRoute(navEntry.toRoute())
                    }
                    composable<Follow> {
                        FollowRoute(
                            innerPadding = innerPadding,
                        )
                    }
                    composable<Daily> {
                        DailyRoute()
                    }
                    composable<History> {
                        val navigator = LocalNavigator.current
                        HistoryRoute(
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    composable<Account> {
                        val navigator = LocalNavigator.current
                        AccountSettingScreen(
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    composable<Search>(
                        enterTransition = {
                            if (initialState.destination.hasRoute<Search>()) {
                                EnterTransition.None
                            } else {
                                fadeIn(animationSpec = tween(durationMillis = 240)) +
                                    slideInVertically(animationSpec = tween(durationMillis = 280)) { it / 16 } +
                                    scaleIn(
                                        animationSpec = tween(durationMillis = 280),
                                        initialScale = 0.985f,
                                    )
                            }
                        },
                        popExitTransition = {
                            if (targetState.destination.hasRoute<Search>()) {
                                ExitTransition.None
                            } else {
                                fadeOut(animationSpec = tween(durationMillis = 180)) +
                                    slideOutVertically(animationSpec = tween(durationMillis = 220)) { it / 20 } +
                                    scaleOut(
                                        animationSpec = tween(durationMillis = 220),
                                        targetScale = 0.985f,
                                    )
                            }
                        },
                    ) { navEntry ->
                        val search: Search = navEntry.toRoute()
                        val navigator = LocalNavigator.current
                        SearchRoute(
                            search = search,
                            onBack = navigator.onNavigateBack,
                        )
                    }
                    composable<Collections> { navEntry ->
                        val navigator = LocalNavigator.current
                        val data: Collections = navEntry.toRoute()
                        CollectionRoute(
                            urlToken = data.userToken,
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    composable<CollectionContent> { navEntry ->
                        val navigator = LocalNavigator.current
                        val content: CollectionContent = navEntry.toRoute()
                        CollectionContentRoute(
                            collectionId = content.collectionId,
                            onNavigateBack = navigator.onNavigateBack,
                            onDestinationClick = { destination ->
                                destination?.let(navigator.onNavigate)
                            },
                        )
                    }
                    composable<Person> { navEntry ->
                        val person: Person = navEntry.toRoute()
                        val navigator = LocalNavigator.current
                        PeopleRoute(
                            person = person,
                            onNavigate = navigator.onNavigate,
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    composable<Column> { navEntry ->
                        val column: Column = navEntry.toRoute()
                        columnContent(column, navEntry)
                    }
                    composable<PostDestination>(
                        typeMap = mapOf(typeOf<PostType>() to PostTypeNavType),
                    ) { navEntry ->
                        val destination: PostDestination = navEntry.toRoute()
                        postContent(destination, navEntry)
                    }
                    composable<Notification> {
                        val navigator = LocalNavigator.current
                        NotificationRoute(
                            onNavigateBack = navigator.onNavigateBack,
                            onCategoryClick = { category ->
                                navigator.onNavigate(
                                    Notification.Entry(category.entryName, category.detailTitle),
                                )
                            },
                            onInvitationClick = { navigator.onNavigate(Notification.Invitations) },
                            onConversationClick = { destination -> navigator.onNavigate(destination) },
                        )
                    }
                    composable<Notification.Entry> { navEntry ->
                        val navigator = LocalNavigator.current
                        val entry: Notification.Entry = navEntry.toRoute()
                        NotificationTimelineRoute(
                            entryName = entry.entryName,
                            title = entry.title,
                            onNavigateBack = navigator.onNavigateBack,
                            onDestinationClick = { destination -> navigator.onNavigate(destination) },
                        )
                    }
                    composable<Notification.Invitations> {
                        val navigator = LocalNavigator.current
                        NotificationTimelineRoute(
                            entryName = "invite",
                            title = "邀请回答",
                            onNavigateBack = navigator.onNavigateBack,
                            onDestinationClick = { destination -> navigator.onNavigate(destination) },
                        )
                    }
                    composable<Notification.Message> { navEntry ->
                        val navigator = LocalNavigator.current
                        val destination: Notification.Message = navEntry.toRoute()
                        PrivateMessageRoute(
                            destination = destination,
                            onNavigateBack = navigator.onNavigateBack,
                        )
                    }
                    composable<Account.NotificationSettings> { navEntry ->
                        NotificationSettingsScreen(
                            setting = navEntry.toRoute<Account.NotificationSettings>().setting,
                        )
                    }
                    composable<Account.AppearanceSettings> { navEntry ->
                        val args = navEntry.toRoute<Account.AppearanceSettings>()
                        AppearanceSettingsScreen(
                            setting = args.setting,
                            onExit = preferenceState::reload,
                        )
                    }
                    composable<Account.SettingsSearch> {
                        SettingsSearchScreen()
                    }
                    composable<Account.OpenSourceLicenses> {
                        OpenSourceLicensesScreen()
                    }
                }
            }
        }
    }
}

/**
 * 渲染可配置底部导航主壳内的页面。不支持左右滑动手势，只能通过底部导航栏或深链接等程序化导航切换，
 * 切换时附带一个跟随切换方向的轻微横滑 + 淡入淡出过渡。
 *
 * 每个页面都接收主壳给出的 [innerPadding]，保证系统栏、底部栏和子页面之间的留白一致。
 * [SaveableStateProvider] 按 [MainTabPage.key] 隔离各 tab 的可保存状态，切走再切回时滚动位置等不丢失。
 */
@Composable
private fun MainTabsContent(
    currentTabIndex: Int,
    pages: List<MainTabPage>,
    innerPadding: PaddingValues,
) {
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = currentTabIndex,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            val slideSpec = tween<IntOffset>(durationMillis = 320, easing = FastOutSlowInEasing)
            val fadeSpec = tween<Float>(durationMillis = 280, easing = LinearOutSlowInEasing)
            val fadeOutSpec = tween<Float>(durationMillis = 200, easing = FastOutLinearInEasing)
            if (targetState >= initialState) {
                (fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { it / 4 }) togetherWith
                    (fadeOut(fadeOutSpec) + slideOutHorizontally(slideSpec) { -it / 4 })
            } else {
                (fadeIn(fadeSpec) + slideInHorizontally(slideSpec) { -it / 4 }) togetherWith
                    (fadeOut(fadeOutSpec) + slideOutHorizontally(slideSpec) { it / 4 })
            }
        },
        label = "MainTabs",
    ) { tabIndex ->
        val page = pages.getOrNull(tabIndex) ?: return@AnimatedContent
        stateHolder.SaveableStateProvider(page.key) {
            when (page) {
                MainTabPage.HomePage -> HomeRoute(
                    innerPadding = innerPadding,
                )

                MainTabPage.FollowPage -> FollowRoute(
                    innerPadding = innerPadding,
                )

                MainTabPage.DailyPage -> DailyRoute()
            }
        }
    }
}

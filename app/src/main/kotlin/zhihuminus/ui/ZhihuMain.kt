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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Whatshot
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.Column
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.HotList
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.MyCollections
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Navigator
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.OnlineHistory
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.PostTypeNavType
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.navigation.Topic
import com.zhihuminus.platform.PlatformBackHandler
import com.zhihuminus.ui.subscreens.AppearanceSettingsScreen
import com.zhihuminus.ui.subscreens.IdentityManagementScreen
import com.zhihuminus.ui.subscreens.OpenSourceLicensesScreen
import com.zhihuminus.ui.subscreens.SettingsSearchScreen
import com.zhihuminus.ui.subscreens.SystemAndUpdateSettingsScreen
import kotlinx.coroutines.delay
import kotlin.reflect.typeOf
import kotlin.time.Duration.Companion.milliseconds

private sealed class MainTabPage(
    val bottomDestination: TopLevelDestination,
    val key: String,
) {
    data object HomePage : MainTabPage(Home, "home")

    data object FollowPage : MainTabPage(Follow, "follow")

    data object HotListPage : MainTabPage(HotList, "hotlist")

    data object DailyPage : MainTabPage(Daily, "daily")

    data object OnlineHistoryPage : MainTabPage(OnlineHistory, "online_history")

    data object MyCollectionsPage : MainTabPage(MyCollections, "my_collections")

    data object AccountPage : MainTabPage(Account, "account")
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
    val collectionDirectBrowseEnabled = preferenceState.collectionDirectBrowseEnabled
    val selectedBottomBarItemKeys = preferenceState.selectedBottomBarItemKeys
    val startDestination = preferenceState.startDestination
    val reloadBottomBarPreferences = preferenceState::reload
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

    var scrollToTopTrigger by remember { mutableIntStateOf(0) }
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

    val allBottomBarItems = listOf(
        Triple(Home, "主页", Icons.Filled.Home),
        Triple(Follow, "关注", Icons.Filled.Group),
        Triple(HotList, "热榜", Icons.Filled.Whatshot),
        Triple(Daily, "日报", Icons.Filled.Newspaper),
        Triple(OnlineHistory, "历史", Icons.Filled.History),
        Triple(MyCollections, "收藏夹", Icons.Filled.Bookmarks),
        Triple(Account, "账号", Icons.Filled.ManageAccounts),
    )
    val bottomBarItems = selectedBottomBarItemKeys.mapNotNull { key ->
        allBottomBarItems.firstOrNull { it.first.name == key }
    }

    val mainTabPages = remember(bottomBarItems) {
        bottomBarItems.flatMap { item ->
            when (item.first) {
                Home -> listOf(MainTabPage.HomePage)
                Follow -> listOf(MainTabPage.FollowPage)
                HotList -> listOf(MainTabPage.HotListPage)
                Daily -> listOf(MainTabPage.DailyPage)
                OnlineHistory -> listOf(MainTabPage.OnlineHistoryPage)
                MyCollections -> listOf(MainTabPage.MyCollectionsPage)
                Account -> listOf(MainTabPage.AccountPage)
                else -> emptyList()
            }
        }
    }

    fun pageIndexForDestination(destination: TopLevelDestination): Int = mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == destination::class
        }.takeIf { it >= 0 } ?: mainTabPages
        .indexOfFirst {
            it.bottomDestination::class == startDestination::class
        }.takeIf { it >= 0 } ?: 0

    var currentTabIndex by rememberSaveable {
        mutableIntStateOf(pageIndexForDestination(startDestination))
    }

    var currentMainTabDestination by remember { mutableStateOf(startDestination) }

    fun navigateTopLevel(destination: TopLevelDestination) {
        currentTabIndex = pageIndexForDestination(destination)
    }

    LaunchedEffect(currentTabIndex, mainTabPages) {
        mainTabPages.getOrNull(currentTabIndex)?.bottomDestination?.let { destination ->
            currentMainTabDestination = destination
        }
    }

    PlatformBackHandler(currentTabIndex != 0) {
        currentTabIndex = 0
    }

    LaunchedEffect(mainTabNavigationTarget, mainTabPages) {
        mainTabNavigationTarget?.let { destination ->
            // 平台适配层会把旧的顶层 route 请求映射到 MainTabs。这里消费该请求，
            // 让 deeplink 等调用方仍能选中 Home/Follow 等 tab，而不是把旧 route 压入返回栈。
            currentTabIndex = pageIndexForDestination(destination)
            consumeMainTabNavigationTarget(destination)
        }
    }

    LaunchedEffect(mainTabPages) {
        if (mainTabPages.isNotEmpty()) {
            val currentDestinationStillVisible = mainTabPages.any {
                it.bottomDestination::class == currentMainTabDestination::class
            }
            val targetDestination = if (currentDestinationStillVisible) {
                currentMainTabDestination
            } else {
                startDestination
            }
            val targetPage = pageIndexForDestination(targetDestination)
            if (currentTabIndex != targetPage || currentTabIndex !in mainTabPages.indices) {
                currentTabIndex = targetPage
            }
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
                                    currentBottomDestination?.let { it::class == destination::class } == true,
                                    onClick = {
                                        isReadingPlayerExpandedByUser = false
                                        if (currentBottomDestination?.let { it::class == destination::class } != true) {
                                            navigateTopLevel(destination)
                                        } else if (tapToScrollToTopEnabled) {
                                            scrollToTopTrigger++
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
                        slideInHorizontally(tween(300)) { it }
                    },
                    exitTransition = {
                        ExitTransition.None
                    },
                    popEnterTransition = {
                        EnterTransition.None
                    },
                    popExitTransition = {
                        slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                    },
                ) {
                    composable<MainTabs> {
                        MainTabsContent(
                            currentTabIndex = currentTabIndex,
                            pages = mainTabPages,
                            scrollToTopTrigger = scrollToTopTrigger,
                            innerPadding = innerPadding,
                            collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                        )
                    }
                    composable<Question> { navEntry ->
                        val question: Question = navEntry.toRoute()
                        questionContent(question, navEntry)
                    }
                    composable<Topic> { navEntry ->
                        TopicScreen(navEntry.toRoute())
                    }
                    composable<HotList> {
                        HotListScreen(innerPadding)
                    }
                    composable<Follow> {
                        FollowScreen(
                            scrollToTopTrigger = scrollToTopTrigger,
                            innerPadding = innerPadding,
                        )
                    }
                    composable<Daily> {
                        DailyScreen()
                    }
                    composable<OnlineHistory> {
                        OnlineHistoryScreen()
                    }
                    composable<Account> {
                        AccountSettingScreen(innerPadding)
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
                        SearchScreen(search)
                    }
                    composable<Collections> { navEntry ->
                        val data: Collections = navEntry.toRoute()
                        CollectionScreen(data.userToken)
                    }
                    composable<CollectionContent> { navEntry ->
                        val content: CollectionContent = navEntry.toRoute()
                        CollectionContentScreen(content.collectionId)
                    }
                    composable<Person> { navEntry ->
                        val person: Person = navEntry.toRoute()
                        PeopleScreen(person)
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
                        NotificationScreen()
                    }
                    composable<Notification.Entry> { navEntry ->
                        val entry: Notification.Entry = navEntry.toRoute()
                        NotificationTimelineScreen(entry.entryName, entry.title)
                    }
                    composable<Notification.Invitations> {
                        NotificationTimelineScreen("invite", "邀请回答")
                    }
                    composable<Notification.Message> { navEntry ->
                        PrivateMessageScreen(navEntry.toRoute())
                    }
                    composable<Notification.NotificationSettings> { navEntry ->
                        NotificationSettingsScreen(
                            setting = navEntry.toRoute<Notification.NotificationSettings>().setting,
                        )
                    }
                    composable<Account.AppearanceSettings> { navEntry ->
                        val args = navEntry.toRoute<Account.AppearanceSettings>()
                        AppearanceSettingsScreen(
                            setting = args.setting,
                            onExit = reloadBottomBarPreferences,
                        )
                    }
                    composable<Account.IdentityManagement> {
                        IdentityManagementScreen()
                    }
                    composable<Account.SystemAndUpdateSettings> { navEntry ->
                        SystemAndUpdateSettingsScreen()
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
    scrollToTopTrigger: Int,
    innerPadding: PaddingValues,
    collectionDirectBrowseEnabled: Boolean,
) {
    val stateHolder = rememberSaveableStateHolder()
    AnimatedContent(
        targetState = currentTabIndex,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (targetState >= initialState) {
                (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { it / 16 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { -it / 16 })
            } else {
                (fadeIn(tween(200)) + slideInHorizontally(tween(200)) { -it / 16 }) togetherWith
                    (fadeOut(tween(160)) + slideOutHorizontally(tween(200)) { it / 16 })
            }
        },
        label = "MainTabs",
    ) { tabIndex ->
        val page = pages.getOrNull(tabIndex) ?: return@AnimatedContent
        stateHolder.SaveableStateProvider(page.key) {
            when (page) {
                MainTabPage.HomePage -> HomeScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                )

                MainTabPage.FollowPage -> FollowScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                    innerPadding = innerPadding,
                )

                MainTabPage.HotListPage -> HotListScreen(
                    innerPadding = innerPadding,
                    scrollToTopTrigger = scrollToTopTrigger,
                )

                MainTabPage.DailyPage -> DailyScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                )

                MainTabPage.OnlineHistoryPage -> OnlineHistoryScreen(
                    scrollToTopTrigger = scrollToTopTrigger,
                )

                MainTabPage.MyCollectionsPage -> MyCollectionsTopLevelPage(
                    scrollToTopTrigger = scrollToTopTrigger,
                    collectionDirectBrowseEnabled = collectionDirectBrowseEnabled,
                )

                MainTabPage.AccountPage -> AccountSettingScreen(innerPadding)
            }
        }
    }
}

@Composable
private fun MyCollectionsTopLevelPage(
    scrollToTopTrigger: Int,
    collectionDirectBrowseEnabled: Boolean,
) {
    val account = rememberAccountSettingsAccountState().value
    if (collectionDirectBrowseEnabled) {
        CollectionBrowseScreen(
            urlToken = account.urlToken,
            showBackButton = false,
            scrollToTopTrigger = scrollToTopTrigger,
        )
    } else {
        CollectionScreen(
            urlToken = account.urlToken,
            showBackButton = false,
        )
    }
}

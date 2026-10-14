package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.watchlater.WatchLaterItem
import com.zhihuminus.feature.watchlater.WatchLaterRepository
import com.zhihuminus.navigation.PostDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class FakeHomeRepository : HomeRepository {
        var fetchCallCount = 0
        var touchReportCount = 0
        var readReportCount = 0
        var markUninterestedCallCount = 0
        var markUninterestedResult = true
        var lastMarkUninterestedToken: String? = null
        var lastMarkUninterestedType: String? = null
        val itemsPage1 = listOf(
            FeedDisplayItem(
                title = "Item 1",
                summary = "Summary 1",
                details = "Details 1",
                navDestinationJson = PostDestination(
                    title = "Title 1",
                    type = PostType.Answer,
                    id = 1001L,
                    authorName = "Author 1",
                ).toFeedDisplayItemNavDestinationJson(),
            ),
            FeedDisplayItem(
                title = "Item 2",
                summary = "Summary 2",
                details = "Details 2",
                navDestinationJson = PostDestination(
                    title = "Title 2",
                    type = PostType.Article,
                    id = 2002L,
                    authorName = "Author 2",
                ).toFeedDisplayItemNavDestinationJson(),
            ),
        )
        val itemsPage2 = listOf(
            FeedDisplayItem(
                title = "Item 2",
                summary = "Summary 2",
                details = "Details 2",
                navDestinationJson = PostDestination(
                    title = "Title 2",
                    type = PostType.Article,
                    id = 2002L,
                    authorName = "Author 2",
                ).toFeedDisplayItemNavDestinationJson(),
            ), // 重复条目
            FeedDisplayItem(
                title = "Item 3",
                summary = "Summary 3",
                details = "Details 3",
                navDestinationJson = PostDestination(
                    title = "Title 3",
                    type = PostType.Pin,
                    id = 3003L,
                    authorName = "Author 3",
                ).toFeedDisplayItemNavDestinationJson(),
            ),
        )

        override suspend fun fetchRecommendFeed(nextUrl: String?): HomeFeedPage {
            fetchCallCount++
            return if (nextUrl == null) {
                HomeFeedPage(items = itemsPage1, nextUrl = "page2", isEnd = false)
            } else {
                HomeFeedPage(items = itemsPage2, nextUrl = null, isEnd = true)
            }
        }

        val reportedExposures = mutableListOf<FeedExposureRecord>()

        override suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
            touchReportCount += untouchedItems.size
        }

        override suspend fun reportExposures(records: List<FeedExposureRecord>) {
            reportedExposures.addAll(records)
            reportContentTouch(records.map { it.type to it.id })
        }

        override suspend fun reportContentRead(type: String, id: String) {
            readReportCount++
            lastReadType = type
            lastReadId = id
        }

        var lastReadType: String? = null
        var lastReadId: String? = null

        override suspend fun fetchUnreadNotificationCount(): Int = 5

        override suspend fun loadStartupSnapshot(): List<FeedDisplayItem> = emptyList()

        override suspend fun saveStartupSnapshot(items: List<FeedDisplayItem>) {}

        override suspend fun markUninterested(contentToken: String, contentType: String): Boolean {
            markUninterestedCallCount++
            lastMarkUninterestedToken = contentToken
            lastMarkUninterestedType = contentType
            return markUninterestedResult
        }
    }

    private class FakeWatchLaterRepository : WatchLaterRepository {
        val items = mutableListOf<WatchLaterItem>()
        private val _itemsFlow = MutableStateFlow<List<WatchLaterItem>>(emptyList())
        override val itemsFlow: StateFlow<List<WatchLaterItem>> = _itemsFlow.asStateFlow()

        override suspend fun getItems(): List<WatchLaterItem> = items.toList()

        override suspend fun addItem(item: WatchLaterItem) {
            items.add(0, item)
            _itemsFlow.value = items.toList()
        }

        override suspend fun deleteItem(id: Long, type: String) {
            items.removeAll { it.id == id && it.type.equals(type, ignoreCase = true) }
            _itemsFlow.value = items.toList()
        }

        override suspend fun clearAll() {
            items.clear()
            _itemsFlow.value = emptyList()
        }
    }

    @Test
    fun testHomeFeedPageCreation() {
        val repo = FakeHomeRepository()
        runBlocking {
            val page1 = repo.fetchRecommendFeed(null)
            assertEquals(2, page1.items.size)
            assertEquals("page2", page1.nextUrl)
            assertFalse(page1.isEnd)

            val page2 = repo.fetchRecommendFeed("page2")
            assertEquals(2, page2.items.size)
            assertTrue(page2.isEnd)
        }
    }

    @Test
    fun testFeedDeduplication() {
        val repo = FakeHomeRepository()
        runBlocking {
            val page1 = repo.fetchRecommendFeed(null)
            val page2 = repo.fetchRecommendFeed("page2")

            val currentItems = page1.items
            val existingKeys = currentItems.map { it.stableKey }.toSet()
            val newItems = page2.items.filter { it.stableKey !in existingKeys }
            val combined = currentItems + newItems

            assertEquals(3, combined.size)
            assertEquals(listOf("Item 1", "Item 2", "Item 3"), combined.map { it.title })
        }
    }

    @Test
    fun testFakeRepoTouchAndReadTracking() {
        val repo = FakeHomeRepository()
        runBlocking {
            repo.reportContentTouch(listOf("answer" to "1", "post" to "2"))
            assertEquals(2, repo.touchReportCount)

            repo.reportContentRead("post", "2")
            assertEquals(1, repo.readReportCount)
            assertEquals("post", repo.lastReadType)
            assertEquals("2", repo.lastReadId)
        }
    }

    @Test
    fun testItemsExposedReportsTouchAndZaWithCorrectTypes() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.items.size)

        // Simulate items exposed on screen
        viewModel.onEvent(HomeEvent.ItemsExposed(viewModel.uiState.items))
        advanceUntilIdle()

        assertEquals(2, repo.reportedExposures.size)
        assertEquals(2, repo.touchReportCount)

        // Item 1 is an Answer -> "answer"
        assertEquals("answer", repo.reportedExposures[0].type)
        assertEquals("1001", repo.reportedExposures[0].id)

        // Item 2 is an Article -> MUST be "post" for Zhihu API
        assertEquals("post", repo.reportedExposures[1].type)
        assertEquals("2002", repo.reportedExposures[1].id)

        // Re-exposing the same items should NOT report them again (deduplication)
        viewModel.onEvent(HomeEvent.ItemsExposed(viewModel.uiState.items))
        advanceUntilIdle()
        assertEquals(2, repo.reportedExposures.size)
        assertEquals(2, repo.touchReportCount)
    }

    @Test
    fun testContentClickReportsReadWithPostForArticle() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()
        val articleItem = viewModel.uiState.items.first { it.title == "Item 2" }

        viewModel.onEvent(HomeEvent.ContentClick(articleItem))
        advanceUntilIdle()

        assertEquals(1, repo.readReportCount)
        assertEquals("post", repo.lastReadType)
        assertEquals("2002", repo.lastReadId)
    }

    @Test
    fun testHomeViewModelUnauthenticatedDoesNotLoadFeedsOnStartup() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = false,
        )
        advanceUntilIdle()

        assertEquals(0, repo.fetchCallCount)
        assertTrue(viewModel.uiState.items.isEmpty())
    }

    @Test
    fun testHomeViewModelRefreshAfterLoginLoadsFeeds() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = false,
        )
        advanceUntilIdle()
        assertEquals(0, repo.fetchCallCount)

        viewModel.onEvent(HomeEvent.Refresh)
        advanceUntilIdle()

        assertEquals(1, repo.fetchCallCount)
        assertEquals(2, viewModel.uiState.items.size)
        assertEquals("Item 1", viewModel.uiState.items[0].title)
    }

    @Test
    fun testHomeViewModelAuthenticatedLoadsFeedsOnStartup() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()

        assertEquals(1, repo.fetchCallCount)
        assertEquals(2, viewModel.uiState.items.size)
    }

    @Test
    fun testMarkUninterestedSuccessRemovesItemAndShowsMessage() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.items.size)
        val targetItem = viewModel.uiState.items.first { it.title == "Item 1" }

        val effects = mutableListOf<HomeEffect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onEvent(HomeEvent.MarkUninterested(targetItem))
        advanceUntilIdle()

        assertEquals(1, repo.markUninterestedCallCount)
        assertEquals("1001", repo.lastMarkUninterestedToken)
        assertEquals("1", repo.lastMarkUninterestedType)
        assertEquals(1, viewModel.uiState.items.size)
        assertEquals(
            "Item 2",
            viewModel.uiState.items
                .first()
                .title,
        )
        assertTrue(effects.any { it is HomeEffect.ShowMessage && it.message == "已标记不感兴趣" })

        effectJob.cancel()
    }

    @Test
    fun testMarkUninterestedFailureRetainsItemAndShowsError() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        repo.markUninterestedResult = false
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.items.size)
        val targetItem = viewModel.uiState.items.first { it.title == "Item 2" }

        val effects = mutableListOf<HomeEffect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onEvent(HomeEvent.MarkUninterested(targetItem))
        advanceUntilIdle()

        assertEquals(1, repo.markUninterestedCallCount)
        assertEquals("2002", repo.lastMarkUninterestedToken)
        assertEquals("2", repo.lastMarkUninterestedType)
        assertEquals(2, viewModel.uiState.items.size)
        assertTrue(effects.any { it is HomeEffect.ShowMessage && it.message == "操作失败，请重试" })

        effectJob.cancel()
    }

    @Test
    fun testMarkUninterestedUnsupportedItemShowsNotice() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
        )
        advanceUntilIdle()
        val unsupportedItem = FeedDisplayItem(
            title = "Unsupported",
            summary = "No target",
            details = "details",
        )

        val effects = mutableListOf<HomeEffect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onEvent(HomeEvent.MarkUninterested(unsupportedItem))
        advanceUntilIdle()

        assertEquals(0, repo.markUninterestedCallCount)
        assertTrue(effects.any { it is HomeEffect.ShowMessage && it.message == "该条目不支持标记不感兴趣" })

        effectJob.cancel()
    }

    @Test
    fun testAddToWatchLaterSupportedItemAddsAndEmitsToast() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val watchLaterRepo = FakeWatchLaterRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
            watchLaterRepository = watchLaterRepo,
        )
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.items.size)
        val targetItem = viewModel.uiState.items.first { it.title == "Item 1" }

        val effects = mutableListOf<HomeEffect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onEvent(HomeEvent.AddToWatchLater(targetItem))
        advanceUntilIdle()

        assertEquals(1, watchLaterRepo.items.size)
        val added = watchLaterRepo.items.first()
        assertEquals(1001L, added.id)
        assertEquals("answer", added.type)
        assertEquals("Item 1", added.title)
        assertEquals("Summary 1", added.excerpt)
        assertTrue(effects.any { it is HomeEffect.ShowMessage && it.message == "已添加到稍后再看" })

        effectJob.cancel()
    }

    @Test
    fun testAddToWatchLaterUnsupportedItemShowsNotice() = runTest(testDispatcher) {
        val repo = FakeHomeRepository()
        val watchLaterRepo = FakeWatchLaterRepository()
        val viewModel = HomeViewModel(
            repository = repo,
            autoRefreshOnStartup = true,
            initialIsLoggedIn = true,
            watchLaterRepository = watchLaterRepo,
        )
        advanceUntilIdle()
        val unsupportedItem = FeedDisplayItem(
            title = "Unsupported",
            summary = "No target",
            details = "details",
        )

        val effects = mutableListOf<HomeEffect>()
        val effectJob = launch { viewModel.effect.collect { effects.add(it) } }

        viewModel.onEvent(HomeEvent.AddToWatchLater(unsupportedItem))
        advanceUntilIdle()

        assertEquals(0, watchLaterRepo.items.size)
        assertTrue(effects.any { it is HomeEffect.ShowMessage && it.message == "该条目不支持添加到稍后再看" })

        effectJob.cancel()
    }
}

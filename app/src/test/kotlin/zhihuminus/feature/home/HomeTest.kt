package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

        override suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
            touchReportCount += untouchedItems.size
        }

        override suspend fun reportContentRead(type: String, id: String) {
            readReportCount++
        }

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
            repo.reportContentTouch(listOf("answer" to "1", "article" to "2"))
            assertEquals(2, repo.touchReportCount)

            repo.reportContentRead("answer", "1")
            assertEquals(1, repo.readReportCount)
        }
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
}

package com.zhihuminus.feature.home

import com.zhihuminus.data.FeedDisplayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val itemsPage1 = listOf(
            FeedDisplayItem(title = "Item 1", summary = "Summary 1", details = "Details 1", navDestinationJson = "nav:1"),
            FeedDisplayItem(title = "Item 2", summary = "Summary 2", details = "Details 2", navDestinationJson = "nav:2"),
        )
        val itemsPage2 = listOf(
            FeedDisplayItem(title = "Item 2", summary = "Summary 2", details = "Details 2", navDestinationJson = "nav:2"), // 重复条目
            FeedDisplayItem(title = "Item 3", summary = "Summary 3", details = "Details 3", navDestinationJson = "nav:3"),
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
}

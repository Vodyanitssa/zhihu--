package com.zhihuminus.feature.search

import com.zhihuminus.data.FeedDisplayItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class TestSearchRepository : SearchRepository {
        var hotSearchesResult: List<HotSearchItem> = listOf(
            HotSearchItem(query = "Hot 1", hotShow = "100 万", label = "热"),
            HotSearchItem(query = "Hot 2", hotShow = "80 万"),
        )
        var shouldFailHotSearch = false
        var hotSearchEnabledFlag = true
        var searchHistoryEnabledFlag = true

        override suspend fun searchGeneral(
            query: String,
            tab: SearchTab,
            sort: SearchSortOption,
            contentType: SearchContentType,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<FeedDisplayItem> = SearchPage(emptyList(), null, true)

        override suspend fun searchPeople(
            query: String,
            sort: SearchSortOption,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<PeopleSearchResult> = SearchPage(emptyList(), null, true)

        override suspend fun searchTopics(
            query: String,
            sort: SearchSortOption,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<TopicSearchResult> = SearchPage(emptyList(), null, true)

        override suspend fun fetchHotSearches(): List<HotSearchItem> {
            if (shouldFailHotSearch) throw RuntimeException("HTTP 404 Not Found")
            return hotSearchesResult
        }

        override fun isHotSearchEnabled(): Boolean = hotSearchEnabledFlag

        override fun isSearchHistoryEnabled(): Boolean = searchHistoryEnabledFlag

        override suspend fun setTopicFollowing(topicId: String, following: Boolean): Result<Unit> = Result.success(Unit)

        override suspend fun setMemberFollowing(urlToken: String, following: Boolean): Result<Unit> = Result.success(Unit)

        override fun getSearchHistory(): List<String> = emptyList()

        override fun saveSearchHistory(history: List<String>) = Unit

        override fun clearSearchHistory() = Unit
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialHotSearchLoadedSuccessfully() = runTest(testDispatcher) {
        val repo = TestSearchRepository()
        val viewModel = SearchViewModel(initialQuery = "", repository = repo)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isHotSearchLoading)
        assertEquals(2, viewModel.uiState.hotSearchItems.size)
        assertEquals("Hot 1", viewModel.uiState.hotSearchItems[0].query)
    }

    @Test
    fun testInitialHotSearchFailureHandledGracefully() = runTest(testDispatcher) {
        val repo = TestSearchRepository().apply {
            shouldFailHotSearch = true
        }
        val viewModel = SearchViewModel(initialQuery = "", repository = repo)

        advanceUntilIdle()

        assertFalse(viewModel.uiState.isHotSearchLoading)
        assertTrue(viewModel.uiState.hotSearchItems.isEmpty())
    }

    @Test
    fun testRefreshHotSearchEventFailureHandledGracefully() = runTest(testDispatcher) {
        val repo = TestSearchRepository()
        val viewModel = SearchViewModel(initialQuery = "", repository = repo)
        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.hotSearchItems.size)

        repo.shouldFailHotSearch = true
        viewModel.onEvent(SearchEvent.RefreshHotSearch)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isHotSearchLoading)
    }
}

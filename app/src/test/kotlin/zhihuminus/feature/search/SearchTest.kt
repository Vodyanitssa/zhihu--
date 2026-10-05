package com.zhihuminus.feature.search

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.PeopleSearchResult
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {
    private class FakeSearchRepository : SearchRepository {
        var followingTopicIds = mutableSetOf<String>()
        var historyList = mutableListOf("kotlin", "compose")

        override suspend fun searchGeneral(
            query: String,
            tab: SearchTab,
            sort: SearchSortOption,
            contentType: SearchContentType,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<FeedDisplayItem> = SearchPage(
            items = listOf(FeedDisplayItem(title = "Search result for $query", summary = "summary", details = "details")),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun searchPeople(
            query: String,
            sort: SearchSortOption,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<PeopleSearchResult> = SearchPage(items = emptyList(), nextUrl = null, isEnd = true)

        override suspend fun searchTopics(
            query: String,
            sort: SearchSortOption,
            timeRange: SearchTimeRange,
            restrictedMemberHashId: String,
            nextUrl: String?,
        ): SearchPage<TopicSearchResult> = SearchPage(items = emptyList(), nextUrl = null, isEnd = true)

        override suspend fun fetchHotSearches(): List<HotSearchItem> = listOf(
            HotSearchItem(query = "Hot 1"),
            HotSearchItem(query = "Hot 2"),
        )

        var hotSearchEnabled: Boolean = true
        var searchHistoryEnabled: Boolean = true

        val followingMemberTokens = mutableSetOf<String>()

        override fun isHotSearchEnabled(): Boolean = hotSearchEnabled

        override fun isSearchHistoryEnabled(): Boolean = searchHistoryEnabled

        override suspend fun setTopicFollowing(topicId: String, following: Boolean): Result<Unit> {
            if (following) followingTopicIds.add(topicId) else followingTopicIds.remove(topicId)
            return Result.success(Unit)
        }

        override suspend fun setMemberFollowing(urlToken: String, following: Boolean): Result<Unit> {
            if (following) followingMemberTokens.add(urlToken) else followingMemberTokens.remove(urlToken)
            return Result.success(Unit)
        }

        override fun getSearchHistory(): List<String> = historyList.toList()

        override fun saveSearchHistory(history: List<String>) {
            historyList = history.toMutableList()
        }

        override fun clearSearchHistory() {
            historyList.clear()
        }
    }

    @Test
    fun testSearchRepositoryHistoryOperations() {
        val repo = FakeSearchRepository()
        assertEquals(listOf("kotlin", "compose"), repo.getSearchHistory())

        repo.saveSearchHistory(listOf("android", "kotlin"))
        assertEquals(listOf("android", "kotlin"), repo.getSearchHistory())

        repo.clearSearchHistory()
        assertTrue(repo.getSearchHistory().isEmpty())
    }

    @Test
    fun testSearchGeneralPage() {
        val repo = FakeSearchRepository()
        runBlocking {
            val page = repo.searchGeneral(
                query = "Android",
                tab = SearchTab.General,
                sort = SearchSortOption.Default,
                contentType = SearchContentType.All,
                timeRange = SearchTimeRange.All,
                restrictedMemberHashId = "",
                nextUrl = null,
            )
            assertEquals(1, page.items.size)
            assertEquals("Search result for Android", page.items.first().title)
            assertTrue(page.isEnd)
        }
    }

    @Test
    fun testTopicFollowToggle() {
        val repo = FakeSearchRepository()
        runBlocking {
            assertFalse(repo.followingTopicIds.contains("123"))
            repo.setTopicFollowing("123", true)
            assertTrue(repo.followingTopicIds.contains("123"))
            repo.setTopicFollowing("123", false)
            assertFalse(repo.followingTopicIds.contains("123"))
        }
    }

    @Test
    fun testMemberFollowToggle() {
        val repo = FakeSearchRepository()
        runBlocking {
            assertFalse(repo.followingMemberTokens.contains("user-token"))
            repo.setMemberFollowing("user-token", true)
            assertTrue(repo.followingMemberTokens.contains("user-token"))
            repo.setMemberFollowing("user-token", false)
            assertFalse(repo.followingMemberTokens.contains("user-token"))
        }
    }

    @Test
    fun testHotSearchAndHistorySettings() {
        val repo = FakeSearchRepository()
        assertTrue(repo.isHotSearchEnabled())
        assertTrue(repo.isSearchHistoryEnabled())
        runBlocking {
            val hotSearches = repo.fetchHotSearches()
            assertEquals(2, hotSearches.size)
            assertEquals("Hot 1", hotSearches[0].query)
        }
    }
}

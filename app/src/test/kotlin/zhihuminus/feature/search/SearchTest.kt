package com.zhihuminus.feature.search

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.util.HttpStatusException
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.api.ZhihuSearchApi
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.SearchItemDto
import com.zhihuminus.data.zhihu.mapper.toPeopleMemberItem
import com.zhihuminus.feature.people.PeopleMemberItem
import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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
        ): SearchPage<PeopleSearchResult> = SearchPage(
            items = listOf(
                PeopleSearchResult(
                    people = PeopleMemberItem(
                        id = "user1",
                        urlToken = "token1",
                        name = "User One",
                        headline = "Android dev",
                        answerCount = 10,
                        articleCount = 5,
                        followerCount = 100,
                        isFollowing = false,
                    ),
                    highlightedName = "<em>User</em> One",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

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

    @Test
    fun testSearchPeoplePage() {
        val repo = FakeSearchRepository()
        runBlocking {
            val page = repo.searchPeople(
                query = "User",
                sort = SearchSortOption.Default,
                timeRange = SearchTimeRange.All,
                restrictedMemberHashId = "",
                nextUrl = null,
            )
            assertEquals(1, page.items.size)
            val result = page.items.first()
            assertEquals("user1", result.people.id)
            assertEquals("User One", result.people.name)
            assertEquals("<em>User</em> One", result.highlightedName)
            assertEquals(10, result.people.answerCount)
            assertEquals(5, result.people.articleCount)
            assertEquals(100, result.people.followerCount)
            assertFalse(result.people.isFollowing)
            assertTrue(page.isEnd)
        }
    }

    @Test
    fun testSearchResultPeopleDecoding() {
        val json =
            """
            {
                "type": "search_result",
                "id": "123",
                "object": {
                    "type": "people",
                    "id": "user123",
                    "url_token": "zhang-san",
                    "name": "<em>张三</em>",
                    "avatar_url": "https://pic.zhihu.com/avatar.jpg",
                    "headline": "全栈工程师",
                    "badge_v2": {
                        "title": "优秀答主",
                        "icon": "https://pic.zhihu.com/badge.jpg",
                        "detail_badges": [
                            {
                                "title": "优秀答主",
                                "type": "best_answerer",
                                "icon": "https://pic.zhihu.com/badge.jpg"
                            }
                        ]
                    },
                    "answer_count": 42,
                    "articles_count": 10,
                    "follower_count": 999,
                    "is_following": true
                }
            }
            """.trimIndent()

        val parsedElement = ZhihuJson.json.parseToJsonElement(json)
        val searchItem = ZhihuJson.decodeJson<SearchItemDto>(parsedElement)
        assertEquals("search_result", searchItem.type)
        assertEquals("123", searchItem.id)

        val objectJson = searchItem.obj?.jsonObject
        assertNotNull(objectJson)
        assertEquals("people", objectJson["type"]?.jsonPrimitive?.content)

        val memberDto = ZhihuJson.decodeJson<MemberItemDto>(objectJson)
        val cleanName = memberDto.name.replace("<em>", "").replace("</em>", "")
        val peopleResult = PeopleSearchResult(
            people = memberDto.toPeopleMemberItem().copy(name = cleanName),
            highlightedName = memberDto.name,
        )
        assertEquals("<em>张三</em>", peopleResult.highlightedName)

        val member = peopleResult.people
        assertEquals("user123", member.id)
        assertEquals("zhang-san", member.urlToken)
        assertEquals("张三", member.name)
        assertEquals("https://pic.zhihu.com/avatar.jpg", member.avatarUrl)
        assertEquals("全栈工程师", member.headline)
        assertEquals(42, member.answerCount)
        assertEquals(10, member.articleCount)
        assertEquals(999, member.followerCount)
        assertTrue(member.isFollowing)
        assertEquals("优秀答主", member.officialBadge?.title)
    }

    @Test
    fun testHotSearchApiParsesResponseSuccessfully() = runBlocking {
        val jsonPayload =
            """
            {
                "hot_search_queries": [
                    {
                        "query": "国庆节",
                        "hot_show": "500 万",
                        "label": "热"
                    },
                    {
                        "query": "诺贝尔奖",
                        "hot_show": "300 万",
                        "label": ""
                    }
                ]
            }
            """.trimIndent()
        val env = object : ZhihuApiEnvironment {
            override fun httpClient(): HttpClient = error("stub")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

            override suspend fun fetchJson(url: String, include: String): JsonObject = ZhihuJson.json.parseToJsonElement(jsonPayload).jsonObject
        }
        val api = ZhihuSearchApi(env)
        val items = api.getHotSearches()
        assertEquals(2, items.size)
        assertEquals("国庆节", items[0].query)
        assertEquals("500 万", items[0].hotShow)
        assertEquals("热", items[0].label)
        assertEquals("诺贝尔奖", items[1].query)
    }

    @Test
    fun testHotSearchApiHandles404Gracefully() = runBlocking {
        val env = object : ZhihuApiEnvironment {
            override fun httpClient(): HttpClient = error("stub")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

            override suspend fun fetchJson(url: String, include: String): JsonObject? = throw HttpStatusException(HttpStatusCode.NotFound, Url(url), "404 page not found")
        }
        val api = ZhihuSearchApi(env)
        val items = api.getHotSearches()
        assertTrue(items.isEmpty())
    }
}

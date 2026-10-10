package com.zhihuminus.feature.follow

import com.zhihuminus.data.FakeZhihuApiEnvironment
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.dto.FollowingUserActorDto
import com.zhihuminus.data.zhihu.dto.FollowingUserItemDto
import com.zhihuminus.data.zhihu.repository.ZhihuFollowRepository
import com.zhihuminus.navigation.Person
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowTest {
    private class FakeFollowRepository : FollowRepository {
        var fetchFollowFeedCalledCount = 0
        var fetchRecentFollowingUsersCalledCount = 0

        override suspend fun fetchFollowFeed(nextUrl: String?): FollowFeedPage {
            fetchFollowFeedCalledCount++
            return if (nextUrl == null) {
                FollowFeedPage(
                    items = listOf(
                        FeedDisplayItem(title = "Feed 1", summary = "Summary 1", details = "Details 1"),
                        FeedDisplayItem(title = "Feed 2", summary = "Summary 2", details = "Details 2"),
                    ),
                    nextUrl = "https://www.zhihu.com/api/v3/moments?page=2",
                    isEnd = false,
                )
            } else {
                FollowFeedPage(
                    items = listOf(
                        FeedDisplayItem(title = "Feed 2", summary = "Summary 2", details = "Details 2"), // 重复条目
                        FeedDisplayItem(title = "Feed 3", summary = "Summary 3", details = "Details 3"),
                    ),
                    nextUrl = null,
                    isEnd = true,
                )
            }
        }

        override suspend fun fetchRecentFollowingUsers(): List<FollowingUser> {
            fetchRecentFollowingUsersCalledCount++
            return listOf(
                FollowingUser(
                    id = "u1",
                    urlToken = "user-1",
                    name = "User One",
                    avatarUrl = "https://example.com/avatar1.jpg",
                    unreadCount = 2,
                ),
            )
        }
    }

    @Test
    fun testRepositoryFeedAndUserFetching() = runBlocking {
        val repository = FakeFollowRepository()

        val users = repository.fetchRecentFollowingUsers()
        assertEquals(1, repository.fetchRecentFollowingUsersCalledCount)
        assertEquals(1, users.size)
        assertEquals("User One", users.first().name)
        assertEquals(2, users.first().unreadCount)

        val page1 = repository.fetchFollowFeed(null)
        assertEquals(1, repository.fetchFollowFeedCalledCount)
        assertEquals(2, page1.items.size)
        assertFalse(page1.isEnd)

        val page2 = repository.fetchFollowFeed(page1.nextUrl)
        assertEquals(2, repository.fetchFollowFeedCalledCount)
        assertTrue(page2.isEnd)
    }

    @Test
    fun testFeedDeduplication() = runBlocking {
        val repository = FakeFollowRepository()
        val page1 = repository.fetchFollowFeed(null)
        val page2 = repository.fetchFollowFeed(page1.nextUrl)

        val existingKeys = page1.items.map { it.stableKey }.toSet()
        val combined = page1.items + page2.items.filterNot { it.stableKey in existingKeys }

        assertEquals(3, combined.size)
        assertEquals(listOf("Feed 1", "Feed 2", "Feed 3"), combined.map { it.title })
    }

    @Test
    fun testUserClickEventTarget() {
        val user = FollowingUser(
            id = "u123",
            urlToken = "test-user",
            name = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
        )
        val event = FollowEvent.UserClick(user)
        val target = Person(
            id = event.user.id,
            urlToken = event.user.urlToken,
            name = event.user.name,
            jumpTo = "动态",
        )

        assertEquals("u123", target.id)
        assertEquals("test-user", target.urlToken)
        assertEquals("Test User", target.name)
        assertEquals("动态", target.jumpTo)
    }

    @Test
    fun testZhihuFollowRepositoryRecentUsersMapping() = runBlocking {
        val fakeApi = object : ZhihuFeedApi(FakeZhihuApiEnvironment) {
            override suspend fun getRecentFollowingUsers(): List<FollowingUserItemDto> = listOf(
                FollowingUserItemDto(
                    actor = FollowingUserActorDto(
                        id = "actor-123",
                        urlToken = "test-token",
                        name = "Test Author",
                        avatarUrl = "https://example.com/author.jpg",
                    ),
                    unreadCount = 3,
                ),
            )
        }
        val repository = ZhihuFollowRepository(fakeApi)

        val users = repository.fetchRecentFollowingUsers()
        assertEquals(1, users.size)
        val user = users.first()
        assertEquals("actor-123", user.id)
        assertEquals("test-token", user.urlToken)
        assertEquals("Test Author", user.name)
        assertEquals("https://example.com/author.jpg", user.avatarUrl)
        assertEquals(3, user.unreadCount)
    }
}

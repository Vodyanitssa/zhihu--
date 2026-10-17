package com.zhihuminus.feature.follow

import com.zhihuminus.data.FakeZhihuApiEnvironment
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.CommonFeedDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowingUserActorDto
import com.zhihuminus.data.zhihu.dto.FollowingUserItemDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
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

    @Test
    fun testZhihuFollowRepositoryWithLargeOffsetFeedItem() = runBlocking {
        val question = QuestionTargetDto(
            id = 2038200491411236835L,
            _title = "阶层为什么会固化？",
            url = "https://api.zhihu.com/questions/2038200491411236835",
            type = "question",
        )
        val answer = AnswerTargetDto(
            id = 2094736201873887817L,
            url = "https://api.zhihu.com/answers/2094736201873887817",
            question = question,
            voteupCount = 70,
            commentCount = 4,
        )
        val feed = CommonFeedDto(
            id = "0_1792212679113668507_1792204029303_3",
            verb = "MEMBER_ANSWER_QUESTION",
            offset = 1792204029303L,
            actionText = "Thoughts Memo回答了问题",
            target = answer,
        )
        val fakeApi = object : ZhihuFeedApi(FakeZhihuApiEnvironment) {
            override suspend fun fetchFeedPage(url: String, include: String): FeedPage = FeedPage(
                items = listOf(feed),
                nextUrl = "https://www.zhihu.com/api/v3/moments?offset=1792204029303",
                isEnd = false,
            )
        }
        val repository = ZhihuFollowRepository(fakeApi)
        val page = repository.fetchFollowFeed(null)

        assertEquals(1, page.items.size)
        val item = page.items.first()
        assertEquals("阶层为什么会固化？", item.title)
        assertEquals("Thoughts Memo回答了问题", item.sourceLabel)
        assertEquals("回答 · 70 赞同 · 4 评论", item.details)
        assertFalse(page.isEnd)
    }
}

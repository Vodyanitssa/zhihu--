package com.zhihuminus.data.cache

import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.dto.FeedRelationshipDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.VoteUpState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PostContentCacheTest {
    private fun createDummyPost(
        id: Long = 1L,
        type: PostType = PostType.Answer,
        title: String = "Test Title",
        voteState: VoteUpState = VoteUpState.Neutral,
    ) = Post(
        id = id,
        type = type,
        title = title,
        author = Author(id = "user1", name = "User 1", avatarUrl = "https://example.com/avatar.jpg"),
        content = emptyList(),
        voteCount = 42,
        commentCount = 10,
        voteState = voteState,
    )

    @Test
    fun testPutAndGet() = runTest {
        val cache = MemoryPostContentCache()
        val post = createDummyPost(id = 123L, type = PostType.Answer)

        assertNull(cache.get(PostType.Answer, 123L))
        cache.put(post)

        val cached = cache.get(PostType.Answer, 123L)
        assertNotNull(cached)
        assertEquals(123L, cached?.id)
        assertEquals(PostType.Answer, cached?.type)
        assertEquals("Test Title", cached?.title)

        // Different type should not match
        assertNull(cache.get(PostType.Article, 123L))
    }

    @Test
    fun testTtlExpiry() = runTest {
        val cache = MemoryPostContentCache(ttlMillis = 10L)
        val post = createDummyPost(id = 1L)
        cache.put(post)

        assertNotNull(cache.get(PostType.Answer, 1L))
        Thread.sleep(15L)
        assertNull(cache.get(PostType.Answer, 1L))
    }

    @Test
    fun testLruEviction() = runTest {
        val cache = MemoryPostContentCache(maxEntries = 2)
        val post1 = createDummyPost(id = 1L)
        val post2 = createDummyPost(id = 2L)
        val post3 = createDummyPost(id = 3L)

        cache.put(post1)
        cache.put(post2)
        assertEquals(1L, cache.get(PostType.Answer, 1L)?.id)

        // Adding post3 should evict post2 (since post1 was recently accessed via get)
        cache.put(post3)
        assertNotNull(cache.get(PostType.Answer, 1L))
        assertNull(cache.get(PostType.Answer, 2L))
        assertNotNull(cache.get(PostType.Answer, 3L))
    }

    @Test
    fun testClear() = runTest {
        val cache = MemoryPostContentCache()
        cache.put(createDummyPost(id = 1L))
        cache.put(createDummyPost(id = 2L))

        assertNotNull(cache.get(PostType.Answer, 1L))
        cache.clear()
        assertNull(cache.get(PostType.Answer, 1L))
        assertNull(cache.get(PostType.Answer, 2L))
    }

    @Test
    fun testPutFromFeedAnswer() = runTest {
        val cache = MemoryPostContentCache()
        val answerTarget = AnswerTargetDto(
            id = 999L,
            content = "<p>Answer content</p>",
            question = QuestionTargetDto(id = 888L, _title = "Question Title"),
            author = FeedAuthorDto(id = "author1", name = "Author 1", avatarUrl = "https://example.com/a.jpg"),
            voteupCount = 100,
            commentCount = 20,
            relationship = FeedRelationshipDto(voting = 1),
        )

        cache.putFromFeed(answerTarget)

        val cached = cache.get(PostType.Answer, 999L)
        assertNotNull(cached)
        assertEquals(999L, cached?.id)
        assertEquals(PostType.Answer, cached?.type)
        assertEquals("Question Title", cached?.title)
        assertEquals(VoteUpState.Up, cached?.voteState)
        assertEquals(888L, cached?.questionId)
        assertEquals("Author 1", cached?.author?.name)
    }

    @Test
    fun testPutFromFeedArticle() = runTest {
        val cache = MemoryPostContentCache()
        val articleTarget = ArticleTargetDto(
            id = 777L,
            title = "Article Title",
            content = "<p>Article content</p>",
            author = FeedAuthorDto(id = "author2", name = "Author 2", avatarUrl = "https://example.com/b.jpg"),
            voteupCount = 50,
            commentCount = 5,
            voting = -1,
            imageUrl = "https://example.com/cover.jpg",
        )

        cache.putFromFeed(articleTarget)

        val cached = cache.get(PostType.Article, 777L)
        assertNotNull(cached)
        assertEquals(777L, cached?.id)
        assertEquals(PostType.Article, cached?.type)
        assertEquals("Article Title", cached?.title)
        assertEquals(VoteUpState.Down, cached?.voteState)
        assertEquals("https://example.com/cover.jpg", cached?.coverUrl)
    }

    @Test
    fun testPutFromFeedBlankContentIgnored() = runTest {
        val cache = MemoryPostContentCache()
        val blankAnswer = AnswerTargetDto(
            id = 111L,
            content = "",
            question = QuestionTargetDto(id = 222L, _title = "Q"),
        )
        cache.putFromFeed(blankAnswer)
        assertNull(cache.get(PostType.Answer, 111L))

        val questionTarget = QuestionTargetDto(id = 333L, _title = "Question Only")
        cache.putFromFeed(questionTarget)
        assertNull(cache.get(PostType.Answer, 333L))
    }

    @Test
    fun testPutFromFeedPin() = runTest {
        val cache = MemoryPostContentCache()
        val pinTarget = PinTargetDto(
            id = 555L,
            contentHtml = "<div><p>Pin body text</p></div>",
            author = FeedAuthorDto(id = "pinAuthor", name = "Pin Author", avatarUrl = "https://example.com/p.jpg", isFollowing = true),
            likeCount = 15,
            commentCount = 3,
            content = listOf(
                PinContentItemDto(
                    type = "link_card",
                    url = "https://example.com/link",
                    dataContentId = "card1",
                    dataContentType = "link",
                ),
            ),
        )

        cache.putFromFeed(pinTarget)

        val cached = cache.get(PostType.Pin, 555L)
        assertNotNull(cached)
        assertEquals(555L, cached?.id)
        assertEquals(PostType.Pin, cached?.type)
        assertEquals(true, cached?.author?.isFollowing)
        assertEquals("Pin Author", cached?.author?.name)
        assertEquals(1, cached?.linkCards?.size)
        assertEquals("https://example.com/link", cached?.linkCards?.first()?.url)
    }

    @Test
    fun testPutFromFeedPreservesIpInfo() = runTest {
        val cache = MemoryPostContentCache()
        val answerWithIp = AnswerTargetDto(
            id = 1001L,
            content = "<p>Answer content</p>",
            question = QuestionTargetDto(id = 2001L, _title = "Q"),
            author = FeedAuthorDto(id = "a1", name = "Author", avatarUrl = "", ipInfo = "IP 属地北京"),
        )
        cache.putFromFeed(answerWithIp)
        val cached = cache.get(PostType.Answer, 1001L)
        assertNotNull(cached)
        assertEquals("IP 属地北京", cached?.ipInfo)
        assertEquals("IP 属地北京", cached?.author?.ipInfo)
    }

    @Test
    fun testUpdateAuthorFollowing() = runTest {
        val cache = MemoryPostContentCache()
        val post1 = Post(
            id = 101L,
            type = PostType.Answer,
            title = "A1",
            author = Author(id = "user1", name = "User 1", avatarUrl = "", urlToken = "token_user_1", isFollowing = false),
            content = emptyList(),
            voteCount = 0,
            commentCount = 0,
        )
        val post2 = Post(
            id = 102L,
            type = PostType.Article,
            title = "A2",
            author = Author(id = "user1", name = "User 1", avatarUrl = "", urlToken = "token_user_1", isFollowing = false),
            content = emptyList(),
            voteCount = 0,
            commentCount = 0,
        )
        val post3 = Post(
            id = 103L,
            type = PostType.Answer,
            title = "A3",
            author = Author(id = "user2", name = "User 2", avatarUrl = "", urlToken = "token_user_2", isFollowing = false),
            content = emptyList(),
            voteCount = 0,
            commentCount = 0,
        )
        cache.put(post1)
        cache.put(post2)
        cache.put(post3)

        // Update following for user1 by urlToken
        cache.updateAuthorFollowing("token_user_1", true)
        assertEquals(true, cache.get(PostType.Answer, 101L)?.author?.isFollowing)
        assertEquals(true, cache.get(PostType.Article, 102L)?.author?.isFollowing)
        assertEquals(false, cache.get(PostType.Answer, 103L)?.author?.isFollowing)

        // Update following for user1 by id to false
        cache.updateAuthorFollowing("user1", false)
        assertEquals(false, cache.get(PostType.Answer, 101L)?.author?.isFollowing)
        assertEquals(false, cache.get(PostType.Article, 102L)?.author?.isFollowing)
    }
}

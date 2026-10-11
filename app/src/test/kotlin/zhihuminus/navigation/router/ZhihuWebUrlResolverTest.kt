package com.zhihuminus.navigation.router

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Column
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.Topic
import com.zhihuminus.navigation.Video
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuWebUrlResolverTest {
    @Test
    fun resolvesQuestionAndAnswer() {
        assertEquals(
            Question(questionId = 12345L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/question/12345"),
        )
        assertEquals(
            PostDestination(type = PostType.Answer, id = 67890L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/question/12345/answer/67890"),
        )
        assertEquals(
            PostDestination(type = PostType.Answer, id = 67890L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/answer/67890"),
        )
    }

    @Test
    fun resolvesArticleAndColumn() {
        assertEquals(
            PostDestination(type = PostType.Article, id = 111L),
            ZhihuWebUrlResolver.resolve("https://zhuanlan.zhihu.com/p/111"),
        )
        assertEquals(
            PostDestination(type = PostType.Article, id = 222L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/oia/articles/222"),
        )
        assertEquals(
            Column(columnId = "c_123"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/column/c_123"),
        )
    }

    @Test
    fun resolvesPinAndVideo() {
        assertEquals(
            PostDestination(type = PostType.Pin, id = 333L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/pin/333"),
        )
        assertEquals(
            Video(id = 444L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/video/444"),
        )
    }

    @Test
    fun resolvesAppview() {
        assertEquals(
            PostDestination(type = PostType.Pin, id = 1L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/appview/pin/1"),
        )
        assertEquals(
            PostDestination(type = PostType.Answer, id = 2L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/appview/answer/2"),
        )
        assertEquals(
            PostDestination(type = PostType.Article, id = 3L),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/appview/p/3"),
        )
    }

    @Test
    fun resolvesPeople() {
        assertEquals(
            Person(id = "0123456789abcdef0123456789abcdef", urlToken = "0123456789abcdef0123456789abcdef"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/people/0123456789abcdef0123456789abcdef"),
        )
        assertEquals(
            Person(id = Person.EMPTY_ID, urlToken = "some-user"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/people/some-user"),
        )
    }

    @Test
    fun resolvesCollections() {
        assertEquals(
            CollectionContent(collectionId = "9527"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/collection/9527"),
        )
        assertEquals(
            CollectionContent(collectionId = "9527"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/collections/9527"),
        )
        assertEquals(
            CollectionContent(collectionId = "9527"),
            ZhihuWebUrlResolver.resolve("https://api.zhihu.com/collections/9527"),
        )
    }

    @Test
    fun resolvesTopicAndSearch() {
        assertEquals(
            Topic(id = "1954", section = "talk"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/topic/1954/talk"),
        )
        assertEquals(
            Search("compose"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/search?q=compose"),
        )
    }

    @Test
    fun resolvesNotifications() {
        assertEquals(
            Notification.Invitations,
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/compose_answer_tab"),
        )
        assertEquals(
            Notification.Message(peerId = "p1", name = "张三"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/inbox/p1?title=张三"),
        )
        assertEquals(
            Notification.Entry(entryName = "follow", title = "关注"),
            ZhihuWebUrlResolver.resolve("https://www.zhihu.com/notifications/v3/timeline/entry/follow?title=关注"),
        )
    }

    @Test
    fun resolvesLinkRedirect() {
        assertEquals(
            Question(questionId = 888L),
            ZhihuWebUrlResolver.resolve("https://link.zhihu.com/?target=https%3A%2F%2Fwww.zhihu.com%2Fquestion%2F888"),
        )
    }

    @Test
    fun resolvesRelativeUrls() {
        assertEquals(
            Question(questionId = 123L),
            ZhihuWebUrlResolver.resolve("//www.zhihu.com/question/123"),
        )
        assertEquals(
            Question(questionId = 123L),
            ZhihuWebUrlResolver.resolve("/question/123"),
        )
    }

    @Test
    fun safelyRejectsMalformedUrlsWithoutCrashing() {
        assertNull(ZhihuWebUrlResolver.resolve("https://www.zhihu.com/question/not-a-number"))
        assertNull(ZhihuWebUrlResolver.resolve("https://www.zhihu.com/question/123/answer/not-a-number"))
        assertNull(ZhihuWebUrlResolver.resolve("https://www.zhihu.com/video/not-a-number"))
        assertNull(ZhihuWebUrlResolver.resolve("https://zhuanlan.zhihu.com/p/not-a-number"))
        assertNull(ZhihuWebUrlResolver.resolve("https://other-domain.com/question/123"))
        assertNull(ZhihuWebUrlResolver.resolve(":::not-a-url:::"))
    }
}

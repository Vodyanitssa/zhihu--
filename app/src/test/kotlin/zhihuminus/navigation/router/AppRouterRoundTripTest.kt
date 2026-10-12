package com.zhihuminus.navigation.router

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.CommentHolder
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.Topic
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.WatchLater
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppRouterRoundTripTest {
    private fun assertRoundTrip(destination: NavDestination, expectedUrl: String? = destination.toAppUrl()) {
        val url = requireNotNull(destination.toAppUrl()) { "无法编码: $destination" }
        if (expectedUrl != null) assertEquals(expectedUrl, url)
        when (val resolution = AppRouter.resolve(url)) {
            is RouteResolution.Screen -> assertEquals(destination, resolution.destination)
            is RouteResolution.Tab -> error("期望 Screen，实际 Tab: $url")
            null -> error("无法解析: $url")
        }
    }

    private fun resolveScreen(url: String): NavDestination? =
        (AppRouter.resolve(url) as? RouteResolution.Screen)?.destination

    @Test
    fun question() {
        assertRoundTrip(Question(questionId = 123456L), "zhihu://questions/123456")
        assertEquals(RouteResolution.Screen(Question(123456L)), AppRouter.resolve("zhihu://question/123456"))
    }

    @Test
    fun answerPost() {
        assertRoundTrip(PostDestination(type = PostType.Answer, id = 1L), "zhihu://answers/1")
        assertEquals(
            RouteResolution.Screen(PostDestination(type = PostType.Answer, id = 1L)),
            AppRouter.resolve("zhihu://answer/1"),
        )
    }

    @Test
    fun articlePost() {
        assertRoundTrip(PostDestination(type = PostType.Article, id = 2L), "zhihu://articles/2")
        assertEquals(
            RouteResolution.Screen(PostDestination(type = PostType.Article, id = 2L)),
            AppRouter.resolve("zhihu://article/2"),
        )
    }

    @Test
    fun pinPost() = assertRoundTrip(PostDestination(type = PostType.Pin, id = 3L), "zhihu://pin/3")

    @Test
    fun video() = assertRoundTrip(Video(id = 4L), "zhihu://video/4")

    @Test
    fun topicWithSection() = assertRoundTrip(Topic(id = "1954", section = "talk"), "zhihu://topic/1954/talk")

    @Test
    fun topicWithoutSection() {
        assertRoundTrip(Topic(id = "1954"), "zhihu://topic/1954")
        assertEquals(RouteResolution.Screen(Topic("1954")), AppRouter.resolve("zhihu://topics/1954"))
        assertEquals(RouteResolution.Screen(Topic("1954")), AppRouter.resolve("zhihu://pin20?topic_id=1954"))
    }

    @Test
    fun personWithReadableToken() =
        assertRoundTrip(Person(id = Person.EMPTY_ID, urlToken = "exc-fulfillment"), "zhihu://people/exc-fulfillment")

    @Test
    fun personWithHexId() =
        assertRoundTrip(
            Person(id = "0123456789abcdef0123456789abcdef", urlToken = "0123456789abcdef0123456789abcdef"),
            "zhihu://people/0123456789abcdef0123456789abcdef",
        )

    @Test
    fun collections() = assertRoundTrip(Collections(userToken = "some-token"), "zhihu://collections/some-token")

    @Test
    fun collectionContent() = assertRoundTrip(CollectionContent(collectionId = "9527"), "zhihu://collection/9527")

    @Test
    fun searchWithQueryOnly() {
        val url = Search(query = "kotlin").toAppUrl()
        assertEquals("zhihu://search?q=kotlin", url)
        assertEquals(
            RouteResolution.Screen(Search(query = "kotlin")),
            AppRouter.resolve(requireNotNull(url)),
        )
    }

    @Test
    fun searchEncodesUnicodeQuery() {
        val search = Search(query = "知乎")
        val resolved = AppRouter.resolve(requireNotNull(search.toAppUrl()))
        assertEquals(RouteResolution.Screen(search), resolved)
    }

    @Test
    fun searchRestrictedToMember() =
        assertRoundTrip(Search(query = "", restrictedMemberHashId = "abc123"), "zhihu://search?member=abc123")

    @Test
    fun emptySearchHasNoQuery() {
        assertEquals("zhihu://search", Search().toAppUrl())
        assertEquals(RouteResolution.Screen(Search()), AppRouter.resolve("zhihu://search"))
    }

    @Test
    fun commentOnAnswer() {
        assertRoundTrip(
            CommentHolder(commentId = "42", article = PostDestination(type = PostType.Answer, id = 7L)),
            "zhihu://comment/list/answer/7?anchor_comment_id=42",
        )
        // 兼容单数与简写参数
        assertEquals(
            RouteResolution.Screen(
                CommentHolder(commentId = "42", article = PostDestination(type = PostType.Answer, id = 7L)),
            ),
            AppRouter.resolve("zhihu://comment/answer/7?anchor=42"),
        )
    }

    @Test
    fun commentOnQuestion() =
        assertRoundTrip(
            CommentHolder(commentId = "42", article = Question(questionId = 8L)),
            "zhihu://comment/list/question/8?anchor_comment_id=42",
        )

    @Test
    fun commentWithoutAnchorIsRejected() =
        assertNull(resolveScreen("zhihu://comment/answer/7"))

    @Test
    fun feedDeeplinkResolvesToHomeTab() {
        assertEquals(RouteResolution.Tab(Home), AppRouter.resolve("zhihu://feed"))
        assertEquals("zhihu://feed", MainTabs.toAppUrl())
    }

    @Test
    fun tabs() {
        mapOf(
            Home to "zhihu://tab/home",
            Follow to "zhihu://tab/follow",
            Daily to "zhihu://tab/daily",
        ).forEach { destination, url ->
            assertEquals(RouteResolution.Tab(destination), AppRouter.resolve(url), url)
        }
    }

    @Test
    fun accountLegacyTabUrlResolvesToScreen() = assertRoundTrip(Account, "zhihu://tab/account")

    @Test
    fun historyRoot() = assertRoundTrip(History, "zhihu://history")

    @Test
    fun historyLegacyTabUrlResolvesToScreen() {
        assertEquals(RouteResolution.Screen(History), AppRouter.resolve("zhihu://tab/history"))
    }

    @Test
    fun notificationRoot() = assertRoundTrip(Notification, "zhihu://notification")

    @Test
    fun notificationEntry() =
        assertRoundTrip(Notification.Entry(entryName = "follow", title = ""), "zhihu://notification/entry/follow")

    @Test
    fun notificationMessage() =
        assertRoundTrip(Notification.Message(peerId = "p1"), "zhihu://notification/message/p1")

    @Test
    fun notificationInvitations() =
        assertRoundTrip(Notification.Invitations, "zhihu://notification/invitations")

    @Test
    fun settingsPages() {
        assertRoundTrip(Account.AppearanceSettings(setting = "dark"), "zhihu://settings/appearance?setting=dark")
        assertRoundTrip(Account.AppearanceSettings(), "zhihu://settings/appearance")
        assertRoundTrip(Account.NotificationSettings(setting = "s"), "zhihu://settings/notification?setting=s")
        assertRoundTrip(Account.NotificationSettings(), "zhihu://settings/notification")
        assertRoundTrip(Account.SettingsSearch, "zhihu://settings/search")
        assertRoundTrip(Account.OpenSourceLicenses, "zhihu://settings/licenses")
        // 验证阅读与系统旧路径平滑映射到 appearance
        assertEquals(
            RouteResolution.Screen(Account.AppearanceSettings("test")),
            AppRouter.resolve("zhihu://settings/system?setting=test"),
        )
        assertEquals(
            RouteResolution.Screen(Account.AppearanceSettings("")),
            AppRouter.resolve("zhihu://settings/reading"),
        )
    }

    @Test
    fun resolveAnyHandlesBothSchemeAndWeb() {
        assertEquals(
            RouteResolution.Screen(Question(questionId = 123L)),
            AppRouter.resolveAny("zhihu://questions/123"),
        )
        assertEquals(
            RouteResolution.Screen(Question(questionId = 123L)),
            AppRouter.resolveAny("https://www.zhihu.com/question/123"),
        )
        assertEquals(
            RouteResolution.Screen(Question(questionId = 123L)),
            AppRouter.resolveAny("//www.zhihu.com/question/123"),
        )
        assertEquals(
            RouteResolution.Tab(Home),
            AppRouter.resolveAny("zhihu://feed"),
        )
    }

    @Test
    fun foreignSchemeIsIgnored() {
        assertNull(AppRouter.resolve("https://www.zhihu.com/question/1"))
        assertNull(AppRouter.resolve("ftp://answers/1"))
    }

    @Test
    fun malformedUrlsReturnNull() {
        assertNull(resolveScreen("zhihu://question/not-a-number"))
        assertNull(resolveScreen("zhihu://unknown-host/segment"))
        assertNull(resolveScreen("zhihu://question/1/extra"))
        assertNull(resolveScreen("zhihu://write/video/1"))
        assertNull(AppRouter.resolve("::::not a url:::"))
    }

    @Test
    fun hostMatchingIsCaseInsensitive() {
        assertEquals(
            RouteResolution.Screen(Question(questionId = 1L)),
            AppRouter.resolve("ZHIHU://Questions/1"),
        )
    }

    @Test
    fun encodedUrlIsParseableByKtorAgain() {
        assertTrue(Question(questionId = 1L).toAppUrl().let { it == "zhihu://questions/1" && AppRouter.resolve(it) != null })
    }

    @Test
    fun watchLater() {
        assertEquals("zhihu://tab/watch_later", WatchLater.toAppUrl())
        assertEquals(RouteResolution.Tab(WatchLater), AppRouter.resolve("zhihu://tab/watch_later"))
        assertEquals(RouteResolution.Tab(WatchLater), AppRouter.resolve("zhihu://tab/watchlater"))
    }
}

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
import com.zhihuminus.navigation.HotList
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.MyCollections
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.OnlineHistory
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.Topic
import com.zhihuminus.navigation.Video
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
    fun question() = assertRoundTrip(Question(questionId = 123456L), "zhminus://question/123456")

    @Test
    fun answerPost() = assertRoundTrip(PostDestination(type = PostType.Answer, id = 1L), "zhminus://answer/1")

    @Test
    fun articlePost() = assertRoundTrip(PostDestination(type = PostType.Article, id = 2L), "zhminus://article/2")

    @Test
    fun pinPost() = assertRoundTrip(PostDestination(type = PostType.Pin, id = 3L), "zhminus://pin/3")

    @Test
    fun video() = assertRoundTrip(Video(id = 4L), "zhminus://video/4")

    @Test
    fun topicWithSection() = assertRoundTrip(Topic(id = "1954", section = "talk"), "zhminus://topic/1954/talk")

    @Test
    fun topicWithoutSection() = assertRoundTrip(Topic(id = "1954"), "zhminus://topic/1954")

    @Test
    fun personWithReadableToken() =
        assertRoundTrip(Person(id = Person.EMPTY_ID, urlToken = "exc-fulfillment"), "zhminus://people/exc-fulfillment")

    @Test
    fun personWithHexId() =
        assertRoundTrip(
            Person(id = "0123456789abcdef0123456789abcdef", urlToken = "0123456789abcdef0123456789abcdef"),
            "zhminus://people/0123456789abcdef0123456789abcdef",
        )

    @Test
    fun collections() = assertRoundTrip(Collections(userToken = "some-token"), "zhminus://collections/some-token")

    @Test
    fun collectionContent() = assertRoundTrip(CollectionContent(collectionId = "9527"), "zhminus://collection/9527")

    @Test
    fun searchWithQueryOnly() {
        val url = Search(query = "kotlin").toAppUrl()
        assertEquals("zhminus://search?q=kotlin", url)
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
        assertRoundTrip(Search(query = "", restrictedMemberHashId = "abc123"), "zhminus://search?member=abc123")

    @Test
    fun emptySearchHasNoQuery() {
        assertEquals("zhminus://search", Search().toAppUrl())
        assertEquals(RouteResolution.Screen(Search()), AppRouter.resolve("zhminus://search"))
    }

    @Test
    fun commentOnAnswer() =
        assertRoundTrip(
            CommentHolder(commentId = "42", article = PostDestination(type = PostType.Answer, id = 7L)),
            "zhminus://comment/answer/7?anchor=42",
        )

    @Test
    fun commentOnQuestion() =
        assertRoundTrip(
            CommentHolder(commentId = "42", article = Question(questionId = 8L)),
            "zhminus://comment/question/8?anchor=42",
        )

    @Test
    fun commentWithoutAnchorIsRejected() =
        assertNull(resolveScreen("zhminus://comment/answer/7"))

    @Test
    fun tabs() {
        mapOf(
            Home to "zhminus://tab/home",
            Follow to "zhminus://tab/follow",
            HotList to "zhminus://tab/hot",
            Daily to "zhminus://tab/daily",
            OnlineHistory to "zhminus://tab/history",
            MyCollections to "zhminus://tab/collections",
            Account to "zhminus://tab/account",
        ).forEach { destination, url ->
            assertEquals(RouteResolution.Tab(destination), AppRouter.resolve(url), url)
        }
    }

    @Test
    fun mainTabsEncodesToHomeTab() {
        assertEquals("zhminus://tab/home", MainTabs.toAppUrl())
    }

    @Test
    fun legacyHistoryIsNotEncodable() {
        // History 同时实现两个接口，显式按 NavDestination 调用避免重载歧义。
        assertNull((History as NavDestination).toAppUrl())
    }

    @Test
    fun notificationRoot() = assertRoundTrip(Notification, "zhminus://notification")

    @Test
    fun notificationEntry() =
        // 富字段（title）不参与 URL 编码，往返后保持默认空值。
        assertRoundTrip(Notification.Entry(entryName = "follow", title = ""), "zhminus://notification/entry/follow")

    @Test
    fun notificationMessage() =
        // 富字段（name、avatarUrl）不参与 URL 编码，往返后保持默认空值。
        assertRoundTrip(Notification.Message(peerId = "p1"), "zhminus://notification/message/p1")

    @Test
    fun notificationInvitations() =
        assertRoundTrip(Notification.Invitations, "zhminus://notification/invitations")

    @Test
    fun notificationSettings() =
        assertRoundTrip(
            Notification.NotificationSettings(setting = "key"),
            "zhminus://notification/settings?setting=key",
        )

    @Test
    fun settingsPages() {
        assertRoundTrip(Account.AppearanceSettings(setting = "dark"), "zhminus://settings/appearance?setting=dark")
        assertRoundTrip(Account.AppearanceSettings(), "zhminus://settings/appearance")
        assertRoundTrip(Account.ReadingSettings, "zhminus://settings/reading")
        assertRoundTrip(Account.IdentityManagement, "zhminus://settings/identity")
        assertRoundTrip(Account.SystemAndUpdateSettings(setting = "s"), "zhminus://settings/system?setting=s")
        assertRoundTrip(Account.SettingsSearch, "zhminus://settings/search")
        assertRoundTrip(Account.OpenSourceLicenses, "zhminus://settings/licenses")
    }

    @Test
    fun foreignSchemeIsIgnored() {
        assertNull(AppRouter.resolve("https://www.zhihu.com/question/1"))
        assertNull(AppRouter.resolve("zhihu://answers/1"))
    }

    @Test
    fun malformedUrlsReturnNull() {
        assertNull(resolveScreen("zhminus://question/not-a-number"))
        assertNull(resolveScreen("zhminus://unknown-host/segment"))
        assertNull(resolveScreen("zhminus://question/1/extra"))
        assertNull(resolveScreen("zhminus://write/video/1"))
        assertNull(AppRouter.resolve("::::not a url:::"))
    }

    @Test
    fun hostMatchingIsCaseInsensitive() {
        assertEquals(
            RouteResolution.Screen(Question(questionId = 1L)),
            AppRouter.resolve("ZHMINUS://Question/1"),
        )
    }

    @Test
    fun encodedUrlIsParseableByKtorAgain() {
        // 编码结果必须能被 ktor 二次无损解析（无多余端口、路径斜杠等）。
        assertTrue(Question(questionId = 1L).toAppUrl().let { it == "zhminus://question/1" && AppRouter.resolve(it) != null })
    }
}

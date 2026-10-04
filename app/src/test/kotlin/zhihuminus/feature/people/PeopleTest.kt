package com.zhihuminus.feature.people

import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.navDestination
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PeopleTest {
    private class FakePeopleRepository : PeopleRepository {
        var isFollowed = false
        var isBlocked = false

        override suspend fun getProfile(userTokenOrId: String): PeopleProfile = PeopleProfile(
            id = "test-hash-id",
            urlToken = userTokenOrId,
            name = "Test User",
            avatarUrl = "https://picx.zhimg.com/test_xl.jpg",
            headline = "Android Developer",
            followerCount = 100,
            followingCount = 50,
            answerCount = 20,
            articleCount = 10,
            isFollowing = isFollowed,
            isBlocking = isBlocked,
        )

        override suspend fun follow(urlToken: String): Int? {
            isFollowed = true
            return 101
        }

        override suspend fun unfollow(urlToken: String): Int? {
            isFollowed = false
            return 100
        }

        override suspend fun block(urlToken: String) {
            isBlocked = true
        }

        override suspend fun unblock(urlToken: String) {
            isBlocked = false
        }

        override suspend fun getAnswers(
            userTokenOrId: String,
            sortBy: String,
            nextUrl: String?,
        ): PeoplePage<FeedDisplayItem> = PeoplePage(
            items = listOf(
                FeedDisplayItem(
                    title = "Test Answer",
                    summary = "Answer excerpt",
                    details = "details",
                ),
            ),
            nextUrl = if (nextUrl == null) "https://api.zhihu.com/next" else null,
            isEnd = nextUrl != null,
        )

        override suspend fun getArticles(
            userTokenOrId: String,
            sortBy: String,
            nextUrl: String?,
        ): PeoplePage<FeedDisplayItem> = PeoplePage(
            items = listOf(
                FeedDisplayItem(
                    title = "Test Article",
                    summary = "Article excerpt",
                    details = "details",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getActivities(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<FeedDisplayItem> = PeoplePage(
            items = listOf(
                FeedDisplayItem(
                    title = "Activity",
                    summary = "Moment excerpt",
                    details = "details",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getPins(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<FeedDisplayItem> = PeoplePage(
            items = listOf(
                FeedDisplayItem(
                    title = "",
                    summary = "Pin excerpt",
                    details = "想法 · 10 赞",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getCollections(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.Collection> = PeoplePage(
            items = listOf(
                DataHolder.Collection(id = "c1", title = "Favorite Things"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getQuestions(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.Question> = PeoplePage(
            items = emptyList(),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getColumns(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.Column> = PeoplePage(
            items = listOf(
                DataHolder.Column(id = "col1", title = "Compose Mastery"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowers(
            memberId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.People> = PeoplePage(
            items = listOf(
                ZhihuJson.decodeFromString<DataHolder.People>(
                    """{"id": "f1", "name": "Follower 1", "avatar_url": "", "url": "", "headline": "", "gender": 0}""",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowing(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.People> = PeoplePage(
            items = listOf(
                ZhihuJson.decodeFromString<DataHolder.People>(
                    """{"id": "f2", "name": "Following 1", "avatar_url": "", "url": "", "headline": "", "gender": 0}""",
                ),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowingColumns(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.Column> = PeoplePage(
            items = listOf(
                DataHolder.Column(id = "subcol1", title = "Subscribed Column"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowingTopics(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<FollowedTopic> = PeoplePage(
            items = listOf(
                FollowedTopic(id = "t1", name = "Kotlin"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowingQuestions(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<FollowedQuestion> = PeoplePage(
            items = listOf(
                FollowedQuestion(id = "q1", title = "Followed Question"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowingCollections(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<DataHolder.Collection> = PeoplePage(
            items = listOf(
                DataHolder.Collection(id = "sc1", title = "Followed Favlist"),
            ),
            nextUrl = null,
            isEnd = true,
        )
    }

    @Test
    fun testPeopleTabTitles() {
        assertEquals(10, PeopleTab.TITLES.size)
        assertEquals("回答", PeopleTab.TITLES[0])
        assertEquals("文章", PeopleTab.TITLES[1])
        assertEquals("动态", PeopleTab.TITLES[2])
        assertEquals("收藏", PeopleTab.TITLES[3])
        assertEquals("提问", PeopleTab.TITLES[4])
        assertEquals("想法", PeopleTab.TITLES[5])
        assertEquals("专栏", PeopleTab.TITLES[6])
        assertEquals("粉丝", PeopleTab.TITLES[7])
        assertEquals("关注", PeopleTab.TITLES[8])
        assertEquals("关注订阅", PeopleTab.TITLES[9])
    }

    @Test
    fun testPeopleSubscriptionTabTitles() {
        assertEquals(4, PeopleSubscriptionTab.TITLES.size)
        assertEquals("我订阅的专栏", PeopleSubscriptionTab.TITLES[0])
        assertEquals("关注的话题", PeopleSubscriptionTab.TITLES[1])
        assertEquals("关注的问题", PeopleSubscriptionTab.TITLES[2])
        assertEquals("关注的收藏夹", PeopleSubscriptionTab.TITLES[3])
    }

    @Test
    fun testPeopleScreenInitialPage() {
        assertEquals(2, peopleScreenInitialPage(Person(id = "1", urlToken = "u1")))
        assertEquals(0, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "回答")))
        assertEquals(1, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "文章")))
        assertEquals(6, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "专栏")))
        assertEquals(2, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "nonexistent")))
    }

    @Test
    fun testColumnWebUrl() {
        val apiCol = DataHolder.Column(id = "test", url = "http://www.zhihu.com/api/v4/columns/test")
        assertEquals("https://www.zhihu.com/column/test", apiCol.webUrl())

        val webCol = DataHolder.Column(id = "test", url = "http://zhuanlan.zhihu.com/test")
        assertEquals("https://zhuanlan.zhihu.com/test", webCol.webUrl())

        val rawCol = DataHolder.Column(id = "my-column", url = "")
        assertEquals("https://www.zhihu.com/column/my-column", rawCol.webUrl())
    }

    @Test
    fun testPostConversionToFeedDisplayItem() {
        val answer = ZhihuJson.decodeFromString<DataHolder.Answer>(
            """{
                "id": 1001,
                "type": "answer",
                "answer_type": "normal",
                "url": "",
                "created_time": 1000,
                "updated_time": 1000,
                "thanks_count": 0,
                "voteup_count": 42,
                "comment_count": 5,
                "content": "content",
                "excerpt": "Sample answer excerpt",
                "can_comment": {"status": true, "reason": ""},
                "question": {"id": 2001, "title": "Question Title", "type": "question", "created": 0, "question_type": "normal", "updated_time": 0, "url": ""},
                "author": {"id": "a1", "name": "Author 1", "avatar_url": "", "headline": "", "url": "", "url_token": "", "user_type": "people", "type": "people", "gender": 0, "is_org": false, "is_advertiser": false}
            }""",
        )
        val answerItem = answer.toPeopleAnswerDisplayItem()
        assertEquals("Question Title", answerItem.title)
        assertEquals("Sample answer excerpt", answerItem.summary)
        assertEquals("回答", answerItem.contentTypeLabel)
        assertNotNull(answerItem.navDestinationJson)
        val answerDest = answerItem.navDestination as? PostDestination
        assertNotNull(answerDest)
        assertEquals(PostType.Answer, answerDest.type)
        assertEquals(1001L, answerDest.id)

        val article = ZhihuJson.decodeFromString<DataHolder.Article>(
            """{
                "id": 3001,
                "type": "article",
                "url": "",
                "title": "Article Title",
                "excerpt": "Sample article excerpt",
                "content": "content",
                "created": 1000,
                "updated": 1000,
                "voteup_count": 88,
                "comment_count": 12,
                "can_comment": {"status": true, "reason": ""},
                "author": {"id": "a2", "name": "Author 2", "avatar_url": "", "headline": "", "url": "", "url_token": "", "user_type": "people", "type": "people", "gender": 0, "is_org": false, "is_advertiser": false}
            }""",
        )
        val articleItem = article.toPeopleArticleDisplayItem()
        assertEquals("Article Title", articleItem.title)
        assertEquals("Sample article excerpt", articleItem.summary)
        assertEquals("文章", articleItem.contentTypeLabel)
        val articleDest = articleItem.navDestination as? PostDestination
        assertNotNull(articleDest)
        assertEquals(PostType.Article, articleDest.type)
        assertEquals(3001L, articleDest.id)

        val pin = ZhihuJson.decodeFromString<DataHolder.Pin>(
            """{
                "id": "4001",
                "excerpt_title": "Pin HTML &amp; Excerpt",
                "like_count": 15,
                "comment_count": 3,
                "created": 1000,
                "author": {"id": "a3", "name": "Author 3", "avatar_url": "", "headline": "", "url": "", "url_token": "", "user_type": "people", "type": "people", "gender": 0, "is_org": false, "is_advertiser": false}
            }""",
        )
        val pinItem = pin.toPeoplePinDisplayItem()
        assertEquals("Pin HTML & Excerpt", pinItem.summary)
        assertEquals("想法", pinItem.contentTypeLabel)
        val pinDest = pinItem.navDestination as? PostDestination
        assertNotNull(pinDest)
        assertEquals(PostType.Pin, pinDest.type)
        assertEquals(4001L, pinDest.id)
    }

    @Test
    fun testRepositoryProfileAndToggleOperations() = runBlocking {
        val repo = FakePeopleRepository()
        val profile = repo.getProfile("alex")
        assertEquals("alex", profile.urlToken)
        assertEquals("Test User", profile.name)
        assertFalse(profile.isFollowing)
        assertFalse(profile.isBlocking)

        val newFollowerCount = repo.follow("alex")
        assertEquals(101, newFollowerCount)
        assertTrue(repo.isFollowed)

        val unfollowCount = repo.unfollow("alex")
        assertEquals(100, unfollowCount)
        assertFalse(repo.isFollowed)

        repo.block("alex")
        assertTrue(repo.isBlocked)

        repo.unblock("alex")
        assertFalse(repo.isBlocked)
    }

    @Test
    fun testRepositoryDataStreams() = runBlocking {
        val repo = FakePeopleRepository()
        val answers = repo.getAnswers("alex", "voteups")
        assertEquals(1, answers.items.size)
        assertEquals("Test Answer", answers.items.first().title)
        assertFalse(answers.isEnd)

        val articles = repo.getArticles("alex", "created")
        assertEquals(1, articles.items.size)
        assertTrue(articles.isEnd)

        val pins = repo.getPins("alex")
        assertEquals(1, pins.items.size)
        assertEquals("Pin excerpt", pins.items.first().summary)

        val collections = repo.getCollections("alex")
        assertEquals(1, collections.items.size)
        assertEquals("Favorite Things", collections.items.first().title)

        val subTopics = repo.getFollowingTopics("alex")
        assertEquals(1, subTopics.items.size)
        assertEquals("Kotlin", subTopics.items.first().name)
    }
}

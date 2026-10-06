package com.zhihuminus.feature.people

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.OfficialBadge
import com.zhihuminus.data.zhihu.dto.MemberBadgeItemDto
import com.zhihuminus.data.zhihu.dto.MemberBadgeV2Dto
import com.zhihuminus.data.zhihu.dto.MemberSocialMediaDto
import com.zhihuminus.data.zhihu.dto.MemberSocialMediaModuleDto
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Person
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        ): PeoplePage<PeopleCreationItem> = PeoplePage(
            items = listOf(
                PeopleCreationItem(
                    id = 1001L,
                    type = PostType.Answer,
                    title = "Test Answer",
                    summary = "Answer excerpt",
                    details = "回答 · 42 赞同 · 5 评论",
                ),
            ),
            nextUrl = if (nextUrl == null) "https://api.zhihu.com/next" else null,
            isEnd = nextUrl != null,
        )

        override suspend fun getArticles(
            userTokenOrId: String,
            sortBy: String,
            nextUrl: String?,
        ): PeoplePage<PeopleCreationItem> = PeoplePage(
            items = listOf(
                PeopleCreationItem(
                    id = 3001L,
                    type = PostType.Article,
                    title = "Test Article",
                    summary = "Article excerpt",
                    details = "文章 · 88 赞同 · 12 评论",
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
        ): PeoplePage<PeopleCreationItem> = PeoplePage(
            items = listOf(
                PeopleCreationItem(
                    id = 4001L,
                    type = PostType.Pin,
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
        ): PeoplePage<Collection> = PeoplePage(
            items = listOf(
                Collection(id = "c1", title = "Favorite Things"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getQuestions(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<FollowedQuestion> = PeoplePage(
            items = emptyList(),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getColumns(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<PeopleColumnItem> = PeoplePage(
            items = listOf(
                PeopleColumnItem(id = "col1", title = "Compose Mastery"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowers(
            memberId: String,
            nextUrl: String?,
        ): PeoplePage<PeopleMemberItem> = PeoplePage(
            items = listOf(
                PeopleMemberItem(id = "f1", name = "Follower 1"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowing(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<PeopleMemberItem> = PeoplePage(
            items = listOf(
                PeopleMemberItem(id = "f2", name = "Following 1"),
            ),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun getFollowingColumns(
            userTokenOrId: String,
            nextUrl: String?,
        ): PeoplePage<PeopleColumnItem> = PeoplePage(
            items = listOf(
                PeopleColumnItem(id = "subcol1", title = "Subscribed Column"),
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
        ): PeoplePage<Collection> = PeoplePage(
            items = listOf(
                Collection(id = "sc1", title = "Followed Favlist"),
            ),
            nextUrl = null,
            isEnd = true,
        )
    }

    @Test
    fun testPeoplePrimaryTabTitles() {
        assertEquals(4, PeoplePrimaryTab.TITLES.size)
        assertEquals("创作", PeoplePrimaryTab.TITLES[0])
        assertEquals("动态", PeoplePrimaryTab.TITLES[1])
        assertEquals("收藏", PeoplePrimaryTab.TITLES[2])
        assertEquals("关注订阅", PeoplePrimaryTab.TITLES[3])
    }

    @Test
    fun testPeopleCreationTabTitles() {
        assertEquals(5, PeopleCreationTab.TITLES.size)
        assertEquals("回答", PeopleCreationTab.TITLES[0])
        assertEquals("文章", PeopleCreationTab.TITLES[1])
        assertEquals("想法", PeopleCreationTab.TITLES[2])
        assertEquals("专栏", PeopleCreationTab.TITLES[3])
        assertEquals("提问", PeopleCreationTab.TITLES[4])
    }

    @Test
    fun testPeopleUserListTypeTitles() {
        assertEquals(2, PeopleUserListType.TITLES.size)
        assertEquals("关注", PeopleUserListType.TITLES[0])
        assertEquals("粉丝", PeopleUserListType.TITLES[1])
    }

    @Test
    fun testResolvePeopleInitialSelection() {
        val defaultSelection = resolvePeopleInitialSelection("")
        assertEquals(PeoplePrimaryTab.Activities, defaultSelection.primaryTab)
        assertEquals(PeopleCreationTab.Answers, defaultSelection.creationTab)
        assertEquals(null, defaultSelection.initialUserListType)

        val answerSelection = resolvePeopleInitialSelection("回答")
        assertEquals(PeoplePrimaryTab.Creations, answerSelection.primaryTab)
        assertEquals(PeopleCreationTab.Answers, answerSelection.creationTab)

        val articleSelection = resolvePeopleInitialSelection("文章")
        assertEquals(PeoplePrimaryTab.Creations, articleSelection.primaryTab)
        assertEquals(PeopleCreationTab.Articles, articleSelection.creationTab)

        val pinSelection = resolvePeopleInitialSelection("想法")
        assertEquals(PeoplePrimaryTab.Creations, pinSelection.primaryTab)
        assertEquals(PeopleCreationTab.Pins, pinSelection.creationTab)

        val colSelection = resolvePeopleInitialSelection("专栏")
        assertEquals(PeoplePrimaryTab.Creations, colSelection.primaryTab)
        assertEquals(PeopleCreationTab.Columns, colSelection.creationTab)

        val questionSelection = resolvePeopleInitialSelection("提问")
        assertEquals(PeoplePrimaryTab.Creations, questionSelection.primaryTab)
        assertEquals(PeopleCreationTab.Questions, questionSelection.creationTab)

        val creationSelection = resolvePeopleInitialSelection("创作")
        assertEquals(PeoplePrimaryTab.Creations, creationSelection.primaryTab)

        val actSelection = resolvePeopleInitialSelection("动态")
        assertEquals(PeoplePrimaryTab.Activities, actSelection.primaryTab)

        val favSelection = resolvePeopleInitialSelection("收藏")
        assertEquals(PeoplePrimaryTab.Collections, favSelection.primaryTab)

        val subSelection = resolvePeopleInitialSelection("关注订阅")
        assertEquals(PeoplePrimaryTab.FollowingSubscriptions, subSelection.primaryTab)

        val followingSelection = resolvePeopleInitialSelection("关注")
        assertEquals(PeoplePrimaryTab.Activities, followingSelection.primaryTab)
        assertEquals(PeopleUserListType.Following, followingSelection.initialUserListType)

        val followerSelection = resolvePeopleInitialSelection("粉丝")
        assertEquals(PeoplePrimaryTab.Activities, followerSelection.primaryTab)
        assertEquals(PeopleUserListType.Followers, followerSelection.initialUserListType)

        val unknownSelection = resolvePeopleInitialSelection("unknown")
        assertEquals(PeoplePrimaryTab.Activities, unknownSelection.primaryTab)
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
        assertEquals(1, peopleScreenInitialPage(Person(id = "1", urlToken = "u1")))
        assertEquals(0, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "回答")))
        assertEquals(0, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "文章")))
        assertEquals(0, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "专栏")))
        assertEquals(0, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "提问")))
        assertEquals(1, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "动态")))
        assertEquals(2, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "收藏")))
        assertEquals(3, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "关注订阅")))
        assertEquals(1, peopleScreenInitialPage(Person(id = "1", urlToken = "u1", jumpTo = "nonexistent")))
    }

    @Test
    fun testColumnWebUrl() {
        val apiCol = PeopleColumnItem(id = "test", title = "test", url = "http://www.zhihu.com/api/v4/columns/test")
        assertEquals("https://www.zhihu.com/column/test", apiCol.webUrl)

        val webCol = PeopleColumnItem(id = "test", title = "test", url = "http://zhuanlan.zhihu.com/test")
        assertEquals("https://zhuanlan.zhihu.com/test", webCol.webUrl)

        val rawCol = PeopleColumnItem(id = "my-column", title = "test", url = "")
        assertEquals("https://www.zhihu.com/column/my-column", rawCol.webUrl)
    }

    @Test
    fun testPeopleCreationItemToDestination() {
        val answerItem = PeopleCreationItem(
            id = 1001L,
            type = PostType.Answer,
            title = "Question Title",
            summary = "Sample answer excerpt",
            details = "回答 · 42 赞同 · 5 评论",
            authorName = "Author 1",
            authorBio = "Bio",
            avatarUrl = "https://example.com/avatar.jpg",
        )
        val answerDest = answerItem.toDestination()
        assertEquals(PostType.Answer, answerDest.type)
        assertEquals(1001L, answerDest.id)
        assertEquals("Question Title", answerDest.title)
        assertEquals("Author 1", answerDest.authorName)
        assertEquals("Bio", answerDest.authorBio)
        assertEquals("Sample answer excerpt", answerDest.excerpt)

        val articleItem = PeopleCreationItem(
            id = 3001L,
            type = PostType.Article,
            title = "Article Title",
            summary = "Sample article excerpt",
            details = "文章 · 88 赞同 · 12 评论",
            authorName = "Author 2",
            authorBio = "Bio",
            avatarUrl = "https://example.com/avatar2.jpg",
        )
        val articleDest = articleItem.toDestination()
        assertEquals(PostType.Article, articleDest.type)
        assertEquals(3001L, articleDest.id)
        assertEquals("Article Title", articleDest.title)

        val pinItem = PeopleCreationItem(
            id = 4001L,
            type = PostType.Pin,
            title = "",
            summary = "Pin HTML & Excerpt",
            details = "想法 · 15 赞 · 3 评论",
            authorName = "Author 3",
        )
        val pinDest = pinItem.toDestination()
        assertEquals(PostType.Pin, pinDest.type)
        assertEquals(4001L, pinDest.id)
        assertEquals("Author 3", pinDest.authorName)
    }

    @Test
    fun testMemberBadgeDtoMapping() {
        val badgeDto = MemberBadgeV2Dto(
            title = "优秀答主",
            icon = "https://example.com/icon.png",
            detailBadges = listOf(
                MemberBadgeItemDto(
                    title = "优秀答主",
                    description = "科技领域优秀答主",
                    icon = "https://example.com/detail.png",
                    type = "best_answerer",
                ),
            ),
        )
        val badge = badgeDto.toOfficialBadge()
        assertNotNull(badge)
        assertEquals("优秀答主", badge.title)
        assertEquals("科技领域优秀答主", badge.description)
        assertEquals("https://example.com/icon.png", badge.iconUrl)

        val details = badgeDto.toOfficialBadgeDetails()
        assertEquals(1, details.size)
        assertEquals("科技领域优秀答主", details.first().description)
    }

    @Test
    fun testMemberSocialMediaDtoMapping() {
        val githubMedia = MemberSocialMediaDto(
            type = "github",
            title = "GitHub · alex",
            link = "zhihu://user/alex",
            modules = listOf(
                MemberSocialMediaModuleDto(title = "stars", value = "1200"),
            ),
        )
        val githubState = githubMedia.toGithubSocialUiState()
        assertNotNull(githubState)
        assertEquals("GitHub · alex", githubState.title)
        assertEquals("1200", githubState.starCount)
        assertEquals("https://github.com/alex", githubState.profileUrl)

        val otherMedia = MemberSocialMediaDto(
            type = "weibo",
            title = "Weibo",
            link = "https://weibo.com",
        )
        assertNull(otherMedia.toGithubSocialUiState())
    }

    @Test
    fun testOfficialBadgeDetailTitle() {
        val certBadge = OfficialBadge(title = "认证", description = "个人认证")
        assertEquals("认证信息", certBadge.peopleDetailTitle)

        val passedBadge = OfficialBadge(title = "已认证的个人", description = "个人认证")
        assertEquals("认证信息", passedBadge.peopleDetailTitle)

        val customBadge = OfficialBadge(title = "优秀答主", description = "答主")
        assertEquals("优秀答主", customBadge.peopleDetailTitle)
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

        val columns = repo.getColumns("alex")
        assertEquals(1, columns.items.size)
        assertEquals("Compose Mastery", columns.items.first().title)

        val followers = repo.getFollowers("alex")
        assertEquals(1, followers.items.size)
        assertEquals("Follower 1", followers.items.first().name)
    }
}

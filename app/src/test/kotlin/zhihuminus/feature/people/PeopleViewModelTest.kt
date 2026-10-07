package com.zhihuminus.feature.people

import com.zhihuminus.navigation.Person
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class TestPeopleRepo : PeopleRepository {
        var isFollowed = false
        var isBlocked = false
        var shouldFail = false

        override suspend fun getProfile(userTokenOrId: String): PeopleProfile = PeopleProfile(
            id = "test-id",
            urlToken = userTokenOrId,
            name = "Test User",
            avatarUrl = "",
            headline = "",
            followerCount = 10,
            followingCount = 5,
            answerCount = 2,
            articleCount = 1,
            isFollowing = isFollowed,
            isBlocking = isBlocked,
        )

        override suspend fun follow(urlToken: String): Int? {
            if (shouldFail) throw RuntimeException("Follow failed")
            isFollowed = true
            return 11
        }

        override suspend fun unfollow(urlToken: String): Int? {
            if (shouldFail) throw RuntimeException("Unfollow failed")
            isFollowed = false
            return 10
        }

        override suspend fun block(urlToken: String) {
            if (shouldFail) throw RuntimeException("Block failed")
            isBlocked = true
        }

        override suspend fun unblock(urlToken: String) {
            if (shouldFail) throw RuntimeException("Unblock failed")
            isBlocked = false
        }

        override suspend fun getAnswers(userTokenOrId: String, sortBy: String, nextUrl: String?): PeoplePage<PeopleCreationItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getArticles(userTokenOrId: String, sortBy: String, nextUrl: String?): PeoplePage<PeopleCreationItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getActivities(userTokenOrId: String, nextUrl: String?): PeoplePage<com.zhihuminus.data.FeedDisplayItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getPins(userTokenOrId: String, nextUrl: String?): PeoplePage<PeopleCreationItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getCollections(userTokenOrId: String, nextUrl: String?): PeoplePage<com.zhihuminus.feature.collection.Collection> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getQuestions(userTokenOrId: String, nextUrl: String?): PeoplePage<FollowedQuestion> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getColumns(userTokenOrId: String, nextUrl: String?): PeoplePage<PeopleColumnItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowers(memberId: String, nextUrl: String?): PeoplePage<PeopleMemberItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowing(userTokenOrId: String, nextUrl: String?): PeoplePage<PeopleMemberItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowingColumns(userTokenOrId: String, nextUrl: String?): PeoplePage<PeopleColumnItem> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowingTopics(userTokenOrId: String, nextUrl: String?): PeoplePage<FollowedTopic> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowingQuestions(userTokenOrId: String, nextUrl: String?): PeoplePage<FollowedQuestion> =
            PeoplePage(emptyList(), null, true)

        override suspend fun getFollowingCollections(userTokenOrId: String, nextUrl: String?): PeoplePage<com.zhihuminus.feature.collection.Collection> =
            PeoplePage(emptyList(), null, true)
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testToggleFollowSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = TestPeopleRepo()
        val viewModel = PeopleViewModel(Person("1", "alice"), repo)
        advanceUntilIdle()

        // 1. Follow
        viewModel.onEvent(PeopleEvent.ToggleFollow)
        advanceUntilIdle()

        val followEffect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertEquals("已关注", followEffect.message)

        // 2. Unfollow
        viewModel.onEvent(PeopleEvent.ToggleFollow)
        advanceUntilIdle()

        val unfollowEffect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertEquals("已取消关注", unfollowEffect.message)
    }

    @Test
    fun testToggleFollowFailureEmitsErrorWithoutSuccessToast() = runTest(testDispatcher) {
        val repo = TestPeopleRepo()
        repo.shouldFail = true
        val viewModel = PeopleViewModel(Person("1", "alice"), repo)
        advanceUntilIdle()

        viewModel.onEvent(PeopleEvent.ToggleFollow)
        advanceUntilIdle()

        val effect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertTrue(effect.message.contains("操作失败"))
    }

    @Test
    fun testToggleItemFollowSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = TestPeopleRepo()
        val viewModel = PeopleViewModel(Person("1", "alice"), repo)
        advanceUntilIdle()

        val item = PeopleMemberItem(id = "user2", name = "Bob", urlToken = "bob", isFollowing = false)
        viewModel.onEvent(PeopleEvent.ToggleItemFollow(item))
        advanceUntilIdle()

        val effect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertEquals("已关注", effect.message)
    }

    @Test
    fun testToggleBlockSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = TestPeopleRepo()
        val viewModel = PeopleViewModel(Person("1", "alice"), repo)
        advanceUntilIdle()

        // Block
        viewModel.onEvent(PeopleEvent.ToggleBlock)
        advanceUntilIdle()

        val blockEffect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertEquals("已加入黑名单", blockEffect.message)

        // Unblock
        viewModel.onEvent(PeopleEvent.ToggleBlock)
        advanceUntilIdle()

        val unblockEffect = viewModel.effect.filterIsInstance<PeopleEffect.ShowMessage>().first()
        assertEquals("已移出黑名单", unblockEffect.message)
    }
}

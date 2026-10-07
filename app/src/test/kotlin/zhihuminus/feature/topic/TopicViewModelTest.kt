package com.zhihuminus.feature.topic

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class TopicViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeTopicRepository(
        var followResult: Result<Unit> = Result.success(Unit),
    ) : TopicRepository {
        val topic = TopicDetail(
            id = "123",
            name = "Test Topic",
            followersCount = 10,
            isFollowing = false,
        )

        override suspend fun getTopicDetail(topicId: String): TopicDetail = topic

        override suspend fun loadTopicFeed(
            topicId: String,
            tab: TopicFeedTab,
            discussionSort: TopicDiscussionSort,
            ideasSort: TopicIdeasSort,
            nextUrl: String?,
        ): TopicFeedResult = TopicFeedResult(
            items = emptyList(),
            nextUrl = null,
            isEnd = true,
        )

        override suspend fun setFollowing(topicId: String, following: Boolean): Result<Unit> = followResult
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
    fun testFollowTopicSuccessEmitsToast() = runTest(testDispatcher) {
        val repository = FakeTopicRepository(followResult = Result.success(Unit))
        val viewModel = TopicViewModel("123", "Test Topic", repository)
        advanceUntilIdle()

        viewModel.onEvent(TopicEvent.Follow(true))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is TopicEffect.ShowMessage)
        assertEquals("已关注话题", effect.message)
    }

    @Test
    fun testUnfollowTopicSuccessEmitsToast() = runTest(testDispatcher) {
        val repository = FakeTopicRepository(followResult = Result.success(Unit))
        val viewModel = TopicViewModel("123", "Test Topic", repository)
        advanceUntilIdle()

        // First follow so current is following
        viewModel.onEvent(TopicEvent.Follow(true))
        advanceUntilIdle()
        viewModel.effect.first() // consume first effect

        // Now unfollow
        viewModel.onEvent(TopicEvent.Follow(false))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is TopicEffect.ShowMessage)
        assertEquals("已取消关注话题", effect.message)
    }

    @Test
    fun testFollowTopicFailureEmitsErrorWithoutSuccessToast() = runTest(testDispatcher) {
        val repository = FakeTopicRepository(followResult = Result.failure(RuntimeException("Network error")))
        val viewModel = TopicViewModel("123", "Test Topic", repository)
        advanceUntilIdle()

        viewModel.onEvent(TopicEvent.Follow(true))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is TopicEffect.ShowMessage)
        assertTrue(effect.message.contains("关注失败"))
    }
}

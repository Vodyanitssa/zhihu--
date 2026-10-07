package com.zhihuminus.feature.comment

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
class CommentViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeCommentRepository : CommentRepository {
        var shouldFail = false
        val sampleComment = Comment(
            id = "c1",
            content = "Hello comment",
            author = CommentAuthor("a1", "Alice", "", "alice", ""),
            createdAt = 1000L,
            likeCount = 5,
            liked = false,
            canDelete = true,
            isAuthor = false,
            childCommentCount = 0,
            childComments = emptyList(),
            replyToAuthor = null,
            commentTags = emptyList(),
            authorTag = null,
        )

        override suspend fun getRootComments(
            type: CommentContentType,
            id: Long,
            orderBy: CommentSortOrder,
            offset: Int,
        ): CommentPage = CommentPage(
            comments = listOf(sampleComment),
            isEnd = true,
        )

        override suspend fun getNextPage(nextUrl: String): CommentPage = CommentPage(emptyList(), true)

        override suspend fun getChildComments(commentId: String, offset: Int): CommentPage =
            CommentPage(emptyList(), true)

        override suspend fun getComment(commentId: String): Comment = sampleComment

        override suspend fun submitComment(
            type: CommentContentType,
            id: Long,
            content: String,
            replyToCommentId: String?,
        ): Comment {
            if (shouldFail) throw RuntimeException("Submit failed")
            return sampleComment.copy(id = "new_c", content = content)
        }

        override suspend fun likeComment(commentId: String) {
            if (shouldFail) throw RuntimeException("Like failed")
        }

        override suspend fun unlikeComment(commentId: String) {
            if (shouldFail) throw RuntimeException("Unlike failed")
        }

        override suspend fun deleteComment(commentId: String) {
            if (shouldFail) throw RuntimeException("Delete failed")
        }
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
    fun testLikeCommentSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.LikeComment("c1"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CommentEffect.ShowMessage)
        assertEquals("已点赞", effect.message)
    }

    @Test
    fun testUnlikeCommentSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.UnlikeComment("c1"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CommentEffect.ShowMessage)
        assertEquals("已取消点赞", effect.message)
    }

    @Test
    fun testLikeCommentFailureEmitsErrorWithoutSuccessToast() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        repo.shouldFail = true
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.LikeComment("c1"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CommentEffect.ShowMessage)
        assertTrue(effect.message.contains("点赞失败"))
    }

    @Test
    fun testSubmitCommentSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.SubmitComment("Great post!"))
        advanceUntilIdle()

        // First effect is ScrollToTop, second is ShowMessage
        var foundToast: String? = null
        while (true) {
            val eff = viewModel.effect.first()
            if (eff is CommentEffect.ShowMessage) {
                foundToast = eff.message
                break
            }
        }
        assertEquals("评论已发送", foundToast)
    }

    @Test
    fun testDeleteCommentSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.DeleteComment("c1"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CommentEffect.ShowMessage)
        assertEquals("评论已删除", effect.message)
    }
}

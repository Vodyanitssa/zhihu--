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
import kotlin.test.assertFalse
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

        var rootCommentsPage: CommentPage = CommentPage(
            comments = listOf(sampleComment),
            isEnd = true,
        )
        var nextPageProvider: (String) -> CommentPage = { CommentPage(emptyList(), true) }
        var childCommentsPage: CommentPage = CommentPage(emptyList(), true)

        override suspend fun getRootComments(
            type: CommentContentType,
            id: Long,
            orderBy: CommentSortOrder,
            offset: Int,
        ): CommentPage = rootCommentsPage

        override suspend fun getNextPage(nextUrl: String): CommentPage = nextPageProvider(nextUrl)

        override suspend fun getChildComments(commentId: String, offset: Int): CommentPage =
            childCommentsPage

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
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
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
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
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
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
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
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
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
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.DeleteComment("c1"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is CommentEffect.ShowMessage)
        assertEquals("评论已删除", effect.message)
    }

    @Test
    fun testInitialLoadSetsIsEndWhenNoNextPage() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.isEnd)
        assertEquals(1, viewModel.uiState.items.size)
    }

    @Test
    fun testLoadMoreUpdatesIsEndAndItems() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        repo.rootCommentsPage = CommentPage(
            comments = listOf(repo.sampleComment),
            isEnd = false,
            nextUrl = "https://example.com/comments?offset=1",
        )
        val secondComment = repo.sampleComment.copy(id = "c2", content = "Second comment")
        repo.nextPageProvider = { url ->
            CommentPage(comments = listOf(secondComment), isEnd = true, nextUrl = null)
        }
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.isEnd)
        assertEquals(1, viewModel.uiState.items.size)

        viewModel.onEvent(CommentEvent.LoadMore)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.isEnd)
        assertEquals(2, viewModel.uiState.items.size)
    }

    @Test
    fun testChildCommentLoadMoreSetsIsEnd() = runTest(testDispatcher) {
        val repo = FakeCommentRepository()
        val parent = repo.sampleComment.copy(id = "parent_1", childCommentCount = 2)
        repo.rootCommentsPage = CommentPage(listOf(parent), isEnd = true)
        val child1 = repo.sampleComment.copy(id = "child_1", content = "Child 1")
        val child2 = repo.sampleComment.copy(id = "child_2", content = "Child 2")
        repo.childCommentsPage = CommentPage(
            comments = listOf(child1),
            isEnd = false,
            nextUrl = "https://example.com/child_comments?offset=1",
        )
        repo.nextPageProvider = { url ->
            CommentPage(comments = listOf(child2), isEnd = true, nextUrl = null)
        }
        val viewModel = CommentViewModel(CommentContentType.Answer, 100L, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.onEvent(CommentEvent.OpenChildComments(parent))
        advanceUntilIdle()

        val parentItem = viewModel.uiState.items.first { it.comment.id == "parent_1" }
        assertEquals(1, parentItem.children?.size)
        assertFalse(parentItem.isChildEnd)
        assertFalse(viewModel.isChildEnd)

        viewModel.loadMoreChildComments()
        advanceUntilIdle()

        val updatedParentItem = viewModel.uiState.items.first { it.comment.id == "parent_1" }
        assertEquals(2, updatedParentItem.children?.size)
        assertTrue(updatedParentItem.isChildEnd)
        assertTrue(viewModel.isChildEnd)
    }
}

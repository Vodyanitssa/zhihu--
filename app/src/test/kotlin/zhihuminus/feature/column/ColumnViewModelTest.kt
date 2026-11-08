package com.zhihuminus.feature.column

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.navigation.Person
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ColumnViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeColumnRepository(
        var detail: ColumnDetail = ColumnDetail(id = "c_1", title = "测试专栏", followerCount = 10),
        var detailError: Throwable? = null,
        var articles: ColumnArticleResult = ColumnArticleResult(emptyList(), null, true, 0),
        var followResult: Result<Unit> = Result.success(Unit),
        var authorFollowResult: Result<Unit> = Result.success(Unit),
    ) : ColumnRepository {
        override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticleResult = articles

        override suspend fun getColumnDetail(columnId: String): ColumnDetail {
            detailError?.let { throw it }
            return detail
        }

        override suspend fun setFollowing(columnId: String, following: Boolean) {
            followResult.exceptionOrNull()?.let { throw it }
        }

        override suspend fun setAuthorFollowing(urlToken: String, following: Boolean) {
            authorFollowResult.exceptionOrNull()?.let { throw it }
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
    fun testDetailAndTotalsLoadedIntoState() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(
            articles = ColumnArticleResult(
                articles = listOf(FeedDisplayItem(title = "文章", summary = null, details = "")),
                nextUrl = null,
                isEnd = true,
                totals = 26,
            ),
        )
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        assertEquals("测试专栏", viewModel.uiState.detail?.title)
        assertEquals(26, viewModel.uiState.totals)
        assertEquals(1, viewModel.uiState.articles.size)
        assertNull(viewModel.uiState.detailErrorMessage)
    }

    @Test
    fun testDetailFailureKeepsArticlesAndSetsRetryError() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(
            detailError = RuntimeException("Network error"),
            articles = ColumnArticleResult(
                articles = listOf(FeedDisplayItem(title = "文章", summary = null, details = "")),
                nextUrl = null,
                isEnd = true,
                totals = 1,
            ),
        )
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        assertNull(viewModel.uiState.detail)
        assertTrue(viewModel.uiState.detailErrorMessage != null)
        assertEquals(1, viewModel.uiState.articles.size)
    }

    @Test
    fun testRetryDetailRecoversAfterFailure() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(detailError = RuntimeException("Network error"))
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.detailErrorMessage != null)

        repository.detailError = null
        viewModel.onEvent(ColumnEvent.RetryDetail)
        advanceUntilIdle()

        assertEquals("测试专栏", viewModel.uiState.detail?.title)
        assertNull(viewModel.uiState.detailErrorMessage)
    }

    @Test
    fun testFollowSuccessUpdatesStateAndEmitsToast() = runTest(testDispatcher) {
        val repository = FakeColumnRepository()
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        viewModel.onEvent(ColumnEvent.Follow(true))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.detail?.isFollowing)
        assertEquals(11, viewModel.uiState.detail?.followerCount)
        assertEquals(false, viewModel.uiState.isFollowingChanging)

        val effect = viewModel.effect.first()
        assertTrue(effect is ColumnEffect.ShowMessage)
        assertEquals("已关注专栏", effect.message)
    }

    @Test
    fun testFollowFailureRollsBackAndEmitsError() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(followResult = Result.failure(RuntimeException("Network error")))
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        viewModel.onEvent(ColumnEvent.Follow(true))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.detail?.isFollowing)
        assertEquals(10, viewModel.uiState.detail?.followerCount)
        assertEquals(false, viewModel.uiState.isFollowingChanging)

        val effect = viewModel.effect.first()
        assertTrue(effect is ColumnEffect.ShowMessage)
        assertTrue(effect.isError)
        assertTrue(effect.message.contains("关注失败"))
    }

    @Test
    fun testAuthorClickEmitsPersonNavigation() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(
            detail = ColumnDetail(
                id = "c_1",
                title = "测试专栏",
                author = ColumnAuthor(id = "uid", urlToken = "token", name = "作者"),
            ),
        )
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        viewModel.onEvent(ColumnEvent.AuthorClick(id = "uid", urlToken = "token", name = "作者"))
        advanceUntilIdle()

        val effect = viewModel.effect.first()
        assertTrue(effect is ColumnEffect.Navigate)
        val destination = effect.destination
        assertTrue(destination is Person)
        assertEquals("uid", destination.id)
        assertEquals("token", destination.urlToken)
        assertEquals("作者", destination.name)
    }

    @Test
    fun testFollowAuthorSuccessUpdatesAuthorStateAndEmitsToast() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(
            detail = ColumnDetail(
                id = "c_1",
                title = "测试专栏",
                author = ColumnAuthor(id = "uid", urlToken = "token", name = "作者"),
            ),
        )
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        viewModel.onEvent(ColumnEvent.FollowAuthor(true))
        advanceUntilIdle()

        val author = viewModel.uiState.detail?.author
        assertEquals(true, author?.isFollowing)
        assertEquals(false, viewModel.uiState.isAuthorFollowingChanging)

        val effect = viewModel.effect.first()
        assertTrue(effect is ColumnEffect.ShowMessage)
        assertEquals("已关注作者", effect.message)
    }

    @Test
    fun testFollowAuthorFailureRollsBackAndEmitsError() = runTest(testDispatcher) {
        val repository = FakeColumnRepository(
            detail = ColumnDetail(
                id = "c_1",
                title = "测试专栏",
                author = ColumnAuthor(id = "uid", urlToken = "token", name = "作者", isFollowing = false),
            ),
            authorFollowResult = Result.failure(RuntimeException("Network error")),
        )
        val viewModel = ColumnViewModel("c_1", repository)
        advanceUntilIdle()

        viewModel.onEvent(ColumnEvent.FollowAuthor(true))
        advanceUntilIdle()

        val author = viewModel.uiState.detail?.author
        assertEquals(false, author?.isFollowing)
        assertEquals(false, viewModel.uiState.isAuthorFollowingChanging)

        val effect = viewModel.effect.first()
        assertTrue(effect is ColumnEffect.ShowMessage)
        assertTrue(effect.isError)
        assertTrue(effect.message.contains("关注失败"))
    }
}

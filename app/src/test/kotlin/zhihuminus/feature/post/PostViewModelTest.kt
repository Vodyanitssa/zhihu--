package com.zhihuminus.feature.post

import android.app.Application
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.watchlater.WatchLaterItem
import com.zhihuminus.feature.watchlater.WatchLaterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
class PostViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeWatchLaterRepository : WatchLaterRepository {
        val items = mutableListOf<WatchLaterItem>()
        private val _itemsFlow = MutableStateFlow<List<WatchLaterItem>>(emptyList())
        override val itemsFlow: StateFlow<List<WatchLaterItem>> = _itemsFlow

        override suspend fun getItems(): List<WatchLaterItem> = items.toList()

        override suspend fun addItem(item: WatchLaterItem) {
            items.add(0, item)
            _itemsFlow.value = items.toList()
        }

        override suspend fun deleteItem(id: Long, type: String) {
            items.removeAll { it.id == id && it.type == type }
            _itemsFlow.value = items.toList()
        }

        override suspend fun clearAll() {
            items.clear()
            _itemsFlow.value = emptyList()
        }
    }

    private class FakePostRepository(
        val postType: PostType = PostType.Answer,
        var shouldFail: Boolean = false,
        initialAuthor: Author? = null,
    ) : PostRepository {
        var voteCallCount = 0
        var lastVoteKey = ""
        var isAuthorFollowed = initialAuthor?.isFollowing ?: false

        val samplePost = Post(
            id = 12345L,
            type = postType,
            title = "Test Post",
            author = initialAuthor ?: Author(
                id = "auth1",
                name = "Author One",
                avatarUrl = "",
                urlToken = "author_one",
                isFollowing = isAuthorFollowed,
            ),
            content = emptyList(),
            voteCount = 42,
            commentCount = 10,
            voteState = VoteUpState.Neutral,
            isFaved = false,
        )

        var cachedPost: Post? = null
        var freshPost: Post? = null

        override suspend fun getPost(type: PostType, id: Long): Post = (freshPost ?: samplePost).copy(
            author = (freshPost ?: samplePost).author.copy(isFollowing = isAuthorFollowed),
        )

        override suspend fun getCachedPost(type: PostType, id: Long): Post? = cachedPost

        override suspend fun vote(postType: PostType, id: Long, vote: String): Int {
            if (shouldFail) throw RuntimeException("Vote failed")
            voteCallCount++
            lastVoteKey = vote
            return when (vote) {
                "up" -> 43
                "down" -> 41
                else -> 42
            }
        }

        override suspend fun submitPinPollVote(pollId: String, optionId: String) {
            if (shouldFail) throw RuntimeException("Poll vote failed")
        }

        override suspend fun getCollections(postType: PostType, id: Long): List<Collection> = listOf(
            Collection(id = "col1", title = "My Favorites", isFavorited = false),
            Collection(id = "col2", title = "Read Later", isFavorited = true),
        )

        override suspend fun addToCollection(postType: PostType, id: Long, collectionId: String) {
            if (shouldFail) throw RuntimeException("Add to collection failed")
        }

        override suspend fun removeFromCollection(postType: PostType, id: Long, collectionId: String) {
            if (shouldFail) throw RuntimeException("Remove from collection failed")
        }

        override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection {
            if (shouldFail) throw RuntimeException("Create collection failed")
            return Collection(id = "new_col", title = title, isPublic = isPublic)
        }

        var lastFollowedToken = ""
        var lastCachedPost: Post? = null

        override suspend fun followMember(urlToken: String, follow: Boolean) {
            if (shouldFail) throw RuntimeException("Follow failed")
            isAuthorFollowed = follow
            lastFollowedToken = urlToken
        }

        override suspend fun putCachedPost(post: Post) {
            lastCachedPost = post
        }

        override suspend fun recordHistory(postType: PostType, id: Long) {}
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
    fun testVoteAnswerSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        // 1. Vote Up
        viewModel.onEvent(PostEvent.VoteUp)
        advanceUntilIdle()

        val upEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已赞同", upEffect.message)

        // 2. Vote Up again -> Neutral (cancel upvote)
        viewModel.onEvent(PostEvent.VoteUp)
        advanceUntilIdle()

        val cancelUpEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已取消赞同", cancelUpEffect.message)

        // 3. Vote Down
        viewModel.onEvent(PostEvent.VoteDown)
        advanceUntilIdle()

        val downEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已反对", downEffect.message)

        // 4. Vote Down again -> Neutral (cancel downvote)
        viewModel.onEvent(PostEvent.VoteDown)
        advanceUntilIdle()

        val cancelDownEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已取消反对", cancelDownEffect.message)
    }

    @Test
    fun testVotePinSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Pin)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Pin, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        // 1. Like Pin
        viewModel.onEvent(PostEvent.VoteUp)
        advanceUntilIdle()

        val likeEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已点赞", likeEffect.message)

        // 2. Cancel Like Pin
        viewModel.onEvent(PostEvent.VoteUp)
        advanceUntilIdle()

        val cancelLikeEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已取消点赞", cancelLikeEffect.message)
    }

    @Test
    fun testVoteFailureEmitsErrorWithoutSuccessToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer, shouldFail = true)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.onEvent(PostEvent.VoteUp)
        advanceUntilIdle()

        val effect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertTrue(effect.message.contains("投票失败"))
    }

    @Test
    fun testToggleCollectionSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        // Add to col1 (isFavorited = false)
        val col1 = Collection(id = "col1", title = "My Favorites", isFavorited = false)
        viewModel.onEvent(PostEvent.ToggleCollection(col1))
        advanceUntilIdle()

        val addEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已收藏到「My Favorites」", addEffect.message)

        // Remove from col2 (isFavorited = true)
        val col2 = Collection(id = "col2", title = "Read Later", isFavorited = true)
        viewModel.onEvent(PostEvent.ToggleCollection(col2))
        advanceUntilIdle()

        val removeEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已从「Read Later」移除", removeEffect.message)
    }

    @Test
    fun testCreateCollectionSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.onEvent(PostEvent.CreateCollection("New Folder", "desc", true))
        advanceUntilIdle()

        val effect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("收藏夹已创建", effect.message)
    }

    @Test
    fun testFollowAuthorSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        // Follow
        viewModel.onEvent(PostEvent.FollowAuthor)
        advanceUntilIdle()

        val followEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已关注", followEffect.message)

        // Unfollow
        viewModel.onEvent(PostEvent.FollowAuthor)
        advanceUntilIdle()

        val unfollowEffect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已取消关注", unfollowEffect.message)
    }

    @Test
    fun testFollowAuthorFallsBackToIdAndCachesPost() = runTest(testDispatcher) {
        val repo = FakePostRepository(
            postType = PostType.Answer,
            initialAuthor = Author(
                id = "user_fallback_id",
                name = "Author Fallback",
                avatarUrl = "",
                urlToken = "",
                isFollowing = false,
            ),
        )
        val viewModel = PostViewModel(Application(), 12345L, PostType.Answer, repo, defaultDispatcher = testDispatcher)
        advanceUntilIdle()

        viewModel.onEvent(PostEvent.FollowAuthor)
        advanceUntilIdle()

        assertEquals("user_fallback_id", repo.lastFollowedToken)
        assertEquals(true, repo.isAuthorFollowed)
        assertEquals(true, repo.lastCachedPost?.author?.isFollowing)
    }

    @Test
    fun testAddToWatchLaterSuccessEmitsToast() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        val watchLaterRepo = FakeWatchLaterRepository()
        val viewModel = PostViewModel(
            Application(),
            12345L,
            PostType.Answer,
            repo,
            watchLaterRepository = watchLaterRepo,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        viewModel.onEvent(PostEvent.AddToWatchLater)
        advanceUntilIdle()

        assertEquals(1, watchLaterRepo.items.size)
        assertEquals(12345L, watchLaterRepo.items[0].id)
        assertEquals("answer", watchLaterRepo.items[0].type)
        assertEquals("Test Post", watchLaterRepo.items[0].title)

        val effect = viewModel.effect.filterIsInstance<PostEffect.ShowMessage>().first()
        assertEquals("已添加到稍后再看", effect.message)
    }

    @Test
    fun testCachedPostWithoutIpInfoRefreshesInBackground() = runTest(testDispatcher) {
        val repo = FakePostRepository(PostType.Answer)
        repo.cachedPost = repo.samplePost.copy(ipInfo = null)
        repo.freshPost = repo.samplePost.copy(ipInfo = "IP 属地上海")

        val viewModel = PostViewModel(
            Application(),
            12345L,
            PostType.Answer,
            repo,
            defaultDispatcher = testDispatcher,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.loadState
        assertTrue(state is PostLoadState.Success)
        assertEquals("IP 属地上海", state.post.ipInfo)
    }
}

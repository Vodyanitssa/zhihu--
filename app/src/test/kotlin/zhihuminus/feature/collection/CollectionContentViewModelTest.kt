package com.zhihuminus.feature.collection

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class CollectionContentViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeCollectionRepository(
        var collection: Collection? = Collection(id = "c1", title = "Test Collection", itemCount = 2),
        var items: List<FeedDisplayItem> = emptyList(),
        var totals: Int = 2,
        var collectionFailure: Throwable? = null,
        var itemsFailure: Throwable? = null,
        var removeFailure: Throwable? = null,
        var deleteFailure: Throwable? = null,
    ) : CollectionRepository {
        var lastRemovedType: String? = null
        var lastRemovedId: Long? = null

        override suspend fun getUserCollections(urlToken: String, nextUrl: String?): CollectionsPageResult =
            CollectionsPageResult(items = emptyList(), nextUrl = null, isEnd = true)

        override suspend fun getCollection(collectionId: String): Collection {
            collectionFailure?.let { throw it }
            return collection ?: error("collection missing")
        }

        override suspend fun getCollectionItems(
            collectionId: String,
            offset: Int?,
            limit: Int,
            nextUrl: String?,
        ): CollectionContentPageResult {
            itemsFailure?.let { throw it }
            return CollectionContentPageResult(
                items = items,
                nextUrl = null,
                isEnd = true,
                totals = totals,
            )
        }

        override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection =
            Collection(id = "created", title = title, description = description, isPublic = isPublic)

        override suspend fun deleteCollection(collectionId: String): Boolean {
            deleteFailure?.let { throw it }
            return true
        }

        override suspend fun getCollectionsForContent(type: String, id: Long): List<Collection> = emptyList()

        override suspend fun addToCollection(type: String, id: Long, collectionId: String) = Unit

        override suspend fun removeFromCollection(type: String, id: Long, collectionId: String) {
            removeFailure?.let { throw it }
            lastRemovedType = type
            lastRemovedId = id
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

    private fun sampleItem(id: Long = 1L): FeedDisplayItem = FeedDisplayItem(
        title = "Answer $id",
        summary = "summary",
        details = "0 赞同",
        navDestinationJson = PostDestination(type = PostType.Answer, id = id).toFeedDisplayItemNavDestinationJson(),
    )

    private fun sampleItems(vararg ids: Long): List<FeedDisplayItem> = ids.map { sampleItem(it) }

    @Test
    fun testLoadSuccessPopulatesState() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(items = sampleItems(1, 2), totals = 42)
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        assertEquals("Test Collection", viewModel.uiState.collection?.title)
        assertEquals(2, viewModel.uiState.items.size)
        assertEquals(42, viewModel.uiState.totalItems)
        assertNull(viewModel.uiState.errorMessage)
        assertNull(viewModel.uiState.detailErrorMessage)
    }

    @Test
    fun testDetailFailureKeepsItems() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(
            items = sampleItems(1),
            totals = 1,
            collectionFailure = RuntimeException("detail failed"),
        )
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.detailErrorMessage)
        assertNull(viewModel.uiState.collection)
        assertEquals(1, viewModel.uiState.items.size)
    }

    @Test
    fun testItemsFailureSetsErrorMessage() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(itemsFailure = RuntimeException("items failed"))
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.errorMessage)
        assertFalse(viewModel.uiState.isLoading)
        assertTrue(viewModel.uiState.items.isEmpty())
    }

    @Test
    fun testRefreshFailureWithExistingItemsEmitsMessage() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(items = sampleItems(1, 2), totals = 2)
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        repository.itemsFailure = RuntimeException("refresh failed")
        viewModel.onEvent(CollectionContentEvent.Refresh)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.items.size)
        assertNull(viewModel.uiState.errorMessage)
        val effect = viewModel.effect.first()
        assertTrue(effect is CollectionContentEffect.ShowMessage && effect.isError)
    }

    @Test
    fun testRemoveItemSuccessUpdatesListAndCount() = runTest(testDispatcher) {
        val item = sampleItem(1)
        val repository = FakeCollectionRepository(items = listOf(item, sampleItem(2)), totals = 2)
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        viewModel.onEvent(
            CollectionContentEvent.RequestRemoveItem(
                PendingCollectionItemRemoval(
                    type = "answer",
                    id = 1L,
                    stableKey = item.stableKey,
                    title = item.title,
                ),
            ),
        )
        viewModel.onEvent(CollectionContentEvent.ConfirmRemoveItem)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.items.size)
        assertEquals(1, viewModel.uiState.totalItems)
        assertNull(viewModel.uiState.itemPendingRemoval)
        assertEquals("answer", repository.lastRemovedType)
        assertEquals(1L, repository.lastRemovedId)
        val effect = viewModel.effect.first()
        assertTrue(effect is CollectionContentEffect.ShowMessage && effect.message == "已移出收藏夹")
    }

    @Test
    fun testRemoveItemFailureKeepsDialog() = runTest(testDispatcher) {
        val item = sampleItem(1)
        val repository = FakeCollectionRepository(
            items = listOf(item),
            totals = 1,
            removeFailure = RuntimeException("remove failed"),
        )
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        viewModel.onEvent(
            CollectionContentEvent.RequestRemoveItem(
                PendingCollectionItemRemoval(
                    type = "answer",
                    id = 1L,
                    stableKey = item.stableKey,
                    title = item.title,
                ),
            ),
        )
        viewModel.onEvent(CollectionContentEvent.ConfirmRemoveItem)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.items.size)
        assertNotNull(viewModel.uiState.itemPendingRemoval)
        assertNotNull(viewModel.uiState.removeError)
        assertFalse(viewModel.uiState.isRemovingItem)
    }

    @Test
    fun testDeleteSuccessEmitsNavigateBack() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(items = sampleItems(1), totals = 1)
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        viewModel.onEvent(CollectionContentEvent.RequestDelete)
        assertTrue(viewModel.uiState.showDeleteDialog)
        viewModel.onEvent(CollectionContentEvent.ConfirmDelete)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.showDeleteDialog)
        val effects = viewModel.effect.take(2).toList()
        assertTrue(effects.any { it is CollectionContentEffect.NavigateBack })
        assertTrue(effects.any { it is CollectionContentEffect.ShowMessage && it.message == "收藏夹已删除" })
    }

    @Test
    fun testDeleteFailureKeepsDialog() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(
            items = sampleItems(1),
            totals = 1,
            deleteFailure = RuntimeException("delete failed"),
        )
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        viewModel.onEvent(CollectionContentEvent.RequestDelete)
        viewModel.onEvent(CollectionContentEvent.ConfirmDelete)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.showDeleteDialog)
        assertNotNull(viewModel.uiState.deleteError)
        assertFalse(viewModel.uiState.isDeleting)
    }

    @Test
    fun testDefaultCollectionCannotBeDeleted() = runTest(testDispatcher) {
        val repository = FakeCollectionRepository(
            collection = Collection(id = "c1", title = "Default", itemCount = 1, isDefault = true),
            items = sampleItems(1),
            totals = 1,
        )
        val viewModel = CollectionContentViewModel("c1", repository)
        advanceUntilIdle()

        viewModel.onEvent(CollectionContentEvent.RequestDelete)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.showDeleteDialog)
        val effect = viewModel.effect.first()
        assertTrue(effect is CollectionContentEffect.ShowMessage && effect.isError && effect.message == "默认收藏夹不能删除")
    }
}

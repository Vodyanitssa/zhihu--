package com.zhihuminus.feature.watchlater

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
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
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WatchLaterViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private class FakeWatchLaterRepository(
        initialItems: List<WatchLaterItem> = emptyList(),
    ) : WatchLaterRepository {
        private val _itemsFlow = MutableStateFlow(initialItems)
        override val itemsFlow: StateFlow<List<WatchLaterItem>> = _itemsFlow

        override suspend fun getItems(): List<WatchLaterItem> = _itemsFlow.value

        override suspend fun addItem(item: WatchLaterItem) {
            val list = _itemsFlow.value.toMutableList()
            list.removeAll { it.id == item.id && it.type == item.type }
            list.add(0, item)
            _itemsFlow.value = list
        }

        override suspend fun deleteItem(id: Long, type: String) {
            val list = _itemsFlow.value.toMutableList()
            list.removeAll { it.id == id && it.type == type }
            _itemsFlow.value = list
        }

        override suspend fun clearAll() {
            _itemsFlow.value = emptyList()
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadObservesItems() = runTest(testDispatcher) {
        val sampleItem = WatchLaterItem(
            id = 123L,
            type = "answer",
            title = "回答标题",
            excerpt = "回答摘要",
            authorName = "作者A",
            authorAvatar = "https://avatar.com/a.jpg",
        )
        val repo = FakeWatchLaterRepository(listOf(sampleItem))
        val viewModel = WatchLaterViewModel(repo)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.items.size)
        assertEquals(sampleItem, viewModel.uiState.items[0])
    }

    @Test
    fun testDeleteItemEmitsToast() = runTest(testDispatcher) {
        val item1 = WatchLaterItem(id = 1L, type = "answer", title = "1")
        val item2 = WatchLaterItem(id = 2L, type = "article", title = "2")
        val repo = FakeWatchLaterRepository(listOf(item1, item2))
        val viewModel = WatchLaterViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(WatchLaterEvent.DeleteItem(item1))
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.items.size)
        assertEquals(2L, viewModel.uiState.items[0].id)

        val effect = viewModel.effect.filterIsInstance<WatchLaterEffect.ShowMessage>().first()
        assertEquals("已从稍后再看中删除", effect.message)
    }

    @Test
    fun testClearAllEmitsToast() = runTest(testDispatcher) {
        val item1 = WatchLaterItem(id = 1L, type = "answer", title = "1")
        val repo = FakeWatchLaterRepository(listOf(item1))
        val viewModel = WatchLaterViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(WatchLaterEvent.ClearAll)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.items.isEmpty())

        val effect = viewModel.effect.filterIsInstance<WatchLaterEffect.ShowMessage>().first()
        assertEquals("已清空稍后再看列表", effect.message)
    }

    @Test
    fun testContentClickEmitsNavigateWithPrewarmedMetadata() = runTest(testDispatcher) {
        val item = WatchLaterItem(
            id = 456L,
            type = "article",
            title = "文章标题",
            excerpt = "文章摘要",
            authorName = "作者B",
            authorAvatar = "https://avatar.com/b.jpg",
        )
        val repo = FakeWatchLaterRepository(listOf(item))
        val viewModel = WatchLaterViewModel(repo)
        advanceUntilIdle()

        viewModel.onEvent(WatchLaterEvent.ContentClick(item))
        advanceUntilIdle()

        val effect = viewModel.effect.filterIsInstance<WatchLaterEffect.Navigate>().first()
        assertTrue(effect.destination is PostDestination)
        val dest = effect.destination
        assertEquals(456L, dest.id)
        assertEquals(PostType.Article, dest.type)
        assertEquals("文章标题", dest.title)
        assertEquals("文章摘要", dest.excerpt)
        assertEquals("作者B", dest.authorName)
        assertEquals("https://avatar.com/b.jpg", dest.avatarSrc)
        assertEquals("watch_later", dest.readingQueueSourceId)
    }
}

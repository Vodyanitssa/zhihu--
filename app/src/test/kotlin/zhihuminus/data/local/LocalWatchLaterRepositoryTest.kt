package com.zhihuminus.data.local

import com.zhihuminus.feature.watchlater.WatchLaterItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LocalWatchLaterRepositoryTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var file: File

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        file = tempFolder.newFile("test_watch_later.json")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testEmptyFileInitializesEmptyList() = runTest(testDispatcher) {
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        assertTrue(repo.getItems().isEmpty())
        assertTrue(repo.itemsFlow.value.isEmpty())
    }

    @Test
    fun testAddItemAndPersistence() = runTest(testDispatcher) {
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        val item1 = WatchLaterItem(
            id = 1001L,
            type = "answer",
            title = "回答标题1",
            excerpt = "回答摘要1",
            authorName = "作者1",
            addedAt = 1000L,
        )
        repo.addItem(item1)

        assertEquals(1, repo.getItems().size)
        assertEquals(item1, repo.getItems().first())
        assertEquals(item1, repo.itemsFlow.value.first())

        // Create new instance reading same file
        val repo2 = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        assertEquals(1, repo2.getItems().size)
        assertEquals(item1, repo2.getItems().first())
    }

    @Test
    fun testAddItemDeduplicationAndOrder() = runTest(testDispatcher) {
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        val item1 = WatchLaterItem(
            id = 1001L,
            type = "answer",
            title = "回答标题1",
            excerpt = "回答摘要1",
            addedAt = 1000L,
        )
        val item2 = WatchLaterItem(
            id = 1002L,
            type = "article",
            title = "文章标题2",
            excerpt = "文章摘要2",
            addedAt = 2000L,
        )
        repo.addItem(item1)
        repo.addItem(item2)

        assertEquals(listOf(1002L, 1001L), repo.getItems().map { it.id })

        // Re-add item1 with updated title/time -> should move to index 0
        val updatedItem1 = item1.copy(title = "更新后的标题1", addedAt = 3000L)
        repo.addItem(updatedItem1)

        val items = repo.getItems()
        assertEquals(2, items.size)
        assertEquals(1001L, items[0].id)
        assertEquals("更新后的标题1", items[0].title)
        assertEquals(1002L, items[1].id)
    }

    @Test
    fun testDeleteItem() = runTest(testDispatcher) {
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        val item1 = WatchLaterItem(id = 1001L, type = "answer", title = "1")
        val item2 = WatchLaterItem(id = 1002L, type = "article", title = "2")
        repo.addItem(item1)
        repo.addItem(item2)

        repo.deleteItem(1001L, "answer")
        assertEquals(listOf(1002L), repo.getItems().map { it.id })
    }

    @Test
    fun testClearAll() = runTest(testDispatcher) {
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        repo.addItem(WatchLaterItem(id = 1001L, type = "answer", title = "1"))
        repo.addItem(WatchLaterItem(id = 1002L, type = "article", title = "2"))

        assertEquals(2, repo.getItems().size)
        repo.clearAll()
        assertTrue(repo.getItems().isEmpty())
        assertTrue(repo.itemsFlow.value.isEmpty())

        // Re-read file
        val repo2 = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        assertTrue(repo2.getItems().isEmpty())
    }

    @Test
    fun testCorruptFileHandlesGracefully() = runTest(testDispatcher) {
        file.writeText("invalid json content")
        val repo = FileWatchLaterRepository(file, ioDispatcher = testDispatcher)
        assertTrue(repo.getItems().isEmpty())
    }
}

package com.zhihuminus.data.local

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchHistoryStoreTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEmptyHistoryInitial() {
        val file = File(tempFolder.root, "search_history.json")
        val storage = FileSearchHistoryStore(file)
        assertTrue(storage.history.isEmpty())
    }

    @Test
    fun testAddAndPersistence() {
        val file = File(tempFolder.root, "search_history.json")
        val storage = FileSearchHistoryStore(file, maxSize = 3)

        storage.add("Kotlin")
        storage.add("Compose")
        storage.add("Android")
        storage.add("Kotlin") // Move to front

        assertEquals(listOf("Kotlin", "Android", "Compose"), storage.history)

        // Reload from disk
        val reloaded = FileSearchHistoryStore(file, maxSize = 3)
        assertEquals(listOf("Kotlin", "Android", "Compose"), reloaded.history)
    }

    @Test
    fun testMaxSizeLimit() {
        val file = File(tempFolder.root, "search_history.json")
        val storage = FileSearchHistoryStore(file, maxSize = 2)

        storage.add("A")
        storage.add("B")
        storage.add("C")

        assertEquals(listOf("C", "B"), storage.history)
    }

    @Test
    fun testClear() {
        val file = File(tempFolder.root, "search_history.json")
        val storage = FileSearchHistoryStore(file)
        storage.add("Test")
        storage.clear()
        assertTrue(storage.history.isEmpty())
    }
}

package com.zhihuminus.feature.collection

import com.zhihuminus.data.FeedDisplayItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionTest {
    @Test
    fun testPickDefaultCollectionId() {
        val emptyList = emptyList<Collection>()
        assertNull(pickDefaultCollectionId(emptyList))

        val listWithoutDefault = listOf(
            Collection(id = "1", title = "First"),
            Collection(id = "2", title = "Second"),
        )
        assertEquals("1", pickDefaultCollectionId(listWithoutDefault))

        val listWithDefault = listOf(
            Collection(id = "1", title = "First"),
            Collection(id = "2", title = "Default", isDefault = true),
            Collection(id = "3", title = "Third"),
        )
        assertEquals("2", pickDefaultCollectionId(listWithDefault))
    }

    @Test
    fun testShouldReuseCollectionRandomSession() {
        assertTrue(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 100,
                requestedRandomSeed = 42,
                requestedItemCount = 100,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )

        assertFalse(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 100,
                requestedRandomSeed = 43,
                requestedItemCount = 100,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )

        assertFalse(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 100,
                requestedRandomSeed = 42,
                requestedItemCount = 50,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )
    }

    @Test
    fun testCollectionRandomPageOffsets() {
        val offsets = collectionRandomPageOffsets(
            itemCount = 65,
            randomSeed = 12345,
            pageSize = 20,
        )
        assertEquals(4, offsets.size)
        assertTrue(offsets.containsAll(listOf(0, 20, 40, 60)))
    }

    @Test
    fun testOrderCollectionItemsSequential() {
        val items = listOf(
            FeedDisplayItem(title = "Item 1", summary = "Summary 1", details = "Details 1", feed = null),
            FeedDisplayItem(title = "Item 2", summary = "Summary 2", details = "Details 2", feed = null),
        )
        val ordered = orderCollectionItems(
            items = items,
            randomMode = false,
            randomSeed = 999,
        )
        assertEquals(items, ordered)
    }

    @Test
    fun testOrderCollectionItemsRandomRetainsPreviousKeys() {
        val item1 = FeedDisplayItem(title = "Item 1", summary = "Summary 1", details = "Details 1", feed = null)
        val item2 = FeedDisplayItem(title = "Item 2", summary = "Summary 2", details = "Details 2", feed = null)
        val item3 = FeedDisplayItem(title = "Item 3", summary = "Summary 3", details = "Details 3", feed = null)

        val initial = orderCollectionItems(
            items = listOf(item1, item2),
            randomMode = true,
            randomSeed = 123,
        )
        val retainedKeys = initial.map { it.stableKey }

        val withNewItem = orderCollectionItems(
            items = listOf(item1, item2, item3),
            randomMode = true,
            randomSeed = 123,
            previousRandomOrderKeys = retainedKeys,
        )

        assertEquals(retainedKeys, withNewItem.take(2).map { it.stableKey })
        assertEquals(3, withNewItem.size)
    }

    @Test
    fun testCollectionSubtitleText() {
        val publicCollection = Collection(id = "1", title = "Public Col", itemCount = 5, isPublic = true)
        assertEquals("5 内容·公开", publicCollection.subtitleText)

        val privateCollection = Collection(id = "2", title = "Private Col", itemCount = 5, isPublic = false)
        assertEquals("5 内容·仅自己可见", privateCollection.subtitleText)

        val zeroItemCollection = Collection(id = "3", title = "Zero Col", itemCount = 0, isPublic = false)
        assertEquals("0 内容·仅自己可见", zeroItemCollection.subtitleText)
    }
}

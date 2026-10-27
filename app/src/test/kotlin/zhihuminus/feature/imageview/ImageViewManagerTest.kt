package com.zhihuminus.feature.imageview

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.extractAllImageUrls
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ImageViewManagerTest {
    @Test
    fun `submitImages filters blank and data uris`() {
        val manager = ImageViewManager()
        manager.submitImages(
            listOf(
                "https://example.com/img1.jpg",
                "",
                "   ",
                "data:image/png;base64,123",
                "https://example.com/img2.jpg",
            ),
        )

        assertEquals(2, manager.images.size)
        assertEquals("https://example.com/img1.jpg", manager.images[0])
        assertEquals("https://example.com/img2.jpg", manager.images[1])
    }

    @Test
    fun `show with known url selects existing index`() {
        val manager = ImageViewManager()
        manager.submitImages(listOf("https://example.com/img1.jpg", "https://example.com/img2.jpg"))

        manager.show("https://example.com/img2.jpg")

        assertTrue(manager.isShowing)
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun `show with unknown url appends dynamically and selects it`() {
        val manager = ImageViewManager()
        manager.submitImages(listOf("https://example.com/img1.jpg"))

        manager.show("https://example.com/new.jpg")

        assertTrue(manager.isShowing)
        assertEquals(2, manager.images.size)
        assertEquals("https://example.com/new.jpg", manager.images[1])
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun `show with multi urls sets images and clamped index`() {
        val manager = ImageViewManager()
        manager.show(listOf("https://example.com/a.jpg", "https://example.com/b.jpg"), initialIndex = 5)

        assertTrue(manager.isShowing)
        assertEquals(2, manager.images.size)
        assertEquals(1, manager.currentIndex)
    }

    @Test
    fun `dismiss resets currentIndex to negative one`() {
        val manager = ImageViewManager()
        manager.show(listOf("https://example.com/a.jpg"))
        assertTrue(manager.isShowing)

        manager.dismiss()

        assertFalse(manager.isShowing)
        assertEquals(-1, manager.currentIndex)
    }

    @Test
    fun `extractAllImageUrls extracts from nested ast nodes`() {
        val nodes = listOf(
            ContentNode.Image(url = "https://example.com/img1.jpg", caption = null),
            ContentNode.Listing(
                items = listOf(
                    ContentNode.Image(url = "https://example.com/img2.jpg", caption = "in list"),
                    ContentNode.Image(url = "data:image/svg+xml,...", caption = "data uri"),
                ),
                isSorted = false,
            ),
            ContentNode.Splitter,
        )

        val extracted = nodes.extractAllImageUrls()

        assertEquals(2, extracted.size)
        assertEquals("https://example.com/img1.jpg", extracted[0])
        assertEquals("https://example.com/img2.jpg", extracted[1])
    }
}

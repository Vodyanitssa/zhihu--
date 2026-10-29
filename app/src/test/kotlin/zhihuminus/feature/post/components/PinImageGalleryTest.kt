package com.zhihuminus.feature.post.components

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PinImageGalleryTest {
    @Test
    fun testPinGalleryMode() {
        assertEquals(PinGalleryMode.NONE, pinGalleryMode(0))
        assertEquals(PinGalleryMode.NONE, pinGalleryMode(-1))
        assertEquals(PinGalleryMode.SINGLE, pinGalleryMode(1))
        assertEquals(PinGalleryMode.STRIP, pinGalleryMode(2))
        assertEquals(PinGalleryMode.STRIP, pinGalleryMode(9))
    }

    @Test
    fun testPinImageAspectRatio() {
        assertEquals(1.5f, pinImageAspectRatio(1500f, 1000f))
        assertEquals(0.75f, pinImageAspectRatio(750f, 1000f))
        assertNull(pinImageAspectRatio(0f, 1000f))
        assertNull(pinImageAspectRatio(1000f, 0f))
        assertNull(pinImageAspectRatio(-1000f, 1000f))
        assertNull(pinImageAspectRatio(Float.NaN, 1000f))
        assertNull(pinImageAspectRatio(1000f, Float.POSITIVE_INFINITY))
        assertNull(pinImageAspectRatio(1f, 1000f))
        assertNull(pinImageAspectRatio(1000f, 1f))
    }

    @Test
    fun testPinThumbnailWidth() {
        val barHeight = 200.dp
        assertEquals(150.dp, pinThumbnailWidth(barHeight, null))
        assertEquals(200.dp, pinThumbnailWidth(barHeight, 1f))
        assertEquals(300.dp, pinThumbnailWidth(barHeight, 3f))
        assertEquals(80.dp, pinThumbnailWidth(barHeight, 0.1f))
        assertEquals(300.dp, pinThumbnailWidth(barHeight, 10f))
    }
}

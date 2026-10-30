package com.zhihuminus.feature.post.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PinImageGalleryTest {
    @Test
    fun testPinGalleryMode() {
        assertEquals(PinGalleryMode.NONE, pinGalleryMode(0))
        assertEquals(PinGalleryMode.NONE, pinGalleryMode(-1))
        assertEquals(PinGalleryMode.SINGLE, pinGalleryMode(1))
        assertEquals(PinGalleryMode.STRIP, pinGalleryMode(2))
        assertEquals(PinGalleryMode.STRIP, pinGalleryMode(9))
    }
}

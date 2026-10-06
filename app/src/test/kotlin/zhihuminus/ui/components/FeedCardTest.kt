package com.zhihuminus.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedCardTest {
    @Test
    fun testPinFeedImageLayoutMapping() {
        assertNull(pinFeedImageLayout(0))
        assertEquals(PinFeedImageLayout.SINGLE, pinFeedImageLayout(1))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(2))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(3))
        assertEquals(PinFeedImageLayout.MULTI_ROW, pinFeedImageLayout(4))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(5))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(9))
        assertEquals(PinFeedImageLayout.NINE_GRID, pinFeedImageLayout(15))
    }

    @Test
    fun testFeedCardConfigDefaults() {
        val defaultConfig = FeedCardConfig()
        assertEquals(100, defaultConfig.fontSizePercent)
        assertEquals(160, defaultConfig.lineHeightPercent)
    }

    @Test
    fun testFeedCardConfigCustomValues() {
        val customConfig = FeedCardConfig(
            fontSizePercent = 120,
            lineHeightPercent = 180,
        )
        assertEquals(120, customConfig.fontSizePercent)
        assertEquals(180, customConfig.lineHeightPercent)
    }
}

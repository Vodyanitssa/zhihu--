package com.zhihuminus.core.content

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.zhihuminus.core.content.renderer.ContentTypographyConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentTypographyConfigTest {
    @Test
    fun testDefaultValues() {
        val config = ContentTypographyConfig()
        assertEquals(100, config.fontSizePercent)
        assertEquals(160, config.lineHeightPercent)
        assertTrue(config.enabled)
    }

    @Test
    fun testScaleStyleDefault() {
        val config = ContentTypographyConfig()
        val baseStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        val scaled = config.scaleStyle(baseStyle)

        assertEquals(16.sp, scaled.fontSize)
        assertEquals(25.6.sp, scaled.lineHeight)
    }

    @Test
    fun testScaleStyleCustomPercentages() {
        val config = ContentTypographyConfig(
            fontSizePercent = 150,
            lineHeightPercent = 200,
        )
        val baseStyle = TextStyle(fontSize = 16.sp)
        val scaled = config.scaleStyle(baseStyle)

        assertEquals(24.sp, scaled.fontSize)
        assertEquals(48.sp, scaled.lineHeight)
    }

    @Test
    fun testScaleStyleWithMaxLineHeightClamp() {
        val config = ContentTypographyConfig(
            fontSizePercent = 120,
            lineHeightPercent = 250,
        )
        val baseStyle = TextStyle(fontSize = 32.sp)
        val scaled = config.scaleStyle(baseStyle, maxLineHeight = 1.3f)

        assertEquals(38.4.sp, scaled.fontSize)
        // 38.4 * 1.3 = 49.92.sp
        assertEquals(49.92.sp.value, scaled.lineHeight.value, 0.001f)
    }

    @Test
    fun testDisabledConfigDoesNotScale() {
        val config = ContentTypographyConfig.DISABLED
        assertFalse(config.enabled)

        val baseStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp)
        val scaled = config.scaleStyle(baseStyle)

        assertEquals(baseStyle, scaled)
    }
}

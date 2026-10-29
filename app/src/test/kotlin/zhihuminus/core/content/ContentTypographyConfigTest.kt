package com.zhihuminus.core.content

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    @Test
    fun testParagraphSpacingDefault() {
        val spacing = ContentTypographyConfig().paragraphSpacing(Density(1f))

        // 16sp * ((1.6 - 1) + 0.5) = 17.6sp，density = 1 时等于 17.6dp
        assertEquals(17.6f, spacing.value, 0.001f)
    }

    @Test
    fun testParagraphSpacingExceedsLineGapAtMaxLineHeight() {
        val config = ContentTypographyConfig(lineHeightPercent = 300)
        val spacing = config.paragraphSpacing(Density(1f))

        // 行间距 = 16sp * (3.0 - 1) = 32sp，段间距必须更大
        assertTrue(spacing.value > 32f)
    }

    @Test
    fun testParagraphSpacingDisabled() {
        val spacing = ContentTypographyConfig.DISABLED.paragraphSpacing(Density(1f))

        assertEquals(16.dp, spacing)
    }
}

package com.zhihuminus.core.content.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.zhihuminus.core.settings.LocalAppSettings

/**
 * 富文本 AST 渲染排版配置。
 *
 * 封装正文及富文本阅读的字号缩放比例与行高倍率，统一驱动 [ComposeRenderer] 中所有
 * [com.zhihuminus.core.content.ContentNode] 与 [com.zhihuminus.core.content.InlineNode] 的样式计算。
 */
@Immutable
data class ContentTypographyConfig(
    val fontSizePercent: Int = 100,
    val lineHeightPercent: Int = 160,
    val enabled: Boolean = true,
) {
    /**
     * 将基准样式按照当前排版配置进行缩放。
     *
     * @param baseStyle 原始基础样式
     * @param maxLineHeight 可选的最大行高倍率上限。例如标题或代码块不宜应用长文的高倍行距（如 2.5x），可在此限定上限。
     */
    fun scaleStyle(baseStyle: TextStyle, maxLineHeight: Float? = null): TextStyle {
        if (!enabled) return baseStyle
        val fontScale = fontSizePercent / 100f
        val requestedLineScale = lineHeightPercent / 100f
        val lineScale = if (maxLineHeight != null) minOf(requestedLineScale, maxLineHeight) else requestedLineScale

        val baseFontSize = if (baseStyle.fontSize.isSpecified) baseStyle.fontSize else 16.sp
        val scaledFontSize = baseFontSize * fontScale
        val scaledLineHeight = scaledFontSize * lineScale

        return baseStyle.copy(
            fontSize = scaledFontSize,
            lineHeight = scaledLineHeight,
        )
    }

    companion object {
        val DISABLED = ContentTypographyConfig(
            fontSizePercent = 100,
            lineHeightPercent = 160,
            enabled = false,
        )
    }
}

/**
 * AST 富文本排版配置的 CompositionLocal。
 */
val LocalContentTypographyConfig = compositionLocalOf { ContentTypographyConfig() }

/**
 * 从全局设置中响应式读取并计算 [ContentTypographyConfig]，供上层容器注入给 [LocalContentTypographyConfig]。
 */
@Composable
fun rememberContentTypographyConfig(): ContentTypographyConfig {
    val reading = LocalAppSettings.current.reading
    return remember(reading.fontSizePercent, reading.lineHeightPercent) {
        ContentTypographyConfig(
            fontSizePercent = reading.fontSizePercent,
            lineHeightPercent = reading.lineHeightPercent,
            enabled = true,
        )
    }
}

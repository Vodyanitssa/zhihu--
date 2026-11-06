package com.zhihuminus.core.content.renderer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.FormulaManager
import com.zhihuminus.feature.imageview.ImageViewManager

/**
 * 图片查看管理器的 CompositionLocal。
 *
 * Screen 层通过 [CompositionLocalProvider] 提供 [ImageViewManager] 实例，
 * Renderer 层的 Image composable 在点击时直接调用 [ImageViewManager.show]，
 * 无需通过回调层层传递。
 */
val LocalImageViewManager = staticCompositionLocalOf<ImageViewManager?> { null }

/**
 * 渲染一组块级内容节点。
 *
 * ## 文件拆分约定
 *
 * - 对外渲染后端与入口保留 `Renderer` 后缀：`ComposeRenderer`、`HtmlRenderer`、`PictureRenderer`。
 * - 内部分片按内容形态命名，不带 `Renderer` 后缀：
 *   `InlineText`（行内节点）、`BlockText`（文本类块）、`Media`（图片与视频）、`Table`（表格与列宽计算）。
 * - `LocalImageViewManager` 是跨模块的 CompositionLocal 契约，继续由本入口文件提供。
 */
@Composable
fun RenderContentNodes(
    nodes: List<ContentNode>,
    modifier: Modifier = Modifier,
    spacing: Dp? = null,
) {
    val typographyConfig = LocalContentTypographyConfig.current
    val density = LocalDensity.current
    val itemSpacing = spacing ?: typographyConfig.paragraphSpacing(density)
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(itemSpacing),
    ) {
        nodes.forEach { node ->
            RenderContentNode(node)
        }
    }
}

@Composable
fun RenderContentNode(
    node: ContentNode,
) {
    when (node) {
        is ContentNode.Paragraph -> {
            if (node.content.isNotEmpty()) {
                InlineNodes(
                    nodes = node.content,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        is ContentNode.Heading -> {
            val baseStyle = when (node.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            val effectiveStyle = scaledContentStyle(baseStyle, maxLineHeight = 1.3f)
            Text(
                text = node.content,
                style = effectiveStyle,
                fontWeight = FontWeight.Bold,
            )
        }

        is ContentNode.Splitter -> HorizontalDivider()

        is ContentNode.Link -> {
            Link(node)
        }

        is ContentNode.Code -> {
            Code(node)
        }

        is ContentNode.Formula -> {
            FormulaManager.FormulaResolveEffect(node.url)
            val localUrl = FormulaManager.urlToLocal[node.url] ?: node.url
            Image(
                ContentNode.Image(url = localUrl, caption = null),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface),
            )
        }

        is ContentNode.Quote -> {
            Quote(node)
        }

        is ContentNode.Image -> {
            Image(node)
        }

        is ContentNode.Video -> {
            Video(node)
        }

        is ContentNode.Listing -> {
            Listing(node)
        }

        is ContentNode.Table -> {
            Table(node)
        }
    }
}

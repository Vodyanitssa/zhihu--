package com.zhihuminus.core.content.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.content.ContentNode

/**
 * 表格列布局的计算结果。
 *
 * @property columnCount 表格的列数（取所有行中最大的单元格数量）
 * @property minWidths 每一列在窄屏横向滚动时的最小宽度
 * @property weights 每一列在宽屏铺满时的宽度权重
 * @property totalMinWidth 所有列最小宽度之和，用于判断是否需要横向滚动
 */
internal data class TableColumnLayout(
    val columnCount: Int,
    val minWidths: List<Dp>,
    val weights: List<Float>,
    val totalMinWidth: Dp,
)

private const val SHORT_TEXT_VISUAL_WIDTH = 6
private const val MEDIUM_TEXT_VISUAL_WIDTH = 24

/**
 * 计算文本的可视宽度：ASCII 字符记 1，非 ASCII（中文等）记 2。
 */
internal fun visualLength(text: String): Int = text.fold(0) { count, char ->
    count + if (char.code > 127) 2 else 1
}

/**
 * 依据每一列最长单元格的可视宽度计算列布局。
 *
 * 返回 `null` 表示没有可渲染的列（没有行，或所有行都是空单元格），调用方应跳过整个表格。
 */
internal fun computeTableColumnLayout(rows: List<ContentNode.TableRow>): TableColumnLayout? {
    if (rows.isEmpty()) return null

    val columnCount = rows.maxOf { it.content.size }
    if (columnCount == 0) return null

    val maxVisualLenPerColumn = (0 until columnCount).map { colIndex ->
        rows.maxOf { row ->
            visualLength(
                row.content
                    .getOrNull(colIndex)
                    ?.content
                    .orEmpty(),
            )
        }
    }

    val minWidths = maxVisualLenPerColumn.map { visualLen ->
        when {
            visualLen <= SHORT_TEXT_VISUAL_WIDTH -> 72.dp
            visualLen <= MEDIUM_TEXT_VISUAL_WIDTH -> 108.dp
            else -> 160.dp
        }
    }
    val weights = maxVisualLenPerColumn.map { visualLen ->
        when {
            visualLen <= SHORT_TEXT_VISUAL_WIDTH -> 1.0f
            visualLen <= MEDIUM_TEXT_VISUAL_WIDTH -> 1.6f
            else -> 2.4f
        }
    }

    return TableColumnLayout(
        columnCount = columnCount,
        minWidths = minWidths,
        weights = weights,
        totalMinWidth = minWidths.fold(0.dp) { acc, dp -> acc + dp },
    )
}

@Composable
internal fun Table(node: ContentNode.Table) {
    val layout = remember(node) { computeTableColumnLayout(node.rows) } ?: return
    val columnCount = layout.columnCount
    val colMinWidths = layout.minWidths
    val colWeights = layout.weights
    val totalDesiredMinWidth = layout.totalMinWidth

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val fitsOnScreen = totalDesiredMinWidth <= maxWidth
        val scrollState = rememberScrollState()
        val fadeColor = MaterialTheme.colorScheme.surface

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(8.dp),
                ).background(MaterialTheme.colorScheme.surface)
                .drawWithContent {
                    drawContent()
                    if (!fitsOnScreen) {
                        val fadeWidth = 20.dp.toPx()
                        if (scrollState.canScrollBackward) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(fadeColor.copy(alpha = 0.9f), Color.Transparent),
                                    startX = 0f,
                                    endX = fadeWidth,
                                ),
                                size = Size(fadeWidth, size.height),
                            )
                        }
                        if (scrollState.canScrollForward) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, fadeColor.copy(alpha = 0.9f)),
                                    startX = size.width - fadeWidth,
                                    endX = size.width,
                                ),
                                topLeft = Offset(size.width - fadeWidth, 0f),
                                size = Size(fadeWidth, size.height),
                            )
                        }
                    }
                },
        ) {
            Row(
                modifier = if (fitsOnScreen) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                },
            ) {
                Column(
                    modifier = if (fitsOnScreen) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.wrapContentWidth()
                    },
                ) {
                    node.rows.forEachIndexed { rowIndex, row ->
                        val isHeaderRow = row == node.header || (rowIndex == 0 && node.header == null && row.content.all { it.isHeader })
                        val dataRowIndex = if (node.header != null || (rowIndex == 0 && row.content.all { it.isHeader })) {
                            rowIndex - 1
                        } else {
                            rowIndex
                        }
                        val rowBg = when {
                            isHeaderRow -> MaterialTheme.colorScheme.surfaceContainerHigh
                            dataRowIndex % 2 == 1 -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.25f)
                            else -> Color.Transparent
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(rowBg),
                        ) {
                            repeat(columnCount) { columnIndex ->
                                if (columnIndex > 0) {
                                    Box(
                                        modifier = Modifier
                                            .width(0.5.dp)
                                            .fillMaxHeight()
                                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    )
                                }
                                val cell = row.content.getOrNull(columnIndex)
                                val cellModifier = if (fitsOnScreen) {
                                    Modifier.weight(colWeights[columnIndex])
                                } else {
                                    Modifier.width(colMinWidths[columnIndex])
                                }.fillMaxHeight()

                                TableCell(
                                    text = cell?.content.orEmpty(),
                                    isHeader = cell?.isHeader == true || isHeaderRow,
                                    modifier = cellModifier,
                                )
                            }
                        }

                        if (rowIndex < node.rows.lastIndex) {
                            val dividerThickness = if (isHeaderRow) 1.dp else 0.5.dp
                            val dividerAlpha = if (isHeaderRow) 1f else 0.6f
                            HorizontalDivider(
                                thickness = dividerThickness,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = dividerAlpha),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    isHeader: Boolean,
    modifier: Modifier = Modifier,
) {
    val baseStyle = if (isHeader) {
        MaterialTheme.typography.titleSmall
    } else {
        MaterialTheme.typography.bodyMedium
    }
    val effectiveStyle = scaledContentStyle(baseStyle, maxLineHeight = 1.3f)
    Box(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.TopStart,
    ) {
        Text(
            text = text,
            style = effectiveStyle,
            fontWeight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

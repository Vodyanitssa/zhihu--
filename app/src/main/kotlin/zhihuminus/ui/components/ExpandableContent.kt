package com.zhihuminus.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse

private const val CONTENT_SLOT = 0
private const val EXPAND_DURATION = 320
private const val SCRIM_FADE_IN = 100
private const val SCRIM_FADE_OUT = 130
private const val SCRIM_MID_ALPHA = 0.55f
private val SCRIM_HEIGHT: Dp = 48.dp

/**
 * 可展开/收起的通用内容块，内容可以是纯文本，也可以是包含图片的任意 composable。
 *
 * 展开状态由调用方持有（受控组件），折叠量为 [collapsedMaxHeight]：内容自然高度超过它时
 * 才显示按钮与底部渐变遮罩，收起时把内容裁切到该高度。
 * 组件不内置水平内边距，由调用方通过 [modifier] 决定。
 *
 * 布局结构：内容容器（[SubcomposeLayout] + [animateContentSize]）只负责可见高度，渐变遮罩
 * 贴在容器底部，展开按钮作为 [Column] 的普通子项排在容器下方。这样容器高度动画的每一帧
 * 都会带动遮罩和按钮一起移动，收起/展开时按钮与内容同步过渡，而不是瞬移到目标位置。
 *
 * 内容只测量一次：同一个 [Measurable] 不允许在一次测量中被测量多次（普通 `LayoutNode`
 * 的 `canMultiMeasure` 为 false），否则会抛
 * `measure() may not be called multiple times on the same Measurable`。折叠态内容仍按
 * 自然高度布局，由 [clipToBounds] 裁切。
 *
 * [scrimColor] 是折叠遮罩颜色，必须与宿主容器背景一致，否则遮罩会像一块突兀的色块。
 * 默认取 [MaterialTheme.colorScheme.background]（`Scaffold` 的默认容器色），宿主若是其它
 * 容器色（例如评论页的 `surfaceContainerLow`）需显式传入。
 */
@Composable
fun ExpandableContent(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    collapsedMaxHeight: Dp,
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.background,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val collapsedMaxHeightPx = with(density) { collapsedMaxHeight.roundToPx() }
    // 是否需要按钮只能由测量结果反推：组合阶段拿不到自然高度，只能在测量里写 state；
    // 写入后下一帧才组合按钮，LazyColumn 预取会提前完成测量。
    var expandable by remember { mutableStateOf(false) }
    val showScrim = expandable && !isExpanded
    // 展开时内容高度在增长，遮罩不能瞬间消失，用淡出与高度过渡同步。
    val scrimAlpha by animateFloatAsState(
        targetValue = if (showScrim) 1f else 0f,
        animationSpec = tween(durationMillis = if (showScrim) SCRIM_FADE_IN else SCRIM_FADE_OUT),
        label = "expandableScrimAlpha",
    )
    val scrimBrush = remember(scrimColor) {
        Brush.verticalGradient(
            colorStops =
                arrayOf(
                    0f to Color.Transparent,
                    0.5f to scrimColor.copy(alpha = SCRIM_MID_ALPHA),
                    1f to scrimColor,
                ),
        )
    }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            SubcomposeLayout(
                modifier = Modifier
                    .fillMaxWidth()
                    .clipToBounds()
                    .animateContentSize(
                        animationSpec = tween(durationMillis = EXPAND_DURATION, easing = FastOutSlowInEasing),
                    ),
            ) { constraints ->
                val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
                val contentMeasurable = subcompose(CONTENT_SLOT, content).first()
                val naturalPlaceable = contentMeasurable.measure(
                    Constraints(
                        minWidth = width,
                        maxWidth = width,
                        minHeight = 0,
                        maxHeight = Constraints.Infinity,
                    ),
                )
                val isExpandable = naturalPlaceable.height > collapsedMaxHeightPx
                expandable = isExpandable
                val visibleHeight = if (isExpandable && !isExpanded) collapsedMaxHeightPx else naturalPlaceable.height
                layout(width, visibleHeight) {
                    naturalPlaceable.placeRelative(0, 0)
                }
            }

            if (expandable && scrimAlpha > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(SCRIM_HEIGHT)
                        .alpha(scrimAlpha)
                        .background(brush = scrimBrush),
                )
            }
        }

        if (expandable) {
            TextButton(
                onClick = { onExpandedChange(!isExpanded) },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp, end = 4.dp, bottom = 8.dp),
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text(if (isExpanded) "收起" else "展开")
            }
        }
    }
}

/**
 * 计算 [maxLines] 行文本在给定 [style] 下的可见高度，供 [ExpandableContent] 的折叠量使用；
 * 随系统字体缩放自动变化。
 */
@Composable
fun collapsedTextHeight(maxLines: Int, style: TextStyle): Dp {
    val density = LocalDensity.current
    return remember(density, style, maxLines) {
        with(density) {
            val lineHeight = style.lineHeight.takeOrElse {
                style.fontSize.takeOrElse { 16.sp } * 1.4f
            }
            (lineHeight * maxLines.toFloat()).toDp()
        }
    }
}

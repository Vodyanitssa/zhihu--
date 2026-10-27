package com.zhihuminus.feature.imageview

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalTouchSlopOrCancellation
import androidx.compose.foundation.gestures.verticalDrag
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 下拉拖拽退出手势容器。
 *
 * 特性：
 * - 仅在未放大状态（[enabled] 为 true）响应向下拖拽手势
 * - 拖拽过程中通过 [Modifier.graphicsLayer] 驱动垂直平移与微缩变换，避免引起 Compose 重组
 * - 对外提供实时位移进度 [onProgressChange]（0.0 ~ 1.0），用于控制背景遮罩透明度与顶部工具栏淡出
 * - 超过阈值（位移超过 100dp 或下滑速度超过 800dp/s）触发退出，伴随触觉反馈
 * - 未达阈值松手时自动通过弹性弹簧动画平滑复位
 *
 * @param enabled 是否允许下拉退出（通常在图片为原始 1.0x 比例时为 true，放大时为 false）。
 * @param onDismiss 达到退出条件时的回调。
 * @param onProgressChange 实时拖拽进度通知，入参为 0.0f（静止）到 1.0f（最大拖拽）。
 * @param modifier 外部修饰符。
 * @param content 子内容。
 */
@Composable
fun DragToDismissContainer(
    enabled: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onProgressChange: (Float) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }
    val maxDragDistancePx = with(density) { 320.dp.toPx() }
    val dismissThresholdPx = with(density) { 100.dp.toPx() }
    val dismissVelocityPx = with(density) { 800.dp.toPx() }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val velocityTracker = VelocityTracker()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    var initialOverSlop = 0f
                    val drag = awaitVerticalTouchSlopOrCancellation(down.id) { change: PointerInputChange, over: Float ->
                        // 仅向下拖拽时消费手势，向上滑动不拦截
                        if (over > 0f) {
                            change.consume()
                            initialOverSlop = over
                        }
                    }

                    if (drag != null && initialOverSlop > 0f) {
                        var currentOffset = initialOverSlop
                        dragOffsetY = currentOffset
                        onProgressChange((currentOffset / maxDragDistancePx).coerceIn(0f, 1f))

                        val dragSuccessful = verticalDrag(drag.id) { change: PointerInputChange ->
                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                            val dy = change.positionChange().y
                            change.consume()
                            currentOffset = (currentOffset + dy).coerceAtLeast(0f)
                            dragOffsetY = currentOffset
                            onProgressChange((currentOffset / maxDragDistancePx).coerceIn(0f, 1f))
                        }

                        val velocityY = velocityTracker.calculateVelocity().y
                        val shouldDismiss = dragSuccessful &&
                            (currentOffset >= dismissThresholdPx || velocityY >= dismissVelocityPx)

                        if (shouldDismiss) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                animate(
                                    initialValue = currentOffset,
                                    targetValue = screenHeightPx,
                                    initialVelocity = velocityY,
                                    animationSpec = tween(durationMillis = 150),
                                ) { value, _ ->
                                    dragOffsetY = value
                                    onProgressChange(1f)
                                }
                                onDismiss()
                            }
                        } else {
                            // 未达到退出阈值，弹性复位
                            coroutineScope.launch {
                                animate(
                                    initialValue = currentOffset,
                                    targetValue = 0f,
                                    initialVelocity = velocityY,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium,
                                    ),
                                ) { value, _ ->
                                    dragOffsetY = value
                                    onProgressChange((value / maxDragDistancePx).coerceIn(0f, 1f))
                                }
                                dragOffsetY = 0f
                                onProgressChange(0f)
                            }
                        }
                    }
                }
            }.graphicsLayer {
                val progress = (dragOffsetY / maxDragDistancePx).coerceIn(0f, 1f)
                val scale = 1f - progress * 0.22f
                translationY = dragOffsetY
                scaleX = scale
                scaleY = scale
            },
    ) {
        content()
    }
}

package com.zhihuminus.feature.post.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PostScrollToTopButton(
    scrollState: ScrollState,
    containerWidth: Dp,
    containerHeight: Dp,
    positionKey: Any?,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val buttonSize = 48.dp
    val edgeMargin = 24.dp
    val buttonSizePx = with(density) { buttonSize.toPx() }
    val edgeMarginPx = with(density) { edgeMargin.toPx() }
    val containerWidthPx = with(density) { containerWidth.toPx() }
    val containerHeightPx = with(density) { containerHeight.toPx() }
    val maxX = (containerWidthPx - buttonSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)
    val maxY = (containerHeightPx - buttonSizePx - edgeMarginPx).coerceAtLeast(edgeMarginPx)

    var offsetX by rememberSaveable(positionKey) { mutableFloatStateOf(Float.NaN) }
    var offsetY by rememberSaveable(positionKey) { mutableFloatStateOf(Float.NaN) }

    LaunchedEffect(maxX, maxY, positionKey) {
        offsetX = if (offsetX.isNaN()) maxX else offsetX.coerceIn(edgeMarginPx, maxX)
        offsetY = if (offsetY.isNaN()) maxY else offsetY.coerceIn(edgeMarginPx, maxY)
    }

    val scope = rememberCoroutineScope()

    val offsetModifier = Modifier.offset {
        IntOffset(
            (if (offsetX.isNaN()) maxX else offsetX).roundToInt(),
            (if (offsetY.isNaN()) maxY else offsetY).roundToInt(),
        )
    }
    val dragModifier = Modifier.pointerInput(maxX, maxY, edgeMarginPx) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            val currentX = if (offsetX.isNaN()) maxX else offsetX
            val currentY = if (offsetY.isNaN()) maxY else offsetY
            offsetX = (currentX + dragAmount.x).coerceIn(edgeMarginPx, maxX)
            offsetY = (currentY + dragAmount.y).coerceIn(edgeMarginPx, maxY)
        }
    }

    SmallFloatingActionButton(
        onClick = {
            scope.launch { scrollState.animateScrollTo(0) }
        },
        modifier = modifier.then(offsetModifier).then(dragModifier).size(buttonSize),
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Icon(Icons.Filled.ArrowUpward, contentDescription = "回到顶部")
    }
}

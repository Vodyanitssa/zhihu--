package com.zhihuminus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 顶栏标题从占位文案切换为真实标题所需的滚动距离。 */
private val TOP_BAR_TITLE_REVEAL_THRESHOLD: Dp = 160.dp

/**
 * 滚动感知的顶栏标题：未滚动超过 [TOP_BAR_TITLE_REVEAL_THRESHOLD] 时显示 [placeholder]，
 * 越过阈值后以淡入 + 上滑动画切换为 [title]，回滚则反向隐藏。
 * 提供懒列表与普通滚动列两种重载，供不同滚动容器复用。
 */
@Composable
fun ScrollAwareTopBarTitle(
    state: LazyListState,
    title: String,
    placeholder: String,
) {
    val thresholdPx = with(LocalDensity.current) { TOP_BAR_TITLE_REVEAL_THRESHOLD.roundToPx() }
    val visible by remember(state, thresholdPx) {
        derivedStateOf {
            state.firstVisibleItemIndex > 0 || state.firstVisibleItemScrollOffset >= thresholdPx
        }
    }
    TopBarTitleText(visible = visible, title = title, placeholder = placeholder)
}

@Composable
fun ScrollAwareTopBarTitle(
    state: ScrollState,
    title: String,
    placeholder: String,
) {
    val thresholdPx = with(LocalDensity.current) { TOP_BAR_TITLE_REVEAL_THRESHOLD.roundToPx() }
    val visible by remember(state, thresholdPx) {
        derivedStateOf { state.value >= thresholdPx }
    }
    TopBarTitleText(visible = visible, title = title, placeholder = placeholder)
}

@Composable
private fun TopBarTitleText(
    visible: Boolean,
    title: String,
    placeholder: String,
) {
    AnimatedContent(
        targetState = visible,
        transitionSpec = {
            (fadeIn() + slideInVertically(initialOffsetY = { it / 2 })) togetherWith
                (fadeOut() + slideOutVertically(targetOffsetY = { -it / 2 }))
        },
        label = "scroll_aware_top_bar_title",
    ) { showTitle ->
        Text(
            text = if (showTitle) title else placeholder,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

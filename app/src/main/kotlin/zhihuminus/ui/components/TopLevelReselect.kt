package com.zhihuminus.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.zhihuminus.navigation.TopLevelDestination
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

val LocalTopLevelReselectFlow = compositionLocalOf<Flow<TopLevelDestination>> {
    emptyFlow()
}

enum class ReselectBehavior {
    ScrollToTop,
    Refresh,
}

fun resolveReselectBehavior(
    isAtTop: Boolean,
    currentTime: Long,
    lastTapTime: Long,
    doubleTapThresholdMs: Long = 400L,
): ReselectBehavior {
    val isDoubleTap = currentTime - lastTapTime in 1..doubleTapThresholdMs
    return if (isAtTop || isDoubleTap) {
        ReselectBehavior.Refresh
    } else {
        ReselectBehavior.ScrollToTop
    }
}

/**
 * 监听顶层 Tab 重选事件（再次点击底部栏当前选中的项）。
 *
 * 交互规范：
 * 1. 若当前列表已处于顶部，或在 400ms 内发生快速双击：触发 [onRefresh]；
 * 2. 若当前列表未在顶部：启动平滑滚动回到顶部动画。
 */
@Composable
fun HandleTopLevelReselect(
    destination: TopLevelDestination,
    listState: LazyListState,
    onRefresh: () -> Unit,
) {
    val reselectFlow = LocalTopLevelReselectFlow.current
    val scope = rememberCoroutineScope()
    var scrollJob by remember { mutableStateOf<Job?>(null) }
    var lastTapTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(reselectFlow, listState) {
        reselectFlow.collect { target ->
            if (target == destination) {
                val currentTime = System.currentTimeMillis()
                val isAtTop = listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0

                val behavior = resolveReselectBehavior(
                    isAtTop = isAtTop,
                    currentTime = currentTime,
                    lastTapTime = lastTapTime,
                )
                lastTapTime = currentTime

                when (behavior) {
                    ReselectBehavior.Refresh -> {
                        scrollJob?.cancel()
                        onRefresh()
                    }
                    ReselectBehavior.ScrollToTop -> {
                        scrollJob?.cancel()
                        scrollJob = scope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }
    }
}

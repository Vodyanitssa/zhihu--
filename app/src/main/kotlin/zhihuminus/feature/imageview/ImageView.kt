package com.zhihuminus.feature.imageview

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import com.zhihuminus.platform.PlatformBackHandler
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/**
 * 图片预览的业务操作回调。
 */
data class ImageViewActions(
    val onSave: (String) -> Unit = {},
    val onShare: (String) -> Unit = {},
    val onOpenInBrowser: (String) -> Unit = {},
)

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // 禁用图片预览内的震动反馈。
    }
}

/**
 * 全屏图片预览界面。
 *
 * 特性：
 * - 多张图片左右滑动浏览（HorizontalPager）
 * - 双指缩放（ZoomableAsyncImage）
 * - 缩放时锁定横向滑动
 * - 长按弹出操作菜单（保存/分享/浏览器打开）
 * - 单击关闭预览
 *
 * @param manager 图片查看管理器，提供图片列表和当前索引。
 * @param actions 业务操作回调。
 * @param onDismiss 请求关闭预览时触发。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageView(
    manager: ImageViewManager,
    actions: ImageViewActions = ImageViewActions(),
    onDismiss: () -> Unit = { manager.dismiss() },
) {
    if (!manager.isShowing) return

    val previewImages = remember(manager.images) {
        manager.images
            .filter { it.isNotBlank() && !it.startsWith("data:") }
            .distinctBy { it }
    }
    if (previewImages.isEmpty()) return

    val initialPage = manager.currentIndex.coerceIn(0, previewImages.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { previewImages.size }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 记录每张图片是否允许横向滑动翻页
    val pageSwipeEnabled = remember(previewImages) {
        mutableStateMapOf<String, Boolean>().apply {
            previewImages.forEach { put(it, true) }
        }
    }
    val currentPageSwipeEnabled = pageSwipeEnabled[previewImages[pagerState.currentPage]] ?: true

    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }

    // 竖向滑动切换页面（多张图片时）
    val verticalSwipeModifier = if (previewImages.size > 1 && currentPageSwipeEnabled) {
        Modifier.pointerInput(previewImages.size) {
            val threshold = 96f
            var totalDrag = 0f
            detectVerticalDragGestures(
                onVerticalDrag = { _, dragAmount ->
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    val targetPage = when {
                        totalDrag < -threshold -> pagerState.currentPage + 1
                        totalDrag > threshold -> pagerState.currentPage - 1
                        else -> null
                    }?.coerceIn(0, previewImages.lastIndex)

                    if (targetPage != null && targetPage != pagerState.currentPage) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(targetPage)
                        }
                    }
                    totalDrag = 0f
                },
                onDragCancel = {
                    totalDrag = 0f
                },
            )
        }
    } else {
        Modifier
    }

    // 同步 pager 状态到 manager
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }
            .collect { page ->
                manager.setCurrentIndex(page)
            }
    }

    PlatformBackHandler(manager.isShowing) {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        CompositionLocalProvider(LocalHapticFeedback provides NoopHapticFeedback) {
            HorizontalPager(
                state = pagerState,
                key = { page -> "$page:${previewImages[page]}" },
                userScrollEnabled = previewImages.size > 1 && currentPageSwipeEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .then(verticalSwipeModifier),
            ) { page ->
                ImageViewItem(
                    url = previewImages[page],
                    onClick = onDismiss,
                    onLongClick = { offset ->
                        menuOffset = offset
                        showMenu = true
                    },
                    onZoomFractionChange = { zoomFraction ->
                        pageSwipeEnabled[previewImages[page]] = zoomFraction <= 0.01f
                    },
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            offset = with(density) {
                DpOffset(
                    menuOffset.x.toDp(),
                    menuOffset.y.toDp(),
                )
            },
        ) {
            val currentUrl = previewImages[pagerState.currentPage]
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    showMenu = false
                    actions.onSave(currentUrl)
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    showMenu = false
                    actions.onShare(currentUrl)
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    showMenu = false
                    actions.onOpenInBrowser(currentUrl)
                },
            )
        }
    }
}

/**
 * 单个可缩放图片项。
 */
@Composable
private fun ImageViewItem(
    url: String,
    onClick: () -> Unit,
    onLongClick: (Offset) -> Unit,
    onZoomFractionChange: (Float) -> Unit,
) {
    val imageState = rememberZoomableImageState(rememberZoomableState())
    LaunchedEffect(imageState) {
        snapshotFlow { imageState.zoomableState.zoomFraction }
            .collect { zoomFraction ->
                onZoomFractionChange(zoomFraction ?: 0f)
            }
    }
    ZoomableAsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        state = imageState,
        onClick = { onClick() },
        onLongClick = onLongClick,
    )
}

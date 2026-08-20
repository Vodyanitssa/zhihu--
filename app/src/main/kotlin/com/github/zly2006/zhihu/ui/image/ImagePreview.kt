/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.image

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
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/**
 * 图片预览的数据模型，不依赖任何 AST 或业务层。
 *
 * @param url 图片的原始 URL。
 * @param width 图片原始宽度（像素），可选，用于缩放计算等场景。
 * @param height 图片原始高度（像素），可选。
 */
data class ImagePreview(
    val url: String,
    val width: Int = 0,
    val height: Int = 0,
) {
    companion object {
        fun fromUrl(url: String) = ImagePreview(url = url)
    }
}

/**
 * 图片预览的业务操作回调。
 *
 * 预览组件本身不执行任何 Android 系统操作（MediaStore、Intent 等），
 * 而是通过这些回调将“用户请求保存/分享/在浏览器中打开”的意图传递给调用方。
 * 所有回调的参数都是图片的 URL 字符串，调用方可以根据需要自行处理。
 */
data class ImagePreviewActions(
    val onSave: (String) -> Unit = {},
    val onShare: (String) -> Unit = {},
    val onOpenInBrowser: (String) -> Unit = {},
)

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // 无操作，禁用图片预览内的震动反馈，保持交互手感稳定。
    }
}

/**
 * 全屏图片预览界面。
 *
 * 特性：
 * - 多张图片左右滑动浏览（HorizontalPager）
 * - 双指缩放 / 捏合手势（Telephoto ZoomableAsyncImage）
 * - 缩放时锁定横向滑动，避免手势冲突
 * - 长按弹出操作菜单（保存/分享/浏览器打开），菜单位置跟随触点
 * - 单击关闭预览
 * - 单张图片时支持上下滑切换页面
 *
 * @param images 待预览的图片列表。
 * @param initialIndex 初始显示的图片索引。
 * @param actions 业务操作回调，由调用方注入。
 * @param onDismiss 请求关闭预览时触发。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImagePreview(
    images: List<ImagePreview>,
    initialIndex: Int = 0,
    actions: ImagePreviewActions = ImagePreviewActions(),
    onDismiss: () -> Unit,
) {
    val previewImages = remember(images) {
        images
            .filter { it.url.isNotBlank() && !it.url.startsWith("data:") }
            .distinctBy { it.url }
            .ifEmpty { listOf(ImagePreview("")) }
    }
    val initialPage = initialIndex.coerceIn(0, previewImages.lastIndex)

    val pagerState = rememberPagerState(initialPage = initialPage) { previewImages.size }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // 记录每张图片是否允许横向滑动翻页。
    // 当图片被缩放后，禁止横向滑动，防止与横向翻页手势冲突。
    val pageSwipeEnabled = remember(previewImages) {
        mutableStateMapOf<String, Boolean>().apply {
            previewImages.forEach { put(it.url, true) }
        }
    }
    val currentPageSwipeEnabled = pageSwipeEnabled[previewImages[pagerState.currentPage].url] ?: true

    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }

    // 单张以上的图片时，允许通过竖向滑动切换页面。
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        // 禁用图片查看器自带的震动反馈，保持长按菜单手感稳定。
        CompositionLocalProvider(LocalHapticFeedback provides NoopHapticFeedback) {
            HorizontalPager(
                state = pagerState,
                key = { page -> "$page:${previewImages[page].url}" },
                userScrollEnabled = previewImages.size > 1 && currentPageSwipeEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .then(verticalSwipeModifier),
            ) { page ->
                PreviewImageItem(
                    image = previewImages[page],
                    onClick = onDismiss,
                    onLongClick = { offset ->
                        menuOffset = offset
                        showMenu = true
                    },
                    onZoomFractionChange = { zoomFraction ->
                        pageSwipeEnabled[previewImages[page].url] = zoomFraction <= 0.01f
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
            val currentPageUrl = previewImages[pagerState.currentPage].url
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    showMenu = false
                    actions.onSave(currentPageUrl)
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    showMenu = false
                    actions.onShare(currentPageUrl)
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    showMenu = false
                    actions.onOpenInBrowser(currentPageUrl)
                },
            )
        }
    }
}

/**
 * 单个可缩放图片项。
 *
 * 使用 Telephoto 的 ZoomableAsyncImage 实现双指缩放；
 * 通过 snapshotFlow 监听缩放比，在缩放时通知父组件禁用横向滑动。
 */
@Composable
private fun PreviewImageItem(
    image: ImagePreview,
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
        model = image.url,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        state = imageState,
        onClick = { onClick() },
        onLongClick = onLongClick,
    )
}

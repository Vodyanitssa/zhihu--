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

package com.zhihuminus.ui.components

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import com.zhihuminus.platform.rememberImageSaver
import com.zhihuminus.platform.rememberImageSharer
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenImagePreviewContent(
    urls: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onOpenInBrowser: (String) -> Unit,
    imageContent: @Composable (
        url: String,
        onClick: () -> Unit,
        onLongClick: (Offset) -> Unit,
        onPageSwipeEnabledChange: (Boolean) -> Unit,
    ) -> Unit,
) {
    val onSaveImage = rememberImageSaver()
    val onShareImage = rememberImageSharer()
    var showMenu by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val imageUrls = remember(urls) {
        urls
            .filter { it.isNotBlank() && !it.startsWith("data") }
            .distinct()
            .ifEmpty { listOf("") }
    }
    val initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { imageUrls.size }
    val pageSwipeEnabled = remember(imageUrls) {
        mutableStateMapOf<String, Boolean>().apply {
            imageUrls.forEach { put(it, true) }
        }
    }
    val currentPageSwipeEnabled = pageSwipeEnabled[imageUrls[pagerState.currentPage]] ?: true
    val verticalSwipeModifier = if (imageUrls.size > 1 && currentPageSwipeEnabled) {
        Modifier.pointerInput(imageUrls.size) {
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
                    }?.coerceIn(0, imageUrls.lastIndex)

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
                key = { page -> "$page:${imageUrls[page]}" },
                userScrollEnabled = imageUrls.size > 1 && currentPageSwipeEnabled,
                modifier = Modifier
                    .fillMaxSize()
                    .then(verticalSwipeModifier),
            ) { page ->
                imageContent(
                    imageUrls[page],
                    onDismiss,
                    { offset ->
                        menuOffset = offset
                        showMenu = true
                    },
                    { enabled ->
                        pageSwipeEnabled[imageUrls[page]] = enabled
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
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    showMenu = false
                    onSaveImage(imageUrls[pagerState.currentPage])
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    showMenu = false
                    onShareImage(imageUrls[pagerState.currentPage])
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    showMenu = false
                    onOpenInBrowser(imageUrls[pagerState.currentPage])
                },
            )
        }
    }
}

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // 无操作。
    }
}

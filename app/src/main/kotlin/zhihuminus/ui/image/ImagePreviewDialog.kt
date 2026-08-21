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

package com.zhihuminus.ui.image

import android.content.Context
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable

/**
 * 图片预览的对话框形式。
 *
 * 适用于需要从 View / XML / WebView 等非 Compose 上下文中启动图片预览的场景。
 * 内部通过 ComposeView 托管 [ImagePreview] 可组合函数，
 * 业务操作通过 [ImagePreviewActions] 回调传递，保持预览 UI 与业务逻辑解耦。
 *
 * 使用示例：
 * ```kotlin
 * ImagePreviewDialog(
 *     context = context,
 *     images = listOf(ImagePreview(url = "https://example.com/image.jpg")),
 *     actions = ImagePreviewActions(
 *         onSave = { url -> saveImage(url) },
 *         onOpenInBrowser = { url -> openBrowser(url) },
 *     ),
 * ).show()
 * ```
 */
class ImagePreviewDialog(
    context: Context,
    images: List<ImagePreview>,
    initialIndex: Int = 0,
    actions: ImagePreviewActions = ImagePreviewActions(),
) : ComponentDialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    ImagePreview(
                        images = images,
                        initialIndex = initialIndex,
                        actions = actions,
                        onDismiss = { dismiss() },
                    )
                }
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        window?.setBackgroundDrawable(BLACK.toDrawable())
    }
}

/**
 * 记住一个图片预览打开器。
 *
 * 返回一个函数，调用时会创建并显示 [ImagePreviewDialog]。
 * 适用于从 Compose 界面中以对话框形式启动图片预览。
 *
 * @param actions 图片操作回调，由调用方注入具体的保存/分享/浏览器打开逻辑。
 */
@Composable
fun rememberImagePreviewOpener(
    actions: ImagePreviewActions = ImagePreviewActions(),
): (List<ImagePreview>, Int) -> Unit {
    val context = LocalContext.current
    val stableActions = remember(actions) { actions }
    return remember(context, stableActions) {
        { images, initialIndex ->
            ImagePreviewDialog(context, images, initialIndex, stableActions).show()
        }
    }
}

/**
 * 记住一个单张图片预览打开器。
 *
 * 便利函数，内部代理到 [rememberImagePreviewOpener]，
 * 适用于只需要预览单张图片的场景。
 */
@Composable
fun rememberSingleImagePreviewOpener(
    actions: ImagePreviewActions = ImagePreviewActions(),
): (String) -> Unit {
    val openGallery = rememberImagePreviewOpener(actions)
    return remember(openGallery) {
        { url -> openGallery(listOf(ImagePreview(url)), 0) }
    }
}

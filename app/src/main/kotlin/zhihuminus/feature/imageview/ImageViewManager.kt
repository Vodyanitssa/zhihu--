package com.zhihuminus.feature.imageview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

/**
 * 图片查看管理器。
 *
 * 负责在 screen 遍历 ContentNodes 收集图片 URL 后维护图片列表，
 * 并在 renderer 层的 Image 被点击时根据 URL 查找索引并展示大图预览。
 *
 * 使用方式：
 * 1. Screen 层创建实例并通过 [submitImages] 提交图片 URL 列表
 * 2. Screen 层通过 [CompositionLocalProvider] 将其提供给 renderer 层
 * 3. Renderer 层的 Image composable 在点击时调用 [show]
 * 4. Screen 层根据 [isShowing] 状态渲染 [ImageView] 全屏预览
 */
open class ImageViewManager {
    private val _images = mutableStateListOf<String>()
    val images: List<String> get() = _images

    private var _currentIndex by mutableIntStateOf(-1)
    val currentIndex: Int get() = _currentIndex

    val isShowing: Boolean get() = currentIndex >= 0

    /**
     * 提交图片 URL 列表。
     *
     * Screen 层在 contentNodes 变化时调用，从 AST 中提取所有图片 URL。
     */
    fun submitImages(urls: List<String>) {
        _images.clear()
        _images.addAll(urls)
    }

    /**
     * 展示指定 URL 的图片预览。
     *
     * Renderer 层的 Image composable 在点击时调用。
     * 如果 URL 不在已提交的列表中，则忽略。
     */
    open fun show(url: String) {
        val index = _images.indexOf(url)
        if (index >= 0) {
            _currentIndex = index
        }
    }

    /**
     * 关闭图片预览。
     */
    fun dismiss() {
        _currentIndex = -1
    }

    /**
     * 切换到指定索引的图片。
     *
     * 由 [ImageView] 内部的翻页手势调用。
     */
    fun setCurrentIndex(index: Int) {
        _currentIndex = index.coerceIn(-1, _images.lastIndex)
    }
}

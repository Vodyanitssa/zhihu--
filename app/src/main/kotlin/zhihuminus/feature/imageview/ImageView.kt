package com.zhihuminus.feature.imageview

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
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

/**
 * 全屏图片沉浸式预览界面。
 *
 * 特性：
 * - 左右滑动流畅翻页（HorizontalPager）
 * - 支持双指缩放与双击缩放（ZoomableAsyncImage），缩放边界与外层滑动无缝协调
 * - 仅在 1.0x 原始比例下支持下拉拖拽退出（Drag-to-Dismiss），背景遮罩透明度及图片微缩实时线性跟随
 * - 页面滑离后自动复位图片缩放状态
 * - 顶部居中多图页码指示器（1 / N）
 * - 状态栏图标自动切换为白色高对比度，退出自动还原
 * - 长按轻微震动并弹出 Material 3 规范的底部操作面板（保存/分享/外链）
 * - 单击图片或系统返回键关闭预览
 *
 * @param manager 图片查看管理器，提供图片列表和当前索引。
 * @param actions 业务操作回调。
 * @param onDismiss 请求关闭预览时触发。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageView(
    manager: ImageViewManager,
    actions: ImageViewActions = ImageViewActions(),
    onDismiss: () -> Unit = { manager.dismiss() },
) {
    AnimatedVisibility(
        visible = manager.isShowing,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)),
    ) {
        val previewImages = remember(manager.images) {
            manager.images
                .filter { it.isNotBlank() && !it.startsWith("data:") }
                .distinct()
        }
        if (previewImages.isEmpty()) return@AnimatedVisibility

        val initialPage = manager.currentIndex.coerceIn(0, previewImages.lastIndex)
        val pagerState = rememberPagerState(initialPage = initialPage) { previewImages.size }
        val haptic = LocalHapticFeedback.current
        val view = LocalView.current

        var isCurrentPageZoomed by remember { mutableStateOf(false) }
        var dismissProgress by remember { mutableFloatStateOf(0f) }
        var showMenu by remember { mutableStateOf(false) }

        // 接管系统栏状态，确保黑色全屏背景下状态栏图标高对比度可见（白色），退出时自动复原
        DisposableEffect(view) {
            val window = view.context.findActivity()?.window
            val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
            val originalLightStatusBars = insetsController?.isAppearanceLightStatusBars ?: true
            val originalLightNavBars = insetsController?.isAppearanceLightNavigationBars ?: true

            insetsController?.isAppearanceLightStatusBars = false
            insetsController?.isAppearanceLightNavigationBars = false

            onDispose {
                insetsController?.isAppearanceLightStatusBars = originalLightStatusBars
                insetsController?.isAppearanceLightNavigationBars = originalLightNavBars
            }
        }

        // 同步 pager 当前索引到 manager
        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .collect { page ->
                    manager.setCurrentIndex(page)
                }
        }

        BackHandler(manager.isShowing) {
            onDismiss()
        }

        val scrimAlpha = (1f - dismissProgress * 0.85f).coerceIn(0.15f, 1f)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha)),
        ) {
            DragToDismissContainer(
                enabled = !isCurrentPageZoomed,
                onDismiss = onDismiss,
                onProgressChange = { progress -> dismissProgress = progress },
                modifier = Modifier.fillMaxSize(),
            ) {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> "$page:${previewImages[page]}" },
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    ImageViewItem(
                        url = previewImages[page],
                        isCurrentPage = page == pagerState.currentPage,
                        onClick = onDismiss,
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showMenu = true
                        },
                        onZoomChange = { zoomed ->
                            if (page == pagerState.currentPage) {
                                isCurrentPageZoomed = zoomed
                            }
                        },
                    )
                }
            }

            // 多图页码指示器（下拉拖拽时平滑淡出）
            if (previewImages.size > 1 && dismissProgress < 0.1f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.55f), shape = CircleShape)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${previewImages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }

            // 长按底部操作面板
            if (showMenu) {
                val currentUrl = previewImages.getOrNull(pagerState.currentPage)
                if (currentUrl != null) {
                    ModalBottomSheet(
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
                        dragHandle = { BottomSheetDefaults.DragHandle() },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp),
                        ) {
                            MenuActionButton(
                                icon = Icons.Default.Download,
                                text = "保存图片",
                                onClick = {
                                    showMenu = false
                                    actions.onSave(currentUrl)
                                },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuActionButton(
                                icon = Icons.Default.Share,
                                text = "分享图片",
                                onClick = {
                                    showMenu = false
                                    actions.onShare(currentUrl)
                                },
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            MenuActionButton(
                                icon = Icons.Default.OpenInBrowser,
                                text = "在浏览器中打开",
                                onClick = {
                                    showMenu = false
                                    actions.onOpenInBrowser(currentUrl)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 单个可缩放图片项。
 */
@Composable
private fun ImageViewItem(
    url: String,
    isCurrentPage: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onZoomChange: (Boolean) -> Unit,
) {
    val imageState = rememberZoomableImageState(rememberZoomableState())

    val isZoomed by remember {
        derivedStateOf { (imageState.zoomableState.zoomFraction ?: 0f) > 0.001f }
    }

    LaunchedEffect(isZoomed) {
        onZoomChange(isZoomed)
    }

    // 滑离当前页面时，将上一页的缩放状态平滑复位，避免回滑时出现缩放死锁或视觉异常
    if (!isCurrentPage) {
        LaunchedEffect(Unit) {
            imageState.zoomableState.resetZoom(animationSpec = SnapSpec())
        }
    }

    ZoomableAsyncImage(
        model = url,
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        state = imageState,
        onClick = { onClick() },
        onLongClick = { onLongClick() },
    )
}

@Composable
private fun MenuActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor,
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

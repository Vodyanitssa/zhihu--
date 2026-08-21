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

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.Article
import com.zhihuminus.navigation.ArticleType
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Pin
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Topic
import com.zhihuminus.platform.SettingsStore
import com.zhihuminus.platform.UserMessageSink
import com.zhihuminus.platform.androidUserMessageSink
import com.zhihuminus.ui.articleHost
import com.zhihuminus.util.clipboardManager
import com.zhihuminus.util.luoTianYiUrlLauncher
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

/**
 * 通用分享弹窗内容。
 *
 * 弹窗采用底部滑入的操作面板，提供系统分享、复制链接和跳转分享设置三个动作。它只负责视觉和点击分发，
 * 具体分享、复制和设置导航由调用方注入，方便文章、问题、想法等内容复用同一套交互。
 */
@Composable
fun ShareDialogContent(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    onShareClick: () -> Unit,
    onCopyClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onDismissRequest() },
        ) {
            AnimatedVisibility(
                visible = showDialog,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(300, easing = EaseOutCubic),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(300, easing = EaseInCubic),
                ),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = false) { /* 阻止点击穿透 */ },
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        RoundedCornerShape(2.dp),
                                    ),
                            )
                        }

                        MenuActionButton(
                            icon = Icons.Filled.Share,
                            text = "分享",
                            onClick = onShareClick,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuActionButton(
                            icon = Icons.Filled.ContentCopy,
                            text = "复制链接",
                            onClick = onCopyClick,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        MenuActionButton(
                            icon = Icons.Filled.Settings,
                            text = "分享设置",
                            onClick = onSettingsClick,
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f),
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
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.5f),
            )
        }
    }
}

enum class ShareAction {
    Share,
    DirectShare,
    CopyLink,
}

typealias ShareActionExecutor = (ShareAction, NavDestination, String) -> Unit

internal fun clipboardShareActionExecutor(
    copyPlainText: (label: String, text: String) -> Unit,
    userMessages: UserMessageSink,
): ShareActionExecutor = { action, _, shareText ->
    if (action == ShareAction.CopyLink) {
        copyPlainText("Link", shareText)
        userMessages.showMessage("已复制链接")
    } else {
        copyPlainText("Share", shareText)
        userMessages.showMessage("已复制分享文本")
    }
}

@Composable
fun ShareDialog(
    content: NavDestination,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val executeShareAction = rememberShareActionExecutor()

    ShareDialogContent(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        onShareClick = {
            onDismissRequest()
            executeShareAction(ShareAction.Share, content, shareText)
        },
        onCopyClick = {
            onDismissRequest()
            executeShareAction(ShareAction.CopyLink, content, shareText)
        },
        onSettingsClick = {
            onDismissRequest()
            navigator.onNavigate(Account.AppearanceSettings(setting = "shareAction"))
        },
    )
}

/**
 * 根据用户的分享偏好执行默认分享动作。
 *
 * `shareActionMode=ask` 时打开弹窗，`copy` 时直接复制，`share` 时直接调用系统分享。新增分享入口时优先走这里，
 * 避免不同页面对同一个设置项解释不一致。
 */
fun handleShareAction(
    content: NavDestination,
    settings: SettingsStore,
    executeShareAction: ShareActionExecutor,
    onShowDialog: () -> Unit,
) {
    val shareText = getShareText(content) ?: return
    when (settings.getString("shareActionMode", "ask")) {
        "copy" -> executeShareAction(ShareAction.CopyLink, content, shareText)
        "share" -> executeShareAction(ShareAction.DirectShare, content, shareText)
        else -> onShowDialog()
    }
}

fun getShareText(content: NavDestination, title: String = "", authorName: String = ""): String? = when (content) {
    is Article -> {
        when (content.type) {
            ArticleType.Answer -> {
                "https://www.zhihu.com/answer/${content.id}\n【$title - $authorName 的回答】"
            }

            ArticleType.Article -> {
                "https://zhuanlan.zhihu.com/p/${content.id}\n【$title - $authorName 的文章】"
            }
        }
    }

    is Question -> {
        "https://www.zhihu.com/question/${content.questionId}\n【${content.title}】"
    }

    is Pin -> {
        "https://www.zhihu.com/pin/${content.id}"
    }

    is Topic -> {
        "https://www.zhihu.com/topic/${content.id}\n【${content.name.ifBlank { title.ifBlank { "知乎话题" } }}】"
    }

    else -> null
}

fun getShareTitle(content: NavDestination): String = when (content) {
    is Article -> content.title + when (content.type) {
        ArticleType.Answer -> " - ${content.authorName} 的回答"
        ArticleType.Article -> " - ${content.authorName} 的文章"
    }

    is Question -> content.title
    is Topic -> content.name.ifBlank { "知乎话题" }
    else -> "分享内容"
}

class OpenImageDialog(
    context: Context,
    urls: List<String>,
    initialIndex: Int = 0,
) : ComponentDialog(context) {
    constructor(
        context: Context,
        url: String,
    ) : this(context, listOf(url), 0)

    private val imageUrls = urls
        .filter { it.isNotBlank() && !it.startsWith("data") }
        .distinct()
        .ifEmpty { listOf("") }
    private val initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex)

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    OpenImagePreviewContent(
                        urls = imageUrls,
                        initialIndex = initialPage,
                        onDismiss = { dismiss() },
                        onOpenInBrowser = { imageUrl ->
                            luoTianYiUrlLauncher(context, imageUrl.toUri())
                        },
                    ) { imageUrl, onClick, onLongClick, onPageSwipeEnabledChange ->
                        val imageState = rememberZoomableImageState(rememberZoomableState())
                        LaunchedEffect(imageState) {
                            snapshotFlow { imageState.zoomableState.zoomFraction }
                                .collect { zoomFraction ->
                                    onPageSwipeEnabledChange((zoomFraction ?: 0f) <= 0.01f)
                                }
                        }
                        ZoomableAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            state = imageState,
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                    }
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

@Composable
fun rememberShareActionExecutor(): ShareActionExecutor {
    val context = LocalContext.current
    return remember(context) {
        { action, content, shareText ->
            if (action == ShareAction.CopyLink) {
                context.articleHost()?.clipboardDestination = content
                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                androidUserMessageSink(context).showShortMessage("已复制链接")
            } else {
                val shareIntent = Intent().apply {
                    this.action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    if (action == ShareAction.DirectShare) {
                        putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
                    }
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "分享到").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}

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

package com.github.zly2006.zhihu.ui.article

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.articleActionText
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.ShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareActionExecutor
import com.github.zly2006.zhihu.util.Log
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.materialkolor.ktx.harmonize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val VoteUpNeutralContent = Color(0xFF3671EE)
private val VoteUpNeutralContentDark = Color(0xFF628DF7)

@Composable
internal fun voteUpNeutralContent() = if (ThemeManager.isDarkTheme()) VoteUpNeutralContentDark else VoteUpNeutralContent

@Composable
internal fun voteUpNeutralContentDuo3() = if (ThemeManager.isDarkTheme()) {
    VoteUpNeutralContentDark.harmonize(MaterialTheme.colorScheme.primary)
} else {
    VoteUpNeutralContent.harmonize(MaterialTheme.colorScheme.primary)
}

@Composable
internal fun voteUpActiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = voteUpNeutralContent(),
    contentColor = Color.White,
)

@Composable
internal fun voteUpNeutralButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleActionsMenu(
    article: Article,
    viewModel: ArticleViewModel,
    answerQueueFallbackProvider: (suspend (limit: Int) -> List<Article>)? = null,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onExportRequest: () -> Unit,
) {
    val executeShareAction = rememberShareActionExecutor()
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    @Composable
    fun MenuActionButton(
        icon: @Composable () -> Unit,
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
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(24.dp)) {
                    icon()
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

    @Composable
    fun MenuActionButton(
        icon: ImageVector,
        text: String,
        enabled: Boolean = true,
        backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick: () -> Unit,
    ) {
        MenuActionButton(
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                )
            },
            text = text,
            enabled = enabled,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            onClick = onClick,
        )
    }

    @Composable
    fun Content() {
        MenuActionButton(
            icon = Icons.Filled.Share,
            text = "分享",
            onClick = {
                onDismissRequest()
                executeShareAction(
                    ShareAction.Share,
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        MenuActionButton(
            icon = Icons.Filled.ContentCopy,
            text = "复制链接",
            onClick = {
                onDismissRequest()
                executeShareAction(
                    ShareAction.CopyLink,
                    article,
                    articleActionText(article, viewModel.questionId, viewModel.title, viewModel.authorName),
                )
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        MenuActionButton(
            icon = Icons.Filled.FilterCenterFocus,
            text = "进入沉浸式",
            onClick = {
                onDismissRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        MenuActionButton(
            icon = Icons.Filled.GetApp,
            text = "导出文章 (Markdown、图片、HTML、PDF)",
            onClick = {
                onDismissRequest()
                onExportRequest()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        MenuActionButton(
            icon = Icons.Filled.Share,
            text = "分享 Markdown 正文",
            onClick = {
                onDismissRequest()
                executeShareAction(ShareAction.DirectShare, article, viewModel.convertToMarkdown())
            },
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMenu) {
        MyModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Content()
            }
        }
    }
}

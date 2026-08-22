package com.zhihuminus.feature.post.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GetApp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.post.Post
import com.zhihuminus.ui.components.MyModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostActionsMenu(
    post: Post,
    showMenu: Boolean,
    onDismissRequest: () -> Unit,
    onShare: () -> Unit,
    onCopyLink: () -> Unit,
    onExport: () -> Unit,
) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
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
                MenuActionButton(
                    icon = Icons.Filled.Share,
                    text = "分享",
                    onClick = {
                        onDismissRequest()
                        onShare()
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))
                MenuActionButton(
                    icon = Icons.Filled.ContentCopy,
                    text = "复制链接",
                    onClick = {
                        onDismissRequest()
                        onCopyLink()
                    },
                )

                Spacer(modifier = Modifier.height(12.dp))
                MenuActionButton(
                    icon = Icons.Filled.GetApp,
                    text = "导出文章 (图片、PDF)",
                    onClick = {
                        onDismissRequest()
                        onExport()
                    },
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

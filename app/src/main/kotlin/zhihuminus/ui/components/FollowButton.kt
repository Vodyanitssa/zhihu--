package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val FollowButtonHeight: Dp = 36.dp

/**
 * 统一的关注/已关注切换按钮。
 *
 * 两态均为填充样式，仅通过配色区分权重：
 * - 未关注：使用 [MaterialTheme.colorScheme.primaryContainer]；
 * - 已关注：使用 [MaterialTheme.colorScheme.surfaceVariant]。
 *
 * 按钮以「已关注」内容为宽度锚点，切换状态时布局尺寸保持不变；
 * 请求进行中显示转圈并禁止点击。
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    val containerColor =
        if (isFollowing) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    val contentColor =
        if (isFollowing) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    Button(
        onClick = onClick,
        enabled = !isLoading,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor,
                disabledContentColor = contentColor,
            ),
        modifier = modifier.height(FollowButtonHeight).semantics { selected = isFollowing },
    ) {
        Box(contentAlignment = Alignment.Center) {
            FollowLabel(
                icon = Icons.Filled.Check,
                label = "已关注",
                modifier = Modifier.alpha(0f).clearAndSetSemantics {},
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(ButtonDefaults.IconSize),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
            } else {
                FollowLabel(
                    icon = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                    label = if (isFollowing) "已关注" else "关注",
                )
            }
        }
    }
}

@Composable
private fun FollowLabel(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(label)
    }
}

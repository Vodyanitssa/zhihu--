package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 统一的关注/已关注切换按钮。
 *
 * 样式遵循用户主页 (People) 的交互规范：
 * - 未关注：具有浅色背景的填充按钮（使用 [MaterialTheme.colorScheme.primaryContainer]）；
 * - 已关注：低权重的描边按钮 [OutlinedButton]。
 */
@Composable
fun FollowButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
) {
    if (isFollowing) {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            contentPadding = contentPadding,
            modifier = modifier,
        ) {
            Text("已关注")
        }
    } else {
        Button(
            onClick = onClick,
            enabled = enabled,
            contentPadding = contentPadding,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
            modifier = modifier,
        ) {
            Text("+ 关注")
        }
    }
}

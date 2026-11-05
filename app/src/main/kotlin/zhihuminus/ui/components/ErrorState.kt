package com.zhihuminus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.error.AppError

/**
 * 整页错误态：图标 + 「加载失败」标题 + 错误文案 + 重试按钮。
 * [AppError.retryable] 为 true 且提供 [onRetry] 时显示重试按钮。
 */
@Composable
fun ErrorFullPage(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    title: String = "加载失败",
    retryLabel: String = "重新加载",
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 24.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = error.userMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (onRetry != null && error.retryable) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(retryLabel)
                }
            }
        }
    }
}

/**
 * 内联错误态：错误文案 + 可选重试按钮，用于嵌入表单、弹窗或局部区块。
 */
@Composable
fun ErrorInline(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    retryLabel: String = "重试",
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = error.userMessage,
            color = MaterialTheme.colorScheme.error,
        )
        if (onRetry != null && error.retryable) {
            TextButton(onClick = onRetry) {
                Text(retryLabel)
            }
        }
    }
}

/**
 * 分页列表页脚错误态：整行可点击重试。
 */
@Composable
fun ErrorListFooter(
    error: AppError,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clickable = onRetry != null && error.retryable
    TextButton(
        onClick = { onRetry?.invoke() },
        enabled = clickable,
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        Text(if (clickable) "${error.userMessage}，点击重试" else error.userMessage)
    }
}

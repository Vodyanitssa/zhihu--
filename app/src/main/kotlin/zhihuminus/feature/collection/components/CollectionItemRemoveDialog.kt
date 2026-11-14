package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.collection.PendingCollectionItemRemoval

@Composable
fun CollectionItemRemoveDialog(
    target: PendingCollectionItemRemoval?,
    isRemoving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (target == null) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isRemoving) {
                onDismiss()
            }
        },
        title = { Text("移出收藏夹") },
        text = {
            Column {
                Text("确认将「${target.title.ifBlank { "这条内容" }}」移出收藏夹吗？")
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = !isRemoving,
            ) {
                Text(if (isRemoving) "移出中…" else "移出")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isRemoving,
            ) {
                Text("取消")
            }
        },
    )
}

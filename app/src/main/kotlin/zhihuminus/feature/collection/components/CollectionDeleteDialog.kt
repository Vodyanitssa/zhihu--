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
import com.zhihuminus.feature.collection.Collection

@Composable
fun CollectionDeleteDialog(
    collection: Collection?,
    isDeleting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (collection == null) return

    AlertDialog(
        modifier = modifier,
        onDismissRequest = {
            if (!isDeleting) {
                onDismiss()
            }
        },
        title = { Text("删除收藏夹") },
        text = {
            Column {
                Text("删除后无法恢复，确认删除收藏夹“${collection.title}”吗？")
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
                enabled = !isDeleting,
                modifier = Modifier,
            ) {
                Text(if (isDeleting) "删除中…" else "删除")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isDeleting,
                modifier = Modifier,
            ) {
                Text("取消")
            }
        },
    )
}

package com.zhihuminus.feature.collection.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.collection.Collection

@Composable
fun CollectionCard(
    collection: Collection,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    canDelete: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = collection.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (!collection.isDefault) {
                IconButton(
                    onClick = onDeleteClick,
                    enabled = canDelete,
                    modifier = Modifier,
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "删除${collection.title}",
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

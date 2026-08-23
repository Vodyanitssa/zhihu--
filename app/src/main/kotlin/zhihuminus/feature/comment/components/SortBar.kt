package com.zhihuminus.feature.comment.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.comment.CommentSortOrder

@Composable
fun SortBar(
    sortOrder: CommentSortOrder,
    onSortChange: (CommentSortOrder) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SuggestionChip(
            label = {
                Text(
                    "最热",
                    color = if (sortOrder == CommentSortOrder.SCORE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (sortOrder == CommentSortOrder.SCORE) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                )
            },
            onClick = { onSortChange(CommentSortOrder.SCORE) },
        )
        Spacer(Modifier.width(12.dp))
        SuggestionChip(
            label = {
                Text(
                    "最新",
                    color = if (sortOrder == CommentSortOrder.TIME) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (sortOrder == CommentSortOrder.TIME) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                )
            },
            onClick = { onSortChange(CommentSortOrder.TIME) },
        )
    }
}

package com.zhihuminus.feature.people.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.util.formatCount

@Composable
fun PeopleStatItem(
    label: String,
    value: Int,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val clickModifier = if (onClick != null) {
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    } else {
        Modifier
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .then(clickModifier)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = formatCount(value.toLong()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

package com.zhihuminus.feature.people.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.data.OfficialBadge
import com.zhihuminus.feature.people.peopleDetailTitle

@Composable
fun PeopleOfficialBadgeDetails(
    badges: List<OfficialBadge>,
    modifier: Modifier = Modifier,
) {
    if (badges.isEmpty()) return
    Column(modifier = modifier) {
        badges.forEach { badge ->
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (badge.iconUrl.isNotBlank()) {
                    AsyncImage(
                        model = badge.iconUrl,
                        contentDescription = badge.description,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(18.dp),
                    )
                }
                Text(
                    text = "${badge.peopleDetailTitle}: ${badge.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

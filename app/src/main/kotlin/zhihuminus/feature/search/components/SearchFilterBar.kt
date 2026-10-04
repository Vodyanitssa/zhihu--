package com.zhihuminus.feature.search.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.search.SearchContentType
import com.zhihuminus.feature.search.SearchSortOption
import com.zhihuminus.feature.search.SearchTimeRange

@Composable
fun SearchFilterBar(
    sortOption: SearchSortOption,
    contentType: SearchContentType,
    timeRange: SearchTimeRange,
    onSortChange: (SearchSortOption) -> Unit,
    onContentTypeChange: (SearchContentType) -> Unit,
    onTimeRangeChange: (SearchTimeRange) -> Unit,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    var contentMenuOpen by remember { mutableStateOf(false) }
    var timeMenuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box {
            TextButton(onClick = { sortMenuOpen = true }) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(sortOption.label)
            }
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                SearchSortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            sortMenuOpen = false
                            onSortChange(option)
                        },
                    )
                }
            }
        }

        Box {
            TextButton(onClick = { contentMenuOpen = true }) {
                Text(contentType.label)
            }
            DropdownMenu(expanded = contentMenuOpen, onDismissRequest = { contentMenuOpen = false }) {
                SearchContentType.entries.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.label) },
                        onClick = {
                            contentMenuOpen = false
                            onContentTypeChange(type)
                        },
                    )
                }
            }
        }

        Box {
            TextButton(onClick = { timeMenuOpen = true }) {
                Text(timeRange.label)
            }
            DropdownMenu(expanded = timeMenuOpen, onDismissRequest = { timeMenuOpen = false }) {
                SearchTimeRange.entries.forEach { range ->
                    DropdownMenuItem(
                        text = { Text(range.label) },
                        onClick = {
                            timeMenuOpen = false
                            onTimeRangeChange(range)
                        },
                    )
                }
            }
        }
    }
}

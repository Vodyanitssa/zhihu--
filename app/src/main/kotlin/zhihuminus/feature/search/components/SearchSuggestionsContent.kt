package com.zhihuminus.feature.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.feature.search.HotSearchItem

@Composable
fun SearchSuggestionsContent(
    historyItems: List<String>,
    hotItems: List<HotSearchItem>,
    showSearchHistory: Boolean,
    showHotSearch: Boolean,
    isMemberSearch: Boolean,
    memberSearchName: String,
    onItemClick: (String) -> Unit,
    onRefreshHotSearch: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenHotSearchSettings: () -> Unit,
    onOpenSearchHistorySettings: () -> Unit,
) {
    val shouldShowHistory = showSearchHistory && historyItems.isNotEmpty()
    val shouldShowHotSearch = showHotSearch && hotItems.isNotEmpty()

    if (shouldShowHistory || shouldShowHotSearch) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (shouldShowHistory) {
                SearchHistoryHeader(
                    showClearAction = true,
                    onClearHistory = onClearHistory,
                    onOpenSettings = onOpenSearchHistorySettings,
                )
                Spacer(modifier = Modifier.height(8.dp))
                historyItems.forEach { query ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onItemClick(query) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(28.dp)
                                .size(18.dp),
                        )
                        Text(
                            text = query,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                        )
                    }
                }
                if (shouldShowHotSearch) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (shouldShowHotSearch) {
                var hotSearchMoreMenuExpanded by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "热搜",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onRefreshHotSearch,
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "刷新热搜",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { hotSearchMoreMenuExpanded = true },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "更多",
                                modifier = Modifier.size(18.dp),
                            )
                            DropdownMenu(
                                expanded = hotSearchMoreMenuExpanded,
                                onDismissRequest = { hotSearchMoreMenuExpanded = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("关闭热搜显示") },
                                    onClick = {
                                        hotSearchMoreMenuExpanded = false
                                        onOpenHotSearchSettings()
                                    },
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    hotItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item.query) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (index < 3) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.width(28.dp),
                            )
                            Text(
                                text = item.query,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                            )
                            if (item.hotShow.isNotEmpty()) {
                                Text(
                                    text = item.hotShow,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        if (showSearchHistory || isMemberSearch) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                if (showSearchHistory) {
                    SearchHistoryHeader(
                        showClearAction = false,
                        onClearHistory = onClearHistory,
                        onOpenSettings = onOpenSearchHistorySettings,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text(
                    text = if (isMemberSearch) {
                        "输入关键词搜索 ${memberSearchName.ifBlank { "TA" }} 的创作"
                    } else {
                        "暂无搜索历史，输入关键词搜索后会保存在这里"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            Text(
                text = "请输入搜索内容",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            )
        }
    }
}

@Composable
fun SearchHistoryHeader(
    showClearAction: Boolean,
    onClearHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var historyMoreMenuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "搜索历史",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        IconButton(
            onClick = { historyMoreMenuExpanded = true },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = "更多", modifier = Modifier.size(18.dp))
            DropdownMenu(
                expanded = historyMoreMenuExpanded,
                onDismissRequest = { historyMoreMenuExpanded = false },
            ) {
                if (showClearAction) {
                    DropdownMenuItem(
                        text = { Text("清空搜索历史") },
                        onClick = {
                            historyMoreMenuExpanded = false
                            onClearHistory()
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text("前往设置关闭搜索历史") },
                    onClick = {
                        historyMoreMenuExpanded = false
                        onOpenSettings()
                    },
                )
            }
        }
    }
}

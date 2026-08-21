/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.util.Log
import com.zhihuminus.viewmodel.filter.BlockedKeyword
import com.zhihuminus.viewmodel.filter.BlockedTopic
import com.zhihuminus.viewmodel.filter.KeywordType
import com.zhihuminus.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.launch

/**
 * 屏蔽列表管理页。
 *
 * 页面用 tab 管理关键词、用户和主题三类规则，并展示统计、添加、删除和清空操作。
 * 新增屏蔽类型时要同步数据管理、设置页入口和 Feed 卡片菜单。
 */
@Composable
fun BlocklistSettingsScreen() {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val database = remember { getContentFilterDatabase() }
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("屏蔽关键词", "屏蔽主题")

    var loadedBlockedKeywords by remember { mutableStateOf<List<BlockedKeyword>>(emptyList()) }
    var loadedBlockedTopics by remember { mutableStateOf<List<BlockedTopic>>(emptyList()) }

    val blockedKeywords = loadedBlockedKeywords
    val blockedTopics = loadedBlockedTopics

    var showAddKeywordDialog by remember { mutableStateOf(false) }
    var showAddTopicDialog by remember { mutableStateOf(false) }

    // 加载数据
    fun loadData() {
        coroutineScope.launch {
            try {
                // 只获取精确匹配的关键词
                loadedBlockedKeywords = database
                    .blockedKeywordDao()
                    .getAllKeywords()
                    .filter { it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH }
                loadedBlockedTopics = database.blockedTopicDao().getAllTopics()
            } catch (e: Exception) {
                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                userMessages.showShortMessage("加载数据失败: ${e.message}")
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            // 只在关键词和主题屏蔽标签页显示添加按钮
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    modifier = Modifier,
                    onClick = {
                        when (selectedTab) {
                            0 -> showAddKeywordDialog = true
                            1 -> showAddTopicDialog = true
                        }
                    },
                ) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
            }
        },
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .fillMaxWidth(),
        ) {
            // 标签页
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier,
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        modifier = Modifier,
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }

            // 内容区域
            when (selectedTab) {
                0 -> BlockedKeywordsList(
                    keywords = blockedKeywords,
                    onDeleteKeyword = { keyword ->

                        coroutineScope.launch {
                            try {
                                database.blockedKeywordDao().deleteKeywordById(keyword.id)
                                userMessages.showShortMessage("已删除关键词")
                                loadData()
                            } catch (e: Exception) {
                                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                userMessages.showShortMessage("删除失败: ${e.message}")
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                database.blockedKeywordDao().clearAllKeywords()
                                userMessages.showShortMessage("已清空所有关键词")
                                loadData()
                            } catch (e: Exception) {
                                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                userMessages.showShortMessage("清空失败: ${e.message}")
                            }
                        }
                    },
                )

                1 -> BlockedTopicsList(
                    topics = blockedTopics,
                    onDeleteTopic = { topic ->

                        coroutineScope.launch {
                            try {
                                database.blockedTopicDao().deleteTopicById(topic.topicId)
                                userMessages.showShortMessage("已删除主题")
                                loadData()
                            } catch (e: Exception) {
                                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                userMessages.showShortMessage("删除失败: ${e.message}")
                            }
                        }
                    },
                    onClearAll = {
                        coroutineScope.launch {
                            try {
                                database.blockedTopicDao().clearAllTopics()
                                userMessages.showShortMessage("已清空所有主题")
                                loadData()
                            } catch (e: Exception) {
                                Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                                userMessages.showShortMessage("清空失败: ${e.message}")
                            }
                        }
                    },
                )
            }
        }
    }

    // 添加关键词对话框
    if (showAddKeywordDialog) {
        AddKeywordDialog(
            onDismiss = { showAddKeywordDialog = false },
            onConfirm = { keyword, caseSensitive, isRegex ->

                coroutineScope.launch {
                    try {
                        database.blockedKeywordDao().insertKeyword(
                            BlockedKeyword(
                                keyword = keyword.trim(),
                                keywordType = KeywordType.EXACT_MATCH.name,
                                caseSensitive = caseSensitive,
                                isRegex = isRegex,
                            ),
                        )
                        userMessages.showShortMessage("已添加关键词")
                        loadData()
                        showAddKeywordDialog = false
                    } catch (e: Exception) {
                        Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                        userMessages.showShortMessage("添加失败: ${e.message}")
                    }
                }
            },
        )
    }

    // 添加主题对话框
    if (showAddTopicDialog) {
        AddTopicDialog(
            onDismiss = { showAddTopicDialog = false },
            onConfirm = { topicId, topicName ->
                coroutineScope.launch {
                    try {
                        database.blockedTopicDao().insertTopic(BlockedTopic(topicId = topicId, topicName = topicName))
                        userMessages.showShortMessage("已添加主题")
                        loadData()
                        showAddTopicDialog = false
                    } catch (e: Exception) {
                        Log.e("BlocklistSettingsScreen", "Blocklist settings action failed", e)
                        userMessages.showShortMessage("添加失败: ${e.message}")
                    }
                }
            },
        )
    }
}

@Composable
fun BlockedKeywordsList(
    keywords: List<BlockedKeyword>,
    onDeleteKeyword: (BlockedKeyword) -> Unit,
    onClearAll: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (keywords.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    modifier = Modifier,
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空全部")
                }
            }
        }

        if (keywords.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无精确匹配关键词",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加传统关键词屏蔽",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier,
            ) {
                items(keywords, key = { it.id }) { keyword ->
                    ListItem(
                        modifier = Modifier,
                        headlineContent = { Text(keyword.keyword) },
                        supportingContent = {
                            val options = mutableListOf<String>()
                            if (keyword.caseSensitive) options.add("区分大小写")
                            if (keyword.isRegex) options.add("正则表达式")
                            if (options.isNotEmpty()) {
                                Text(options.joinToString(" · "))
                            } else {
                                Text("精确匹配")
                            }
                        },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier,
                                onClick = { onDeleteKeyword(keyword) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun AddKeywordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit,
) {
    var keyword by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var isRegex by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽关键词") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text("关键词") },
                    placeholder = { Text("输入要屏蔽的关键词") },
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        modifier = Modifier,
                        checked = caseSensitive,
                        onCheckedChange = { caseSensitive = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("区分大小写")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Checkbox(
                        modifier = Modifier,
                        checked = isRegex,
                        onCheckedChange = { isRegex = it },
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("正则表达式")
                }
                if (isRegex) {
                    Text(
                        "提示：使用正则表达式可以实现更灵活的匹配，但语法错误会导致该关键词无效",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier,
                onClick = {
                    if (keyword.isNotBlank()) {
                        onConfirm(keyword, caseSensitive, isRegex)
                    }
                },
                enabled = keyword.isNotBlank(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier,
                onClick = onDismiss,
            ) {
                Text("取消")
            }
        },
    )
}

@Composable
fun BlockedTopicsList(
    topics: List<BlockedTopic>,
    onDeleteTopic: (BlockedTopic) -> Unit,
    onClearAll: () -> Unit,
) {
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (topics.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "暂无屏蔽主题",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "点击右下角的 + 按钮添加要屏蔽的主题",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            // 清空按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    modifier = Modifier,
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Text("清空全部")
                }
            }

            LazyColumn(
                modifier = Modifier,
            ) {
                items(topics, key = { it.topicId }) { topic ->
                    ListItem(
                        modifier = Modifier,
                        headlineContent = { Text(topic.topicName) },
                        supportingContent = { Text("ID: ${topic.topicId}") },
                        trailingContent = {
                            IconButton(
                                modifier = Modifier,
                                onClick = { onDeleteTopic(topic) },
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
            }
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("确认清空") },
            text = { Text("确定要清空所有屏蔽主题吗？此操作不可撤销。") },
            confirmButton = {
                Button(
                    modifier = Modifier,
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("清空")
                }
            },
            dismissButton = {
                TextButton(
                    modifier = Modifier,
                    onClick = { showClearConfirmDialog = false },
                ) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
fun AddTopicDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var topicId by remember { mutableStateOf("") }
    var topicName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加屏蔽主题") },
        text = {
            Column {
                Text(
                    "输入要屏蔽的主题ID和名称。主题ID可以从知乎网页版主题链接中获取。",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = topicId,
                    onValueChange = { topicId = it },
                    label = { Text("主题ID") },
                    placeholder = { Text("例如: 19550517") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = topicName,
                    onValueChange = { topicName = it },
                    label = { Text("主题名称") },
                    placeholder = { Text("例如: 娱乐") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                modifier = Modifier,
                onClick = {
                    if (topicId.isNotBlank()) {
                        onConfirm(topicId, topicName.ifBlank { topicId })
                    }
                },
                enabled = topicId.isNotBlank(),
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier,
                onClick = onDismiss,
            ) {
                Text("取消")
            }
        },
    )
}

/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.zhihuminus.ui.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.notification.NotificationType
import com.zhihuminus.ui.AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup

private data class SettingsSearchEntry(
    val id: String,
    val title: String,
    val section: String,
    val description: String,
    val destination: NavDestination,
    val keywords: List<String> = emptyList(),
) {
    fun matches(query: String): Boolean {
        val normalizedTerms = query
            .trim()
            .lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        if (normalizedTerms.isEmpty()) return true
        val searchableText = buildString {
            append(title)
            append('\n')
            append(section)
            append('\n')
            append(description)
            if (keywords.isNotEmpty()) {
                append('\n')
                append(keywords.joinToString(" "))
            }
        }.lowercase()
        return normalizedTerms.all(searchableText::contains)
    }
}

private fun appearanceEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "外观与阅读体验",
    description = description,
    destination = Account.AppearanceSettings(setting = settingKey),
    keywords = keywords,
)

private fun systemEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "系统与更新",
    description = description,
    destination = Account.SystemAndUpdateSettings(setting = settingKey),
    keywords = keywords,
)

private fun notificationEntry(
    id: String,
    title: String,
    description: String,
    settingKey: String,
    keywords: List<String> = emptyList(),
): SettingsSearchEntry = SettingsSearchEntry(
    id = id,
    title = title,
    section = "通知设置",
    description = description,
    destination = Notification.NotificationSettings(setting = settingKey),
    keywords = keywords,
)

private val settingsSearchEntries = buildList {
    add(
        appearanceEntry(
            "appearance.nightMode",
            "主题模式",
            "切换浅色、深色或跟随系统。",
            "nightMode",
            listOf("夜间模式", "深色模式", "暗色模式", "浅色模式", "跟随系统"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.dynamicColor",
            "使用 Material You 动态取色",
            "Android 12+ 根据系统壁纸取色。",
            "dynamicColor",
            listOf("动态颜色", "壁纸取色", "主题色"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.fontScale",
            "字号与行高",
            "调整正文阅读字号和行距。",
            "fontScale",
            listOf("字体大小", "内容字体", "正文字号", "行距"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.showFeedThumbnail",
            "显示 Feed 卡片缩略图",
            "控制信息流卡片图片显示。",
            "showFeedThumbnail",
            listOf("图片", "封面"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.bottomBar",
            "底部导航栏",
            "启动页、底栏显示页面和底栏行为。",
            APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY,
            listOf(
                "启动默认页面",
                "底部栏",
                "主页",
                "首页",
                "关注",
                "日报",
                "热榜",
                "历史",
                "浏览历史",
                "收藏",
                "收藏夹",
                "账号",
                "回到顶部",
                "双击刷新",
                "自动隐藏底栏",
            ),
        ),
    )
    add(
        appearanceEntry(
            "appearance.shareAction",
            "分享操作",
            "设置分享按钮默认复制、分享或询问。",
            "shareAction",
            listOf("复制链接", "分享链接", "分享按钮"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.showSearchHotSearch",
            "搜索界面显示热搜",
            "控制搜索页空查询时是否展示热搜。",
            "showSearchHotSearch",
            listOf("热搜"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.showSearchHistory",
            "记录并显示搜索历史",
            "控制搜索历史记录和展示。",
            "showSearchHistory",
            listOf("搜索记录", "历史记录", "清除搜索历史"),
        ),
    )
    add(
        appearanceEntry(
            "appearance.autoRefreshHomeOnStartup",
            "启动时自动刷新首页",
            "关闭后启动时优先显示上次获取的一批首页推荐。",
            AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY,
            listOf("自动刷新", "首页缓存", "启动页刷新"),
        ),
    )

    add(
        systemEntry(
            "system.autoCheckUpdates",
            "自动检查更新",
            "应用启动后后台检查新版本。",
            "autoCheckUpdates",
            listOf("更新提醒"),
        ),
    )
    add(
        systemEntry(
            "system.checkNightlyUpdates",
            "检查 Nightly 版本更新",
            "检查每日构建版本。",
            "checkNightlyUpdates",
            listOf("每日构建"),
        ),
    )

    add(
        notificationEntry(
            "notification.autoMarkAsRead",
            "打开通知自动已读",
            "进入通知页后自动标记当前批次为已读。",
            "autoMarkAsRead",
            listOf("已读", "标记已读"),
        ),
    )
    add(
        notificationEntry(
            "notification.unreadBadge",
            "显示未读红点",
            "控制首页和账号入口的未读角标。",
            "unreadBadge",
            listOf("角标", "红点", "未读数"),
        ),
    )
    add(
        notificationEntry(
            "notification.systemNotifications",
            "系统通知",
            "控制是否向系统发送各类通知。",
            "systemNotifications",
            NotificationType.entries.map { it.displayName },
        ),
    )
    add(
        notificationEntry(
            "notification.displayInAppNotifications",
            "应用内显示",
            "控制通知页展示哪些通知类型。",
            "displayInAppNotifications",
            NotificationType.entries.map { it.displayName },
        ),
    )

    add(
        SettingsSearchEntry(
            id = "about.licenses",
            title = "开源许可",
            section = "关于",
            description = "查看第三方组件许可证。",
            destination = Account.OpenSourceLicenses,
            keywords = listOf("许可证"),
        ),
    )
}

/**
 * 设置搜索页。
 *
 * 这里维护的是“可搜索标签 -> 已有设置页 route”的轻量索引，不复制设置项实现。新增设置如果已经支持 `settingKey`
 * 高亮，应补一条索引；不支持高亮的页面级入口只跳到对应页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSearchScreen() {
    val navigator = LocalNavigator.current
    var query by rememberSaveable { mutableStateOf("") }
    val results = remember(query) {
        settingsSearchEntries
            .filter { entry -> entry.matches(query) }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("搜索设置项") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text("搜索设置名称或关键词") },
                    singleLine = true,
                )
            }

            if (results.isEmpty()) {
                item {
                    Text(
                        text = "没有找到相关设置",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(
                    items = results,
                    key = { it.id },
                ) { entry ->
                    SettingItemGroup {
                        SettingItem(
                            modifier = Modifier,
                            title = {
                                Column {
                                    Text(entry.title)
                                    Text(
                                        text = entry.section,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            },
                            description = {
                                Text(entry.description)
                            },
                            onClick = {
                                navigator.onNavigate(entry.destination)
                            },
                            endAction = {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

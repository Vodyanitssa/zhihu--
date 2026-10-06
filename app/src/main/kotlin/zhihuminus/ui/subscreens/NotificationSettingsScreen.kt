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

package com.zhihuminus.ui.subscreens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.settings.LocalAppSettingsRepository
import com.zhihuminus.feature.notification.NotificationType
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.ui.components.SettingItemGroup
import com.zhihuminus.ui.components.SettingItemWithSwitch

/**
 * 通知设置页。
 *
 * 页面分为阅读行为、系统通知和应用内显示三组：自动已读控制进入通知页后的处理方式，未读红点控制入口 badge，
 * 系统通知控制是否向 OS 发通知，应用内显示控制通知中心是否展示某类消息。
 * 统一使用 [LocalAppSettings] 和 [LocalAppSettingsRepository] 响应式管理。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    setting: String? = null,
) {
    val navigator = LocalNavigator.current
    val appSettings = LocalAppSettings.current
    val repository = LocalAppSettingsRepository.current
    val notificationSettings = appSettings.notification
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val highlightedSetting = setting.orEmpty()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("通知设置") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            SettingItemGroup(title = "阅读行为") {
                SettingItemWithSwitch(
                    title = { Text("打开通知自动已读") },
                    description = { Text("进入通知板块后，自动把当前查看的板块标记为已读") },
                    checked = notificationSettings.autoMarkAsRead,
                    onCheckedChange = { checked ->
                        repository.updateNotification { it.copy(autoMarkAsRead = checked) }
                    },
                    settingKey = "autoMarkAsRead",
                    highlightedKey = highlightedSetting,
                )
                SettingItemWithSwitch(
                    title = { Text("显示未读红点") },
                    checked = notificationSettings.showUnreadBadge,
                    onCheckedChange = { checked ->
                        repository.updateNotification { it.copy(showUnreadBadge = checked) }
                    },
                    settingKey = "unreadBadge",
                    highlightedKey = highlightedSetting,
                )
            }

            SettingItemGroup(
                title = "系统通知",
                settingKey = "systemNotifications",
                highlightedKey = highlightedSetting,
            ) {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(type.displayName) },
                        checked = notificationSettings.isSystemNotificationEnabled(type),
                        onCheckedChange = { checked ->
                            repository.updateNotification { current ->
                                current.copy(
                                    systemNotifications = current.systemNotifications + (type to checked),
                                )
                            }
                        },
                    )
                }
            }

            SettingItemGroup(
                title = "应用内显示",
                footer = { Text("选择在通知页面显示哪些通知") },
                settingKey = "displayInAppNotifications",
                highlightedKey = highlightedSetting,
            ) {
                NotificationType.entries.forEach { type ->
                    SettingItemWithSwitch(
                        title = { Text(type.displayName) },
                        checked = notificationSettings.isDisplayInAppEnabled(type),
                        onCheckedChange = { checked ->
                            repository.updateNotification { current ->
                                current.copy(
                                    displayInApp = current.displayInApp + (type to checked),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

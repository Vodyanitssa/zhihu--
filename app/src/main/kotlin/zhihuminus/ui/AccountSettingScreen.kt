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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.zhihuminus.R
import com.zhihuminus.core.environment.rememberPaginationEnvironment
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.state.UnreadNotificationState
import com.zhihuminus.core.state.formatUnreadCount
import com.zhihuminus.core.state.rememberUnreadNotificationCount
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.platform.rememberPlainTextClipboard
import com.zhihuminus.platform.rememberSystemUrlOpener
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup

/**
 * 账号与设置页面。
 *
 * 作为应用的一级设置页面展示，包含用户信息展示、快捷操作入口（收藏夹、历史、通知）、
 * 设置项搜索、各功能模块设置（外观与阅读体验、通知设置）以及关于信息。
 * 顶部提供返回导航按钮，可平滑返回上一级页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    unreadCount: Int? = null,
    showUnreadBadge: Boolean = LocalAppSettings.current.notification.showUnreadBadge,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment()
    val accountState = rememberAccountSettingsAccountState()
    val requestQrLoginScan = rememberAccountQrLoginRequester()
    val copyPlainText = rememberPlainTextClipboard()
    val openSystemUrl = rememberSystemUrlOpener()
    val userMessages = rememberUserMessageSink()
    val versionInfo = rememberAppVersionInfo()
    val sharedUnreadCount by rememberUnreadNotificationCount()
    val effectiveUnreadCount = unreadCount ?: sharedUnreadCount
    var showLogoutDialog by remember { mutableStateOf(false) }
    val data by accountState

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner, environment, data.login) {
        if (!data.login) {
            UnreadNotificationState.update(0)
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val api = ZhihuApiImpl(environment)
            val count = runCatching { api.getMeNotifications().totalCount }.getOrDefault(0)
            UnreadNotificationState.update(count)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("账号与设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (data.login) {
                Row(
                    Modifier
                        .padding(16.dp, 0.dp, 16.dp, 8.dp)
                        .clickable {
                            navigator.onNavigate(
                                Person(
                                    id = data.id,
                                    urlToken = data.urlToken ?: "",
                                    name = data.username,
                                ),
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = data.avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            .clip(CircleShape),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = data.username,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.weight(1f))
                    FilledTonalIconButton(
                        onClick = {
                            requestQrLoginScan()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "扫码登录",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    FilledTonalIconButton(
                        onClick = {
                            showLogoutDialog = true
                        },
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        ),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "退出登录",
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            } else {
                SettingItemGroup {
                    SettingItem(
                        title = { Text("登录知乎") },
                        icon = { Icon(Icons.AutoMirrored.Filled.Login, null) },
                        onClick = {
                            if (!environment.requestLogin()) {
                                userMessages.showShortMessage("当前平台暂不支持登录")
                            }
                        },
                    )
                }
            }

            if (data.login) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 16.dp)
                        .clip(RoundedCornerShape(24.dp)),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    AccountQuickActionItem(
                        icon = Icons.Default.Bookmark,
                        label = "收藏夹",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            data.urlToken?.let { navigator.onNavigate(Collections(it)) }
                        },
                    )
                    AccountQuickActionItem(
                        icon = Icons.Default.History,
                        label = "历史",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            navigator.onNavigate(History)
                        },
                    )
                    AccountQuickActionItem(
                        icon = Icons.Default.Notifications,
                        label = "消息",
                        modifier = Modifier.weight(1f),
                        badge = {
                            if (showUnreadBadge && effectiveUnreadCount > 0) {
                                Badge { Text(formatUnreadCount(effectiveUnreadCount)) }
                            }
                        },
                        onClick = {
                            navigator.onNavigate(Notification)
                        },
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = {
                    navigator.onNavigate(Account.SettingsSearch)
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "搜索设置项",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            SettingItemGroup {
                SettingItem(
                    title = { Text("外观与阅读体验") },
                    description = { Text("主题颜色、字体大小等") },
                    icon = { Icon(Icons.Default.Palette, null) },
                    onClick = {
                        navigator.onNavigate(Account.AppearanceSettings())
                    },
                )

                SettingItem(
                    title = { Text("通知设置") },
                    description = { Text("未读红点、系统通知与应用内显示") },
                    icon = { Icon(Icons.Default.Notifications, null) },
                    onClick = {
                        navigator.onNavigate(Account.NotificationSettings())
                    },
                )
            }

            SettingItemGroup(
                title = "关于",
                footer = { Text("本软件仅供学习交流使用，应用内内容由知乎网站提供，著作权归其对应作者所有。") },
            ) {
                SettingItem(
                    title = { Text("知乎++") },
                    description = { Text("版本号：$versionInfo") },
                    icon = {
                        Image(
                            painterResource(R.drawable.ic_zhihuminus_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier
                                .clip(CircleShape)
                                .size(32.dp),
                        )
                    },
                    modifier = Modifier.combinedClickable(
                        enabled = true,
                        onClick = {},
                        onLongClick = {
                            copyPlainText("version", versionInfo)
                            userMessages.showShortMessage("已复制版本号")
                        },
                    ),
                )
                SettingItem(
                    title = { Text("GitHub 项目地址") },
                    description = { Text("https://github.com/zly2006/zhihu-plus-plus") },
                    icon = { Icon(painterResource(R.drawable.ic_github_24dp), null) },
                    onClick = {
                        openSystemUrl("https://github.com/zly2006/zhihu-plus-plus")
                    },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )

                SettingItem(
                    title = { Text("项目协议") },
                    description = { Text("AGPL-3.0-only") },
                    icon = { Icon(painterResource(R.drawable.ic_license_24dp), null) },
                    onClick = {
                        openSystemUrl("https://github.com/zly2006/zhihu-plus-plus/blob/master/LICENSE")
                    },
                    endAction = {
                        Icon(
                            Icons.Default.ArrowOutward,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                SettingItem(
                    title = { Text("开源许可") },
                    description = { Text("查看第三方组件许可证") },
                    icon = { Icon(painterResource(R.drawable.ic_license_24dp), null) },
                    onClick = {
                        navigator.onNavigate(Account.OpenSourceLicenses)
                    },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录") },
            text = { Text("确定要退出登录吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        environment.logout()
                        showLogoutDialog = false
                        userMessages.showShortMessage("已退出登录")
                    },
                ) {
                    Text("退出")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun AccountQuickActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: @Composable (BoxScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clickable(onClick = onClick)
            .padding(8.dp, 16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (badge != null) {
            BadgedBox(badge = badge) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

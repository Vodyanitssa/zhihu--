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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zhihuminus.R
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.platform.rememberPlainTextClipboard
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.platform.rememberSystemUrlOpener
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup
import com.zhihuminus.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.zhihuminus.ui.subscreens.defaultBottomBarSelectionKeys
import com.zhihuminus.ui.subscreens.normalizeBottomBarSelection
import com.zhihuminus.ui.subscreens.shouldShowAccountHistoryShortcut
import com.zhihuminus.util.Log
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

/**
 * 账号与设置入口页。
 *
 * 已登录时顶部展示头像、昵称、扫码登录和退出登录，并额外展示收藏夹、关注订阅、通知和历史等快捷块；
 * 未登录时只展示登录入口。下方设置区是外观、推荐过滤、系统更新、开发者选项和开源许可的统一入口，其中开发者选项通过连续点击版本号开启。
 *
 * 这个页面既可以作为底部栏 tab 展示，也可以作为主页头像弹出的账号面板内容使用，所以 [innerPadding]、[onDismissRequest]
 * 相关逻辑都不能随意删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingScreen(
    innerPadding: PaddingValues,
    unreadCount: Int = 0,
    showUnreadBadge: Boolean = true,
    onDismissRequest: () -> Unit = {},
    refreshAccountProfileOnEnter: Boolean = true,
    testAccountData: AccountSettingsAccountState? = null,
) {
    val navigator = LocalNavigator.current
    val environment = rememberPaginationEnvironment()
    val accountState = rememberAccountSettingsAccountState()
    val requestQrLoginScan = rememberAccountQrLoginRequester()
    val settings = rememberSettingsStore()
    val copyPlainText = rememberPlainTextClipboard()
    val openSystemUrl = rememberSystemUrlOpener()
    val userMessages = rememberUserMessageSink()
    val versionInfo = rememberAppVersionInfo()

    val selectedBottomBarItemKeys = remember {
        normalizeBottomBarSelection(
            settings.getStringSet(
                BOTTOM_BAR_ITEMS_PREFERENCE_KEY,
                defaultBottomBarSelectionKeys(),
            ),
            enforceMinimumSelection = true,
        )
    }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val liveData by accountState
    val data = testAccountData ?: liveData

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(padding),
        ) {
            LaunchedEffect(data.login, refreshAccountProfileOnEnter) {
                if (refreshAccountProfileOnEnter && data.login) {
                    try {
                        environment.refreshAccountProfile()
                    } catch (e: Exception) {
                        Log.e("AccountSettingScreen", "Failed to refresh account profile", e)
                        userMessages.showShortMessage("获取用户信息失败")
                    }
                }
            }

            if (data.login) {
                Row(
                    Modifier
                        .padding(16.dp, 0.dp, 16.dp, 16.dp)
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
                        modifier = Modifier,
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
                        modifier = Modifier,
                        onClick = {
                            if (!environment.requestLogin()) {
                                userMessages.showShortMessage("当前平台暂不支持登录")
                            }
                        },
                    )
                }
            }

            Row(
                Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 32.dp)
                    .clip(RoundedCornerShape(24.dp)),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (data.login) {
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                data.urlToken?.let { navigator.onNavigate(Collections(it)) }
                            }.padding(8.dp, 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Bookmark,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "收藏夹",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                navigator.onNavigate(
                                    Person(
                                        id = data.id,
                                        urlToken = data.urlToken ?: "",
                                        name = data.username,
                                        jumpTo = "关注订阅",
                                    ),
                                )
                            }.padding(8.dp, 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "关注订阅",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                onDismissRequest()
                                navigator.onNavigate(Notification)
                            }.padding(8.dp, 16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        BadgedBox(
                            badge = {
                                if (showUnreadBadge && unreadCount > 0) {
                                    Badge { Text(unreadCount.toString()) }
                                }
                            },
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "通知",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    if (shouldShowAccountHistoryShortcut(selectedBottomBarItemKeys)) {
                        Column(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable {
                                    onDismissRequest()
                                    navigator.onNavigateTopLevel(History)
                                }.padding(8.dp, 16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                Icons.Default.History,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "浏览历史",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }

            Column(Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    modifier = Modifier
                        .height(36.dp),
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
            }

            SettingItemGroup {
                if (data.login && data.identityManagementSupported) {
                    SettingItem(
                        title = { Text("身份管理") },
                        description = { Text("创建马甲号或切换当前账号") },
                        icon = { Icon(Icons.Default.SwitchAccount, null) },
                        modifier = Modifier,
                        onClick = { navigator.onNavigate(Account.IdentityManagement) },
                    )
                }

                SettingItem(
                    title = { Text("外观与阅读体验") },
                    description = { Text("主题颜色、字体大小等") },
                    icon = { Icon(Icons.Default.Palette, null) },
                    modifier = Modifier,
                    onClick = { navigator.onNavigate(Account.AppearanceSettings()) },
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
                    modifier = Modifier,
                    onClick = { navigator.onNavigate(Account.OpenSourceLicenses) },
                )
            }
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

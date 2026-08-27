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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberSettingsStore
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.ui.AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup
import com.zhihuminus.ui.components.SettingItemWithSwitch
import com.zhihuminus.viewmodel.filter.getContentFilterDatabase
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

/**
 * 推荐系统与内容过滤设置页。
 *
 * 页面分为推荐来源、智能过滤、关键词/用户/主题屏蔽、广告/付费内容过滤、屏蔽列表、屏蔽记录和过滤统计。这里的开关会影响首页信息流
 * 数据来源和 Feed 卡片的更多菜单行为，不能只按静态设置页验证；改动后要检查推荐拉取、过滤运行时和 Blocklist 管理入口。
 *
 * [setting] 用于从其他 UI 入口跳转并高亮具体设置项，新增过滤能力时应保持这个参数可用，方便用户从提示或弹窗直接回到相关配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterSettingsScreen(
    setting: String? = null,
) {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    val settings = rememberSettingsStore()
    val contentFilterDao = remember { getContentFilterDatabase().contentFilterDao() }
    val userMessages = rememberUserMessageSink()
    val highlightedSetting = setting.orEmpty()

    val scrollState = rememberScrollState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(highlightedSetting) {
        if (highlightedSetting.isNotEmpty()) {
            delay(200.milliseconds)
            // 收缩 LargeTopAppBar（programmatic scroll 不触发 nestedScroll）
            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("推荐系统与内容过滤") },
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
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(vertical = 16.dp),
        ) {
            SettingItemGroup {
                val isLoginForRecommendation = remember {
                    mutableStateOf(settings.getBoolean("loginForRecommendation", true))
                }
                SettingItemWithSwitch(
                    modifier = Modifier,
                    title = { Text("推荐内容时登录") },
                    description = { Text("获取推荐内容时携带登录凭证") },
                    checked = isLoginForRecommendation.value,
                    onCheckedChange = { checked ->
                        isLoginForRecommendation.value = checked
                        settings.putBoolean("loginForRecommendation", checked)
                    },
                    settingKey = "loginForRecommendation",
                    highlightedKey = highlightedSetting,
                )

                val autoRefreshHomeOnStartup = remember {
                    mutableStateOf(settings.getBoolean(AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY, true))
                }
                SettingItemWithSwitch(
                    modifier = Modifier,
                    title = { Text("启动时自动刷新首页") },
                    description = { Text("关闭后优先显示上次获取的一批首页推荐；没有缓存时仍会加载新推荐") },
                    checked = autoRefreshHomeOnStartup.value,
                    onCheckedChange = { checked ->
                        autoRefreshHomeOnStartup.value = checked
                        settings.putBoolean(AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY, checked)
                    },
                    settingKey = AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY,
                    highlightedKey = highlightedSetting,
                )
            }

            val enableContentFilter = remember { mutableStateOf(settings.getBoolean("enableContentFilter", true)) }
            SettingItemGroup {
                SettingItemWithSwitch(
                    modifier = Modifier,
                    title = { Text("启用智能内容过滤") },
                    description = { Text("自动过滤首页展示超过2次但用户未点击的内容，减少重复推荐") },
                    checked = enableContentFilter.value,
                    onCheckedChange = {
                        enableContentFilter.value = it
                        settings.putBoolean("enableContentFilter", it)
                    },
                    settingKey = "enableContentFilter",
                    highlightedKey = highlightedSetting,
                )
            }

            SettingItemGroup {
                val enableKeywordBlocking =
                    remember { mutableStateOf(settings.getBoolean("enableKeywordBlocking", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用关键词屏蔽") },
                    description = { Text("屏蔽包含特定关键词的内容") },
                    checked = enableKeywordBlocking.value,
                    onCheckedChange = {
                        enableKeywordBlocking.value = it
                        settings.putBoolean("enableKeywordBlocking", it)
                    },
                    settingKey = "enableKeywordBlocking",
                    highlightedKey = highlightedSetting,
                )

                val enableTopicBlocking = remember { mutableStateOf(settings.getBoolean("enableTopicBlocking", true)) }
                SettingItemWithSwitch(
                    title = { Text("启用主题屏蔽") },
                    description = { Text("屏蔽包含特定主题的内容") },
                    checked = enableTopicBlocking.value,
                    onCheckedChange = {
                        enableTopicBlocking.value = it
                        settings.putBoolean("enableTopicBlocking", it)
                    },
                    settingKey = "enableTopicBlocking",
                    highlightedKey = highlightedSetting,
                )
            }

            SettingItemGroup {
                val blockZhihuAdPlatform =
                    remember { mutableStateOf(settings.getBoolean("blockZhihuAdPlatform", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎广告平台内容") },
                    description = { Text("匹配并屏蔽包含 xg.zhihu.com 的推广内容") },
                    checked = blockZhihuAdPlatform.value,
                    onCheckedChange = {
                        blockZhihuAdPlatform.value = it
                        settings.putBoolean("blockZhihuAdPlatform", it)
                    },
                    settingKey = "blockZhihuAdPlatform",
                    highlightedKey = highlightedSetting,
                )

                val blockZhihuSchool = remember { mutableStateOf(settings.getBoolean("blockZhihuSchool", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎学堂内容") },
                    description = { Text("匹配并屏蔽包含 d.zhihu.com 或 data-edu-card-id 的内容") },
                    checked = blockZhihuSchool.value,
                    onCheckedChange = {
                        blockZhihuSchool.value = it
                        settings.putBoolean("blockZhihuSchool", it)
                    },
                    settingKey = "blockZhihuSchool",
                    highlightedKey = highlightedSetting,
                )

                val blockWeChatOfficialAccount =
                    remember { mutableStateOf(settings.getBoolean("blockWeChatOfficialAccount", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽微信公众号文章") },
                    description = { Text("匹配并屏蔽包含 mp.weixin.qq.com 的外链文章") },
                    checked = blockWeChatOfficialAccount.value,
                    onCheckedChange = {
                        blockWeChatOfficialAccount.value = it
                        settings.putBoolean("blockWeChatOfficialAccount", it)
                    },
                    settingKey = "blockWeChatOfficialAccount",
                    highlightedKey = highlightedSetting,
                )

                val blockPaidContent = remember { mutableStateOf(settings.getBoolean("blockPaidContent", true)) }
                SettingItemWithSwitch(
                    title = { Text("屏蔽知乎盐选付费内容") },
                    description = { Text("屏蔽知乎盐选会员专享的付费回答和文章") },
                    checked = blockPaidContent.value,
                    onCheckedChange = {
                        blockPaidContent.value = it
                        settings.putBoolean("blockPaidContent", it)
                    },
                    settingKey = "blockPaidContent",
                    highlightedKey = highlightedSetting,
                )
            }

            SettingItemGroup {
                SettingItem(
                    modifier = Modifier,
                    title = { Text("管理屏蔽列表") },
                    onClick = { navigator.onNavigate(Account.RecommendSettings.Blocklist) },
                    endAction = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }

            SettingItemGroup {
                SettingItem(
                    modifier = Modifier,
                    title = { Text("屏蔽记录") },
                    onClick = { navigator.onNavigate(Account.RecommendSettings.BlockedFeedHistory) },
                    endAction = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
            }
        }
    }
}

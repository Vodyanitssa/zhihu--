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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.settings.LocalAppSettingsRepository
import com.zhihuminus.core.settings.model.ShareActionOption
import com.zhihuminus.core.settings.model.StartDestinationOption
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.platform.rememberUserMessageSink
import com.zhihuminus.theme.ThemeManager
import com.zhihuminus.theme.ThemeMode
import com.zhihuminus.ui.AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY
import com.zhihuminus.ui.components.ColorPickerDialog
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup
import com.zhihuminus.ui.components.SettingItemWithSwitch
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.milliseconds

const val APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY = "appearanceSettings.bottomBarSection"

/**
 * 外观与阅读体验设置页。
 *
 * 这里集中管理主题、字号/行高、信息流缩略图、文章页行为、底部导航栏、分享、搜索和技术性导航开关。页面支持通过 [setting]
 * 跳入指定设置项并高亮滚动到位，因此新增设置时应提供稳定的 `settingKey`，必要时也补充 test tag。
 *
 * 所有设置均通过 [LocalAppSettings] 和 [LocalAppSettingsRepository] 统一响应式驱动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    setting: String?,
    onExit: () -> Unit = {},
) {
    val settingKey = setting.orEmpty()
    val appSettings = LocalAppSettings.current
    val repository = LocalAppSettingsRepository.current
    val userMessages = rememberUserMessageSink()

    val scrollState = rememberScrollState()
    val navigator = LocalNavigator.current

    val bringIntoViewRequesters = remember { mutableStateMapOf<String, BringIntoViewRequester>() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var scrolledSetting by remember { mutableStateOf<String?>(null) }

    fun requesterFor(settingKey: String): BringIntoViewRequester =
        bringIntoViewRequesters.getOrPut(settingKey) { BringIntoViewRequester() }

    DisposableEffect(Unit) {
        onDispose {
            onExit()
        }
    }

    LaunchedEffect(settingKey, bringIntoViewRequesters[settingKey]) {
        if (settingKey.isNotEmpty() && scrolledSetting != settingKey) {
            bringIntoViewRequesters[settingKey]?.let { requester ->
                scrolledSetting = settingKey
                delay(200.milliseconds)
                // 收缩 LargeTopAppBar（programmatic scroll 不触发 nestedScroll）
                scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
                requester.bringIntoView()
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("外观与阅读体验") },
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
            val themeSettings = appSettings.theme
            val readingSettings = appSettings.reading
            val navigationSettings = appSettings.navigation
            val interactionSettings = appSettings.interaction

            // ── 主题 ────────────────────────────────────────────────────────────

            SettingItemGroup(
                title = "主题",
            ) {
                SettingItem(
                    title = { Text("主题模式") },
                    description = { Text("设置应用的显示主题。") },
                    settingKey = "nightMode",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("nightMode"),
                    bottomAction = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        ) {
                            val themeModes = listOf(
                                ThemeMode.SYSTEM to "自动",
                                ThemeMode.LIGHT to "亮色",
                                ThemeMode.DARK to "暗色",
                            )
                            themeModes.forEach { (mode, label) ->
                                val isSelected = themeSettings.mode == mode
                                OutlinedButton(
                                    onClick = {
                                        repository.updateTheme { it.copy(mode = mode) }
                                        userMessages.showShortMessage("已切换到$label")
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            Color.Transparent
                                        },
                                        contentColor = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    ),
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text(label)
                                }
                            }
                        }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("使用 Material You 动态取色") },
                    description = { Text("根据系统壁纸自动提取主题色（Android 12+ 可用）。\n关闭后可以自己设定主题颜色。") },
                    checked = themeSettings.useDynamicColor,
                    onCheckedChange = {
                        repository.updateTheme { current -> current.copy(useDynamicColor = it) }
                        userMessages.showShortMessage("已${if (it) "启用" else "禁用"}动态取色")
                    },
                    settingKey = "dynamicColor",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("dynamicColor"),
                )

                var showColorPicker by remember { mutableStateOf(false) }
                val customColor = Color(themeSettings.customColor)

                AnimatedVisibility(visible = !themeSettings.useDynamicColor) {
                    SettingItem(
                        title = { Text("自定义主题色") },
                        description = { Text("点击选择您喜欢的主题颜色") },
                        onClick = { showColorPicker = true },
                        endAction = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(customColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                            )
                        },
                    )
                }
                if (showColorPicker) {
                    ColorPickerDialog(
                        title = "选择主题色",
                        initialColor = customColor,
                        onDismiss = { showColorPicker = false },
                        onColorSelected = { color ->
                            repository.updateTheme { it.copy(customColor = color.toArgb()) }
                            userMessages.showShortMessage("主题色已保存")
                            showColorPicker = false
                        },
                    )
                }

                var showLuotianYiColorPicker by remember { mutableStateOf(false) }
                val luotianYiColor = Color(themeSettings.browserToolbarColor)

                SettingItem(
                    title = { Text("唤起浏览器主题色") },
                    description = { Text("应用内浏览器的工具栏颜色") },
                    onClick = { showLuotianYiColorPicker = true },
                    endAction = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(luotianYiColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    },
                )

                if (showLuotianYiColorPicker) {
                    ColorPickerDialog(
                        title = "选择浏览器主题色",
                        initialColor = luotianYiColor,
                        presetColors = listOf(
                            Color(0xFF66CCFF),
                            Color(0xFF2196F3),
                            Color(0xFF4CAF50),
                            Color(0xFFF44336),
                            Color(0xFFFF9800),
                            Color(0xFF9C27B0),
                        ),
                        onDismiss = { showLuotianYiColorPicker = false },
                        onColorSelected = { color ->
                            repository.updateTheme { it.copy(browserToolbarColor = color.toArgb()) }
                            userMessages.showShortMessage("浏览器主题色已保存")
                            showLuotianYiColorPicker = false
                        },
                    )
                }

                val currentIsDarkTheme = ThemeManager.isDarkTheme()
                var showBackgroundColorPicker by remember { mutableStateOf(false) }
                val backgroundColor = if (currentIsDarkTheme) {
                    Color(themeSettings.backgroundColorDark)
                } else {
                    Color(themeSettings.backgroundColorLight)
                }

                SettingItem(
                    title = { Text("自定义背景颜色") },
                    description = { Text(if (currentIsDarkTheme) "深色模式背景色" else "浅色模式背景色") },
                    onClick = { showBackgroundColorPicker = true },
                    endAction = {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(backgroundColor)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        )
                    },
                )

                if (showBackgroundColorPicker) {
                    ColorPickerDialog(
                        title = "选择背景颜色",
                        initialColor = backgroundColor,
                        presetColors = listOfNotNull(
                            Color(if (currentIsDarkTheme) 0xFF121212.toInt() else 0xFFFFFFFF.toInt()),
                            MaterialTheme.colorScheme.surfaceContainer,
                            if (ThemeManager.isDarkTheme()) Color.Black else null,
                        ),
                        onDismiss = { showBackgroundColorPicker = false },
                        onColorSelected = { color ->
                            repository.updateTheme { current ->
                                if (currentIsDarkTheme) {
                                    current.copy(backgroundColorDark = color.toArgb())
                                } else {
                                    current.copy(backgroundColorLight = color.toArgb())
                                }
                            }
                            userMessages.showShortMessage("背景颜色已保存")
                            showBackgroundColorPicker = false
                        },
                    )
                }

                val fabOpacity = readingSettings.fabOpacityPercent
                SettingItem(
                    title = { Text("悬浮按钮透明度") },
                    description = { Text("控制所有悬浮按钮的透明度 ($fabOpacity%)。") },
                    bottomAction = {
                        Slider(
                            value = fabOpacity.toFloat(),
                            onValueChange = {
                                val v = (it / 5).roundToInt() * 5
                                repository.updateReading { current -> current.copy(fabOpacityPercent = v) }
                            },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    },
                )
            }

            // ── 阅读 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "阅读",
            ) {
                val fontSize = readingSettings.fontSizePercent
                SettingItem(
                    title = { Text("字号") },
                    description = { Text("调整内容文字大小 ($fontSize%)") },
                    settingKey = "fontScale",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("fontScale"),
                    bottomAction = {
                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = {
                                repository.updateReading { current -> current.copy(fontSizePercent = it.toInt()) }
                            },
                            valueRange = 50f..200f,
                            steps = 14,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    },
                )

                val lineHeight = readingSettings.lineHeightPercent
                SettingItem(
                    title = { Text("行高") },
                    description = { Text("调整内容行间距 (${lineHeight / 100f})") },
                    bottomAction = {
                        Slider(
                            value = lineHeight.toFloat(),
                            onValueChange = {
                                repository.updateReading { current -> current.copy(lineHeightPercent = it.toInt()) }
                            },
                            valueRange = 100f..300f,
                            steps = 19,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                        )
                    },
                )
            }

            // ── 信息流 ──────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "信息流",
            ) {
                SettingItemWithSwitch(
                    title = { Text("启动时自动刷新首页") },
                    description = { Text("关闭后优先显示上次获取的一批首页推荐；没有缓存时仍会加载新推荐") },
                    checked = interactionSettings.autoRefreshHomeOnStartup,
                    onCheckedChange = { checked ->
                        repository.updateInteraction { it.copy(autoRefreshHomeOnStartup = checked) }
                    },
                    settingKey = AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY,
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor(AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY),
                )
            }

            // ── 底部导航栏 ──────────────────────────────────────────────────────
            var startDestinationExpanded by remember { mutableStateOf(false) }

            SettingItemGroup(
                title = "底部导航栏",
                settingKey = APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY,
                highlightedKey = settingKey,
                bringIntoViewRequester = requesterFor(APPEARANCE_SETTINGS_BOTTOM_BAR_SECTION_KEY),
            ) {
                SettingItem(
                    title = { Text("应用启动默认页面") },
                    description = { Text("可选择应用打开时的初始页面。") },
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = startDestinationExpanded,
                            onExpandedChange = { startDestinationExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = navigationSettings.startDestination.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startDestinationExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = startDestinationExpanded,
                                onDismissRequest = { startDestinationExpanded = false },
                            ) {
                                StartDestinationOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            repository.updateNavigation { it.copy(startDestination = option) }
                                            startDestinationExpanded = false
                                            userMessages.showShortMessage("已设置启动页：${option.displayName}，重启后生效")
                                        },
                                    )
                                }
                            }
                        }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("点击底部导航栏回到顶部/刷新") },
                    description = { Text("点击底部导航栏当前页面按钮回到顶部，已在顶部时则刷新页面。双击可直接刷新。") },
                    checked = navigationSettings.tapToScrollToTop,
                    onCheckedChange = {
                        repository.updateNavigation { current -> current.copy(tapToScrollToTop = it) }
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("滚动时自动隐藏底部导航栏") },
                    description = { Text("上划时隐藏底部导航栏，下划时重新显示。") },
                    checked = navigationSettings.autoHideBottomBar,
                    onCheckedChange = {
                        repository.updateNavigation { current -> current.copy(autoHideBottomBar = it) }
                    },
                )
            }

            // ── 交互 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "交互",
            ) {
                var shareActionExpanded by remember { mutableStateOf(false) }
                SettingItem(
                    title = { Text("分享操作") },
                    description = { Text("点击分享按钮时的默认行为。") },
                    settingKey = "shareAction",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("shareAction"),
                    endAction = {
                        ExposedDropdownMenuBox(
                            expanded = shareActionExpanded,
                            onExpandedChange = { shareActionExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = interactionSettings.shareAction.displayName,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = shareActionExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                    .width(160.dp),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            )
                            ExposedDropdownMenu(
                                expanded = shareActionExpanded,
                                onDismissRequest = { shareActionExpanded = false },
                            ) {
                                ShareActionOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.displayName) },
                                        onClick = {
                                            repository.updateInteraction { it.copy(shareAction = option) }
                                            shareActionExpanded = false
                                            userMessages.showShortMessage("已设置为：${option.displayName}")
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
            }

            // ── 搜索 ────────────────────────────────────────────────────────────
            SettingItemGroup(
                title = "搜索",
            ) {
                SettingItemWithSwitch(
                    title = { Text("搜索界面显示热搜") },
                    description = { Text("在搜索界面空白时显示知乎热搜关键词。") },
                    checked = interactionSettings.showSearchHotSearch,
                    onCheckedChange = {
                        repository.updateInteraction { current -> current.copy(showSearchHotSearch = it) }
                    },
                    settingKey = "showSearchHotSearch",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("showSearchHotSearch"),
                )
                SettingItemWithSwitch(
                    title = { Text("记录并显示搜索历史") },
                    description = { Text("在搜索界面显示最近搜索过的关键词，关闭后不再记录新的搜索。") },
                    checked = interactionSettings.showSearchHistory,
                    onCheckedChange = {
                        repository.updateInteraction { current -> current.copy(showSearchHistory = it) }
                    },
                    settingKey = "showSearchHistory",
                    highlightedKey = settingKey,
                    bringIntoViewRequester = requesterFor("showSearchHistory"),
                )
            }
        }
    }
}

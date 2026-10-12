/*
 * Zhihu-- - Free & Ad-Free Zhihu client for Android.
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DataObject
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.util.withContext
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.ui.components.SettingItem
import com.zhihuminus.ui.components.SettingItemGroup

data class ManualLicenseEntry(
    val name: String,
    val license: String,
    val summary: String,
    val url: String,
    val icon: @Composable () -> Unit,
)

val upstreamLibraries = listOf(
    ManualLicenseEntry(
        name = "zhihu-plus-plus",
        license = "AGPL-3.0-only",
        summary = "知乎第三方全平台客户端，本项目基于该项目分支衍生开发。原作者：Liyan Zhao (zly2006)",
        url = "https://github.com/zly2006/zhihu-plus-plus",
        icon = { Icon(Icons.Default.DataObject, contentDescription = null) },
    ),
)

/**
 * 开源许可列表内容。
 *
 * 许可证主体由 about-libraries 生成，手动维护项用于补充当前工具无法自动识别的 Rust/Full variant 依赖。新增依赖时应优先让
 * 构建系统生成许可证；只有无法自动发现时再加入 [manualLibraries]。
 */
@Suppress("DEPRECATION")
@Composable
fun OpenSourceLicensesContent(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    manualLibraries: List<ManualLicenseEntry>,
    onOpenUrl: (String) -> Unit,
) {
    LibrariesContainer(
        libraries = rememberOpenSourceLicensesLibraries(),
        modifier = modifier,
        contentPadding = contentPadding,
        header = {
            if (manualLibraries.isNotEmpty()) {
                item {
                    ManualLicenseEntryGroup(
                        manualLibraries = manualLibraries,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
        },
    )
}

/**
 * 开源许可页面。
 *
 * 页面包装许可证列表、返回按钮和滚动行为，并在顶部展示上游基础项目许可证项。它是账号页“开源许可”入口的终点。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceLicensesScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val manualLibraries = upstreamLibraries

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text("开源许可") },
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
        OpenSourceLicensesContent(
            contentPadding = PaddingValues(16.dp),
            manualLibraries = manualLibraries,
            onOpenUrl = uriHandler::openUri,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
fun ManualLicenseEntryGroup(
    manualLibraries: List<ManualLicenseEntry>,
    onOpenUrl: (String) -> Unit,
) {
    if (manualLibraries.isEmpty()) {
        return
    }
    SettingItemGroup(title = "上游基础项目") {
        manualLibraries.forEach { entry ->
            SettingItem(
                title = { Text(entry.name) },
                description = { Text("${entry.license} · ${entry.summary}") },
                icon = entry.icon,
                onClick = { onOpenUrl(entry.url) },
            )
        }
    }
}

@Composable
fun rememberOpenSourceLicensesLibraries(): Libs {
    val context = LocalContext.current
    return remember(context) {
        Libs
            .Builder()
            .withContext(context)
            .build()
    }
}

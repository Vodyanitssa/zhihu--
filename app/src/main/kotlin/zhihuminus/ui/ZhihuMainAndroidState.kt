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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.settings.model.NavigationSettings

/**
 * 读取当前设置中会影响主壳的设置快照。
 *
 * 这些设置决定启动页和自动隐藏行为。
 */
@Composable
fun rememberAndroidZhihuMainPreferenceState(
    navigation: NavigationSettings = LocalAppSettings.current.navigation,
): ZhihuMainPreferenceState {
    val state = rememberZhihuMainPreferenceState {
        ZhihuMainPreferenceSnapshot(
            tapToScrollToTopEnabled = navigation.tapToScrollToTop,
            autoHideBottomBar = navigation.autoHideBottomBar,
            startDestination = navigation.startDestination.toTopLevelDestination(),
        )
    }
    LaunchedEffect(navigation) {
        state.reload()
    }
    return state
}

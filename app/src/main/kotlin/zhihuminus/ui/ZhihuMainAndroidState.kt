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
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.zhihuminus.navigation.Home
import com.zhihuminus.platform.androidSettingsStore
import com.zhihuminus.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import com.zhihuminus.ui.subscreens.navDestinationFromName

/**
 * 读取 Android SharedPreferences 中会影响主壳的设置快照。
 *
 * 这些设置决定启动页和自动隐藏行为。设置页退出后会重新读取这份快照。
 */
@Composable
fun rememberAndroidZhihuMainPreferenceState(): ZhihuMainPreferenceState {
    val context = LocalContext.current
    val settings = remember(context) {
        androidSettingsStore(context)
    }
    return rememberZhihuMainPreferenceState {
        ZhihuMainPreferenceSnapshot(
            tapToScrollToTopEnabled = settings.getBoolean("bottomBarTapScrollToTop", true),
            autoHideBottomBar = settings.getBoolean("autoHideBottomBar", false),
            startDestination = navDestinationFromName(
                settings.getString(START_DESTINATION_PREFERENCE_KEY, Home.name),
            ),
        )
    }
}

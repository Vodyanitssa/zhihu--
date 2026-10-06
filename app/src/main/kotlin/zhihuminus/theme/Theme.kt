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

package com.zhihuminus.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.dynamicColorScheme
import com.zhihuminus.core.settings.AndroidAppSettingsRepository
import com.zhihuminus.core.settings.LocalAppSettings
import com.zhihuminus.core.settings.model.ThemeSettings

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)

@Composable
fun ZhihuTheme(
    themeSettings: ThemeSettings = LocalAppSettings.current.theme,
    content: @Composable () -> Unit,
) {
    SideEffect {
        ThemeManager.sync(themeSettings)
    }
    val useDynamicColor = themeSettings.useDynamicColor
    val darkTheme = resolveDarkTheme(themeSettings.mode, currentSystemInDarkTheme())
    ThemeManager.isDarkTheme = darkTheme
    val customBackgroundColor = if (darkTheme) {
        Color(themeSettings.backgroundColorDark)
    } else {
        Color(themeSettings.backgroundColorLight)
    }
    val platformDynamicColorScheme = platformDynamicColorScheme(darkTheme)

    val baseColorScheme = when {
        useDynamicColor && platformDynamicColorScheme != null -> platformDynamicColorScheme
        !useDynamicColor -> {
            dynamicColorScheme(
                seedColor = Color(themeSettings.customColor),
                isDark = darkTheme,
                isAmoled = false,
            )
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = baseColorScheme.copy(
        primary = Color(0XFF1772F6),
        background = customBackgroundColor,
        surface = customBackgroundColor,
    )

    PlatformSystemBarEffect(darkTheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

object AndroidThemeSettings {
    fun initialize(context: Context) {
        val theme = AndroidAppSettingsRepository.getInstance(context).current.theme
        ThemeManager.sync(theme)
    }
}

@Composable
fun currentSystemInDarkTheme(): Boolean = isSystemInDarkTheme()

@Composable
fun platformDynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

@Composable
fun PlatformSystemBarEffect(darkTheme: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }
}

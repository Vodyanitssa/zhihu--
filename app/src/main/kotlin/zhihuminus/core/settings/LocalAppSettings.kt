package com.zhihuminus.core.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.zhihuminus.core.settings.model.AppSettings

val LocalAppSettings = compositionLocalOf { AppSettings() }

val LocalAppSettingsRepository = staticCompositionLocalOf<AppSettingsRepository> {
    error("AppSettingsRepository not provided")
}

@Composable
fun rememberAppSettings(): AppSettings = LocalAppSettings.current

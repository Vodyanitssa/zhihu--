package com.zhihuminus.core.settings

import com.zhihuminus.core.settings.model.AppSettings
import com.zhihuminus.core.settings.model.InteractionSettings
import com.zhihuminus.core.settings.model.NavigationSettings
import com.zhihuminus.core.settings.model.NotificationSettings
import com.zhihuminus.core.settings.model.ReadingSettings
import com.zhihuminus.core.settings.model.ThemeSettings
import kotlinx.coroutines.flow.StateFlow

interface AppSettingsRepository {
    val settingsFlow: StateFlow<AppSettings>

    val current: AppSettings

    suspend fun update(transform: (AppSettings) -> AppSettings)

    fun updateAsync(transform: (AppSettings) -> AppSettings)

    fun updateTheme(transform: (ThemeSettings) -> ThemeSettings) =
        updateAsync { it.copy(theme = transform(it.theme)) }

    fun updateReading(transform: (ReadingSettings) -> ReadingSettings) =
        updateAsync { it.copy(reading = transform(it.reading)) }

    fun updateNavigation(transform: (NavigationSettings) -> NavigationSettings) =
        updateAsync { it.copy(navigation = transform(it.navigation)) }

    fun updateInteraction(transform: (InteractionSettings) -> InteractionSettings) =
        updateAsync { it.copy(interaction = transform(it.interaction)) }

    fun updateNotification(transform: (NotificationSettings) -> NotificationSettings) =
        updateAsync { it.copy(notification = transform(it.notification)) }
}

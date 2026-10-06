package com.zhihuminus.core.settings.model

import com.zhihuminus.feature.notification.NotificationType
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.theme.ThemeMode
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val theme: ThemeSettings = ThemeSettings(),
    val reading: ReadingSettings = ReadingSettings(),
    val navigation: NavigationSettings = NavigationSettings(),
    val interaction: InteractionSettings = InteractionSettings(),
    val notification: NotificationSettings = NotificationSettings(),
)

@Serializable
data class ThemeSettings(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val customColor: Int = 0xFF2196F3.toInt(),
    val backgroundColorLight: Int = 0xFFFFFFFF.toInt(),
    val backgroundColorDark: Int = 0xFF121212.toInt(),
    val browserToolbarColor: Int = 0xFF66CCFF.toInt(),
)

@Serializable
data class ReadingSettings(
    val fontSizePercent: Int = 100,
    val lineHeightPercent: Int = 160,
    val fabOpacityPercent: Int = 100,
)

@Serializable
data class NavigationSettings(
    val startDestination: StartDestinationOption = StartDestinationOption.Home,
    val tapToScrollToTop: Boolean = true,
    val autoHideBottomBar: Boolean = false,
)

@Serializable
enum class StartDestinationOption(
    val displayName: String,
) {
    Home("主页"),
    Follow("关注"),
    Daily("日报"),
    ;

    fun toTopLevelDestination(): TopLevelDestination = when (this) {
        Home -> com.zhihuminus.navigation.Home
        Follow -> com.zhihuminus.navigation.Follow
        Daily -> com.zhihuminus.navigation.Daily
    }

    companion object {
        fun fromTopLevelDestination(destination: TopLevelDestination): StartDestinationOption =
            when (destination.name) {
                com.zhihuminus.navigation.Follow.name -> Follow
                com.zhihuminus.navigation.Daily.name -> Daily
                else -> Home
            }
    }
}

@Serializable
data class InteractionSettings(
    val shareAction: ShareActionOption = ShareActionOption.Ask,
    val autoRefreshHomeOnStartup: Boolean = true,
    val showSearchHotSearch: Boolean = true,
    val showSearchHistory: Boolean = true,
)

@Serializable
enum class ShareActionOption(
    val id: String,
    val displayName: String,
) {
    Ask("ask", "询问"),
    CopyLink("copy", "复制链接"),
    SystemShare("share", "Android分享"),
    ;

    companion object {
        fun fromId(id: String?): ShareActionOption =
            entries.firstOrNull { it.id == id } ?: Ask
    }
}

private val defaultSystemNotifications: Map<NotificationType, Boolean> =
    NotificationType.entries.associateWith { false }

private val defaultDisplayInApp: Map<NotificationType, Boolean> =
    NotificationType.entries.associateWith { it.defaultValue }

@Serializable
data class NotificationSettings(
    val autoMarkAsRead: Boolean = true,
    val showUnreadBadge: Boolean = true,
    val systemNotifications: Map<NotificationType, Boolean> = defaultSystemNotifications,
    val displayInApp: Map<NotificationType, Boolean> = defaultDisplayInApp,
) {
    fun isSystemNotificationEnabled(type: NotificationType): Boolean =
        systemNotifications[type] ?: false

    fun isDisplayInAppEnabled(type: NotificationType): Boolean =
        displayInApp[type] ?: type.defaultValue
}

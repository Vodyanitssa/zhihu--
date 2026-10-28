package com.zhihuminus.feature.notification.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.zhihuminus.core.environment.AndroidPaginationEnvironment
import com.zhihuminus.core.platform.SystemNotificationPresenter
import com.zhihuminus.core.settings.AndroidAppSettingsRepository
import com.zhihuminus.core.state.UnreadNotificationState
import com.zhihuminus.core.util.Log
import com.zhihuminus.data.AccountData
import com.zhihuminus.data.zhihu.ZhihuRepositoryFactory
import com.zhihuminus.data.zhihu.api.ZhihuNotificationApi
import com.zhihuminus.feature.notification.NotificationType
import com.zhihuminus.feature.notification.matchNotificationType
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.router.APP_URL_SCHEME
import com.zhihuminus.navigation.router.toAppUrl

class NotificationSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!AccountData.data.login) {
            return Result.success()
        }

        val settingsRepo = AndroidAppSettingsRepository.getInstance(applicationContext)
        val notificationSettings = settingsRepo.current.notification
        val hasAnySystemNotification = NotificationType.entries.any {
            notificationSettings.isSystemNotificationEnabled(it)
        }
        if (!hasAnySystemNotification) {
            return Result.success()
        }

        if (!SystemNotificationPresenter.hasNotificationPermission(applicationContext)) {
            return Result.success()
        }

        return try {
            val environment = AndroidPaginationEnvironment(applicationContext)
            val api = ZhihuNotificationApi(environment)
            val repo = ZhihuRepositoryFactory(environment).createNotificationRepository()

            val me = api.getMeNotifications()
            val totalCount = me.totalCount
            UnreadNotificationState.update(totalCount)

            if (totalCount > 0) {
                val overview = repo.getNotificationOverview()

                if (notificationSettings.isSystemNotificationEnabled(NotificationType.INVITE_ANSWER)) {
                    val invitation = overview.invitation
                    if (invitation != null && invitation.unreadCount > 0) {
                        SystemNotificationPresenter.showNotification(
                            context = applicationContext,
                            id = "invite".hashCode(),
                            channelId = SystemNotificationPresenter.CHANNEL_INVITATIONS,
                            title = invitation.title.ifBlank { "邀请回答" },
                            content = listOf(invitation.textPrefix, invitation.text)
                                .filter { it.isNotBlank() }
                                .joinToString(" "),
                            deepLink = "$APP_URL_SCHEME://notification/invitations",
                        )
                    }
                }

                overview.items.filter { !it.isRead }.take(5).forEach { item ->
                    val content = item.content
                    val verb = listOf(content?.subTitle, content?.title, content?.text)
                        .firstOrNull { !it.isNullOrBlank() }
                        .orEmpty()
                    val type = matchNotificationType(verb)
                    val isMessage = item.type == "message" || item.navDestination() is Notification.Message

                    if (isMessage || (type != null && notificationSettings.isSystemNotificationEnabled(type))) {
                        val channelId = if (isMessage) {
                            SystemNotificationPresenter.CHANNEL_MESSAGES
                        } else {
                            SystemNotificationPresenter.CHANNEL_INTERACTIONS
                        }
                        val title = item.displayTitle()
                        val text = when {
                            content != null && content.abstractText.isNotBlank() -> content.abstractText
                            content != null && content.subTitle == "喜欢了你的评论" -> content.subText
                            else -> content?.text.orEmpty().ifBlank { verb }
                        }
                        val deepLink = item.navDestination()?.toAppUrl() ?: "$APP_URL_SCHEME://notification"

                        SystemNotificationPresenter.showNotification(
                            context = applicationContext,
                            id = item.stableId.hashCode(),
                            channelId = channelId,
                            title = title,
                            content = text,
                            deepLink = deepLink,
                        )
                    }
                }
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("NotificationSyncWorker", "Failed to sync notifications in background", e)
            Result.retry()
        }
    }
}

package com.zhihuminus.core.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.zhihuminus.MainActivity
import com.zhihuminus.R

object SystemNotificationPresenter {
    const val CHANNEL_INTERACTIONS = "zhihu_interactions"
    const val CHANNEL_MESSAGES = "zhihu_messages"
    const val CHANNEL_INVITATIONS = "zhihu_invitations"

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val interactionChannel = NotificationChannel(
                CHANNEL_INTERACTIONS,
                "互动消息",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "点赞、喜欢、回复与关注通知"
            }

            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "私信消息",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "私信会话新消息"
            }

            val invitationChannel = NotificationChannel(
                CHANNEL_INVITATIONS,
                "邀请回答",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "问题回答邀请通知"
            }

            notificationManager.createNotificationChannels(
                listOf(interactionChannel, messageChannel, invitationChannel),
            )
        }
    }

    fun hasNotificationPermission(context: Context): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showNotification(
        context: Context,
        id: Int,
        channelId: String,
        title: String,
        content: String,
        deepLink: String? = null,
    ) {
        if (!hasNotificationPermission(context)) return
        ensureNotificationChannels(context)

        val pendingIntent = deepLink?.let { link ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link), context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            PendingIntent.getActivity(
                context,
                id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        val priority = if (channelId == CHANNEL_MESSAGES) {
            NotificationCompat.PRIORITY_HIGH
        } else {
            NotificationCompat.PRIORITY_DEFAULT
        }

        val builder = NotificationCompat
            .Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_zhihuminus_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(priority)
            .setAutoCancel(true)

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent)
        }

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission revoked concurrently
        }
    }
}

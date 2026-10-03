package com.zhihuminus.feature.notification

import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.resolveContent
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant

enum class NotificationCategory(
    val entryName: String,
    val detailTitle: String,
) {
    Comment("comment", "评论转发@"),
    Like("like", "赞同喜欢"),
    Favorite("favlist_me", "收藏了我"),
    Follow("follow", "关注订阅"),
    ;

    companion object {
        fun fromEntryName(name: String): NotificationCategory? =
            entries.find { it.entryName == name }

        fun fromDetailTitle(title: String): NotificationCategory? =
            entries.find { it.detailTitle == title }
    }
}

data class NotificationOverviewResult(
    val unreadCounts: Map<NotificationCategory, Int> = emptyMap(),
    val invitation: NotificationColumnHead? = null,
    val items: List<NotificationTimelineItem> = emptyList(),
    val nextUrl: String? = null,
    val isEnd: Boolean = true,
)

data class NotificationTimelineResult(
    val items: List<NotificationTimelineItem> = emptyList(),
    val nextUrl: String? = null,
    val isEnd: Boolean = true,
)

data class NotificationColumnHead(
    val id: String = "",
    val title: String = "",
    val textPrefix: String = "",
    val text: String = "",
    val targetLink: String = "",
    val avatarUrls: List<NotificationImage> = emptyList(),
    val images: List<String> = emptyList(),
    val unreadCount: Int = 0,
    val created: Long = 0,
)

data class NotificationImage(
    val url: String = "",
    val nightUrl: String = "",
)

data class NotificationTimelineItem(
    val id: String = "",
    val uniqueId: String = "",
    val type: String = "",
    val cardType: String = "",
    val detailTitle: String = "",
    val isRead: Boolean = true,
    val unreadCount: Int = 0,
    val created: Long = 0,
    val createdStr: String = "",
    val head: NotificationHead? = null,
    val content: NotificationContent? = null,
    val targetSource: NotificationTargetSource? = null,
    val target: NotificationTarget? = null,
    val emptyInfo: NotificationEmptyInfo? = null,
    val additionalInfo: List<NotificationAdditionalInfo> = emptyList(),
) {
    val stableId: String
        get() = uniqueId.ifBlank { id.ifBlank { "$created-$cardType-$detailTitle" } }

    fun displayTitle(): String =
        content?.title?.takeIf { it.isNotBlank() }
            ?: detailTitle.takeIf { it.isNotBlank() }
            ?: target?.name?.takeIf { it.isNotBlank() }
            ?: "通知"

    fun displaySubtitle(): String {
        val subtitle = content?.subTitle?.takeIf { it.isNotBlank() }.orEmpty()
        if ((!subtitle.endsWith("：") && subtitle.startsWith("评论了")) ||
            subtitle.startsWith("赞同了") ||
            subtitle.startsWith("喜欢了")
        ) {
            return "$subtitle："
        }
        return subtitle
    }

    fun sourceText(): String =
        listOfNotNull(
            targetSource?.text?.takeIf { it.isNotBlank() },
            targetSource?.subText?.takeIf { it.isNotBlank() },
        ).joinToString("\n")

    fun avatarUrl(): String =
        head?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: head?.author?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: target?.avatarUrl?.takeIf { it.isNotBlank() }
            ?: content?.subIcon?.takeIf { it.isNotBlank() }
            ?: ""

    fun navDestination(): NavDestination? {
        val destinations = listOf(
            content?.targetLink,
            content?.subTargetLink,
            targetSource?.targetLink,
            head?.targetLink,
        ).mapNotNull { link ->
            link?.takeIf { it.isNotBlank() }?.let(::resolveContent)
        }
        val destination = destinations.firstOrNull { it is Notification.Message }
            ?: destinations.firstOrNull { it !is Person }
            ?: destinations.firstOrNull()
        return if (destination is Notification.Message) {
            destination.copy(
                name = destination.name.ifBlank { displayTitle() },
                avatarUrl = avatarUrl(),
            )
        } else {
            destination ?: target
                ?.takeIf { it.type == "people" && (it.urlToken.isNotBlank() || it.id.isNotBlank()) }
                ?.let {
                    Person(
                        id = it.id.ifBlank { Person.EMPTY_ID },
                        urlToken = it.urlToken,
                        name = it.name.ifBlank { "loading..." },
                    )
                }
        }
    }

    fun notificationListDate(): String {
        val epochSeconds = created.takeIf { it > 0 } ?: createdStr.toLongOrNull()
            ?: return createdStr
        val dateTime = Instant.fromEpochSeconds(epochSeconds).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dateTime.month.number.toString().padStart(2, '0')}-${dateTime.day.toString().padStart(2, '0')}"
    }

    fun invitationTime(): String {
        if (created <= 0) return ""
        val timeZone = TimeZone.currentSystemDefault()
        val dateTime = Instant.fromEpochSeconds(created).toLocalDateTime(timeZone)
        val today = Clock.System
            .now()
            .toLocalDateTime(timeZone)
            .date
        val date = dateTime.date
        return when {
            date == today -> "${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
            date.toEpochDays() == today.toEpochDays() - 1 -> "昨天"
            else -> "${date.month.number.toString().padStart(2, '0')}-${date.day.toString().padStart(2, '0')}"
        }
    }
}

data class NotificationHead(
    val author: NotificationAuthor? = null,
    val avatarUrl: String = "",
    val targetLink: String = "",
    val avatarUrls: List<String> = emptyList(),
    val labels: List<NotificationLabel> = emptyList(),
)

data class NotificationLabel(
    val text: String = "",
)

data class NotificationAuthor(
    val id: String? = null,
    val name: String = "",
    val urlToken: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val messageUserType: String = "",
    val messageUserSubType: String = "",
    val isForbidden: Boolean = false,
    val muteStatus: Boolean = false,
)

data class NotificationContent(
    val title: String = "",
    val subTitle: String = "",
    val text: String = "",
    val subText: String = "",
    val abstractText: String = "",
    val targetLink: String = "",
    val subTargetLink: String = "",
    val subIcon: String = "",
    val isDeleted: Boolean = false,
    val isSubDeleted: Boolean = false,
    val hasVideo: Boolean = false,
)

data class NotificationTargetSource(
    val topText: String = "",
    val subText: String = "",
    val text: String = "",
    val fullText: String = "",
    val targetLink: String = "",
    val isDeleted: Boolean = false,
    val hasVideo: Boolean = false,
    val image: String = "",
    val objectId: String = "",
    val subTextIsLink: Boolean = false,
)

data class NotificationTarget(
    val id: String = "",
    val type: String = "",
    val name: String = "",
    val title: String = "",
    val urlToken: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val url: String = "",
    val myAnswerUrl: String = "",
    val hasAnswer: Boolean = false,
    val followNum: Int = 0,
)

data class NotificationEmptyInfo(
    val text: String = "",
    val number: Int = 0,
)

data class NotificationAdditionalInfo(
    val id: Int = 0,
    val text: String = "",
    val icon: String = "",
)

data class PrivateMessage(
    val id: String = "",
    val type: String = "",
    val contentType: Int = 0,
    val content: String = "",
    val createdTime: Long = 0,
    val sender: NotificationAuthor? = null,
    val receiver: NotificationAuthor? = null,
    val plugin: PrivateMessagePlugin? = null,
) {
    val stableId: String
        get() = id.ifBlank { "$createdTime-${sender?.id}-${receiver?.id}" }
}

data class PrivateMessagePlugin(
    val pluginType: String = "",
    val pluginContent: String = "",
    val excerpt: String = "",
)

data class PrivateMessagePageResult(
    val items: List<PrivateMessage> = emptyList(),
    val nextUrl: String? = null,
    val isEnd: Boolean = true,
)

interface NotificationRepository {
    suspend fun getNotificationOverview(nextUrl: String? = null): NotificationOverviewResult

    suspend fun getNotificationTimeline(entryName: String, nextUrl: String? = null): NotificationTimelineResult

    suspend fun markCategoryRead(entryName: String): Boolean

    suspend fun markAllRead(): Boolean

    suspend fun getPrivateMessages(peerId: String, nextUrl: String? = null): PrivateMessagePageResult

    suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthor?

    suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessage
}

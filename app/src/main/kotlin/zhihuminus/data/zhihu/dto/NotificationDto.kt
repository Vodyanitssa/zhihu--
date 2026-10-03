package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.ZhihuPaging
import kotlinx.serialization.Serializable

@Serializable
data class NotificationOverviewDto(
    val head: List<NotificationHeadEntryDto> = emptyList(),
    val columnHead: List<NotificationColumnHeadDto> = emptyList(),
    val data: List<NotificationTimelineItemDto> = emptyList(),
    val paging: ZhihuPaging? = null,
)

@Serializable
data class NotificationHeadEntryDto(
    val entryName: String = "",
    val detailTitle: String = "",
    val unreadCount: Int = 0,
)

@Serializable
data class NotificationColumnHeadDto(
    val id: String = "",
    val title: String = "",
    val textPrefix: String = "",
    val text: String = "",
    val targetLink: String = "",
    val avatarUrls: List<NotificationImageDto> = emptyList(),
    val images: List<String> = emptyList(),
    val unreadCount: Int = 0,
    val created: Long = 0,
)

@Serializable
data class NotificationImageDto(
    val url: String = "",
    val nightUrl: String = "",
)

@Serializable
data class NotificationTimelineItemDto(
    val id: String = "",
    val uniqueId: String = "",
    val type: String = "",
    val cardType: String = "",
    val detailTitle: String = "",
    val isRead: Boolean = true,
    val unreadCount: Int = 0,
    val created: Long = 0,
    val createdStr: String = "",
    val head: NotificationHeadDto? = null,
    val content: NotificationContentDto? = null,
    val targetSource: NotificationTargetSourceDto? = null,
    val target: NotificationTargetDto? = null,
    val emptyInfo: NotificationEmptyInfoDto? = null,
    val additionalInfo: List<NotificationAdditionalInfoDto> = emptyList(),
)

@Serializable
data class NotificationHeadDto(
    val author: NotificationAuthorDto? = null,
    val avatarUrl: String = "",
    val targetLink: String = "",
    val avatarUrls: List<String> = emptyList(),
    val labels: List<NotificationLabelDto> = emptyList(),
)

@Serializable
data class NotificationLabelDto(
    val text: String = "",
)

@Serializable
data class NotificationAuthorDto(
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

@Serializable
data class NotificationContentDto(
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

@Serializable
data class NotificationTargetSourceDto(
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

@Serializable
data class NotificationTargetDto(
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

@Serializable
data class NotificationEmptyInfoDto(
    val text: String = "",
    val number: Int = 0,
)

@Serializable
data class NotificationAdditionalInfoDto(
    val id: Int = 0,
    val text: String = "",
    val icon: String = "",
)

@Serializable
data class ZhihuMeNotificationsDto(
    val defaultNotificationsCount: Int = 0,
    val followNotificationsCount: Int = 0,
    val voteThankNotificationsCount: Int = 0,
) {
    val totalCount: Int get() = defaultNotificationsCount + followNotificationsCount + voteThankNotificationsCount
}

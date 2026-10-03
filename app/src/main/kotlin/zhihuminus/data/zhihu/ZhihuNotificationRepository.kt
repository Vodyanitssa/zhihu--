package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.dto.NotificationAdditionalInfoDto
import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationColumnHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationContentDto
import com.zhihuminus.data.zhihu.dto.NotificationEmptyInfoDto
import com.zhihuminus.data.zhihu.dto.NotificationHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationImageDto
import com.zhihuminus.data.zhihu.dto.NotificationLabelDto
import com.zhihuminus.data.zhihu.dto.NotificationTargetDto
import com.zhihuminus.data.zhihu.dto.NotificationTargetSourceDto
import com.zhihuminus.data.zhihu.dto.NotificationTimelineItemDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePluginDto
import com.zhihuminus.feature.notification.NotificationAdditionalInfo
import com.zhihuminus.feature.notification.NotificationAuthor
import com.zhihuminus.feature.notification.NotificationCategory
import com.zhihuminus.feature.notification.NotificationColumnHead
import com.zhihuminus.feature.notification.NotificationContent
import com.zhihuminus.feature.notification.NotificationEmptyInfo
import com.zhihuminus.feature.notification.NotificationHead
import com.zhihuminus.feature.notification.NotificationImage
import com.zhihuminus.feature.notification.NotificationLabel
import com.zhihuminus.feature.notification.NotificationOverviewResult
import com.zhihuminus.feature.notification.NotificationRepository
import com.zhihuminus.feature.notification.NotificationTarget
import com.zhihuminus.feature.notification.NotificationTargetSource
import com.zhihuminus.feature.notification.NotificationTimelineItem
import com.zhihuminus.feature.notification.NotificationTimelineResult
import com.zhihuminus.feature.notification.PrivateMessage
import com.zhihuminus.feature.notification.PrivateMessagePageResult
import com.zhihuminus.feature.notification.PrivateMessagePlugin

class ZhihuNotificationRepository(
    private val api: ZhihuApi,
) : NotificationRepository {
    override suspend fun getNotificationOverview(nextUrl: String?): NotificationOverviewResult {
        val dto = api.getNotificationOverview(nextUrl)
        val unreadCounts = mutableMapOf<NotificationCategory, Int>()
        NotificationCategory.entries.forEach { category ->
            unreadCounts[category] = dto.head
                .firstOrNull { it.detailTitle == category.detailTitle || it.entryName == category.entryName }
                ?.unreadCount ?: 0
        }
        val invitation = dto.columnHead.firstOrNull()?.toDomain()
        val items = dto.data
            .filter { it.type != "empty" }
            .map { it.toDomain() }

        val isEnd = dto.paging?.isEnd == true || dto.paging?.next.isNullOrBlank()
        return NotificationOverviewResult(
            unreadCounts = unreadCounts,
            invitation = invitation,
            items = items,
            nextUrl = dto.paging?.next?.takeIf { !isEnd },
            isEnd = isEnd,
        )
    }

    override suspend fun getNotificationTimeline(entryName: String, nextUrl: String?): NotificationTimelineResult {
        val dto = api.getNotificationTimeline(entryName, nextUrl)
        val items = dto.data.map { it.toDomain() }
        val isEnd = dto.paging?.isEnd == true || dto.paging?.next.isNullOrBlank()
        return NotificationTimelineResult(
            items = items,
            nextUrl = dto.paging?.next?.takeIf { !isEnd },
            isEnd = isEnd,
        )
    }

    override suspend fun markCategoryRead(entryName: String): Boolean =
        api.markNotificationCategoryRead(entryName)

    override suspend fun markAllRead(): Boolean {
        var allSuccess = true
        for (category in NotificationCategory.entries) {
            val success = markCategoryRead(category.entryName)
            if (!success) {
                allSuccess = false
            }
        }
        runCatching { markCategoryRead("invite") }
        return allSuccess
    }

    override suspend fun getPrivateMessages(peerId: String, nextUrl: String?): PrivateMessagePageResult {
        val dto = api.getPrivateMessages(peerId, nextUrl)
        val items = dto.data.map { it.toDomain() }
        val isEnd = dto.paging.isEnd || dto.paging.next.isBlank()
        return PrivateMessagePageResult(
            items = items,
            nextUrl = dto.paging.next.takeIf { !isEnd },
            isEnd = isEnd,
        )
    }

    override suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthor? =
        runCatching { api.getPrivateMessagePeer(peerId).toDomain() }.getOrNull()

    override suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessage =
        api.sendPrivateMessage(peerId, content).toDomain()
}

fun NotificationColumnHeadDto.toDomain(): NotificationColumnHead = NotificationColumnHead(
    id = id,
    title = title,
    textPrefix = textPrefix,
    text = text,
    targetLink = targetLink,
    avatarUrls = avatarUrls.map { it.toDomain() },
    images = images,
    unreadCount = unreadCount,
    created = created,
)

fun NotificationImageDto.toDomain(): NotificationImage = NotificationImage(
    url = url,
    nightUrl = nightUrl,
)

fun NotificationTimelineItemDto.toDomain(): NotificationTimelineItem = NotificationTimelineItem(
    id = id,
    uniqueId = uniqueId,
    type = type,
    cardType = cardType,
    detailTitle = detailTitle,
    isRead = isRead,
    unreadCount = unreadCount,
    created = created,
    createdStr = createdStr,
    head = head?.toDomain(),
    content = content?.toDomain(),
    targetSource = targetSource?.toDomain(),
    target = target?.toDomain(),
    emptyInfo = emptyInfo?.toDomain(),
    additionalInfo = additionalInfo.map { it.toDomain() },
)

fun NotificationHeadDto.toDomain(): NotificationHead = NotificationHead(
    author = author?.toDomain(),
    avatarUrl = avatarUrl,
    targetLink = targetLink,
    avatarUrls = avatarUrls,
    labels = labels.map { it.toDomain() },
)

fun NotificationLabelDto.toDomain(): NotificationLabel = NotificationLabel(text = text)

fun NotificationAuthorDto.toDomain(): NotificationAuthor = NotificationAuthor(
    id = id,
    name = name,
    urlToken = urlToken,
    headline = headline,
    avatarUrl = avatarUrl,
    messageUserType = messageUserType,
    messageUserSubType = messageUserSubType,
    isForbidden = isForbidden,
    muteStatus = muteStatus,
)

fun NotificationContentDto.toDomain(): NotificationContent = NotificationContent(
    title = title,
    subTitle = subTitle,
    text = text,
    subText = subText,
    abstractText = abstractText,
    targetLink = targetLink,
    subTargetLink = subTargetLink,
    subIcon = subIcon,
    isDeleted = isDeleted,
    isSubDeleted = isSubDeleted,
    hasVideo = hasVideo,
)

fun NotificationTargetSourceDto.toDomain(): NotificationTargetSource = NotificationTargetSource(
    topText = topText,
    subText = subText,
    text = text,
    fullText = fullText,
    targetLink = targetLink,
    isDeleted = isDeleted,
    hasVideo = hasVideo,
    image = image,
    objectId = objectId,
    subTextIsLink = subTextIsLink,
)

fun NotificationTargetDto.toDomain(): NotificationTarget = NotificationTarget(
    id = id,
    type = type,
    name = name,
    title = title,
    urlToken = urlToken,
    headline = headline,
    avatarUrl = avatarUrl,
    url = url,
    myAnswerUrl = myAnswerUrl,
    hasAnswer = hasAnswer,
    followNum = followNum,
)

fun NotificationEmptyInfoDto.toDomain(): NotificationEmptyInfo = NotificationEmptyInfo(
    text = text,
    number = number,
)

fun NotificationAdditionalInfoDto.toDomain(): NotificationAdditionalInfo = NotificationAdditionalInfo(
    id = id,
    text = text,
    icon = icon,
)

fun PrivateMessageDto.toDomain(): PrivateMessage = PrivateMessage(
    id = id,
    type = type,
    contentType = contentType,
    content = content,
    createdTime = createdTime,
    sender = sender?.toDomain(),
    receiver = receiver?.toDomain(),
    plugin = plugin?.toDomain(),
)

fun PrivateMessagePluginDto.toDomain(): PrivateMessagePlugin = PrivateMessagePlugin(
    pluginType = pluginType,
    pluginContent = pluginContent,
    excerpt = excerpt,
)

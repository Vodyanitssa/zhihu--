package com.zhihuminus.data.zhihu.mapper

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
import com.zhihuminus.feature.notification.NotificationColumnHead
import com.zhihuminus.feature.notification.NotificationContent
import com.zhihuminus.feature.notification.NotificationEmptyInfo
import com.zhihuminus.feature.notification.NotificationHead
import com.zhihuminus.feature.notification.NotificationImage
import com.zhihuminus.feature.notification.NotificationLabel
import com.zhihuminus.feature.notification.NotificationTarget
import com.zhihuminus.feature.notification.NotificationTargetSource
import com.zhihuminus.feature.notification.NotificationTimelineItem
import com.zhihuminus.feature.notification.PrivateMessage
import com.zhihuminus.feature.notification.PrivateMessagePlugin

/**
 * 知乎通知（时间线卡片、专栏头）及私信数据模型映射器。
 */
object NotificationMapper {
    fun toColumnHead(dto: NotificationColumnHeadDto): NotificationColumnHead = NotificationColumnHead(
        id = dto.id,
        title = dto.title,
        textPrefix = dto.textPrefix,
        text = dto.text,
        targetLink = dto.targetLink,
        avatarUrls = dto.avatarUrls.map { toImage(it) },
        images = dto.images,
        unreadCount = dto.unreadCount,
        created = dto.created,
    )

    fun toImage(dto: NotificationImageDto): NotificationImage = NotificationImage(
        url = dto.url,
        nightUrl = dto.nightUrl,
    )

    fun toTimelineItem(dto: NotificationTimelineItemDto): NotificationTimelineItem = NotificationTimelineItem(
        id = dto.id,
        uniqueId = dto.uniqueId,
        type = dto.type,
        cardType = dto.cardType,
        detailTitle = dto.detailTitle,
        isRead = dto.isRead,
        unreadCount = dto.unreadCount,
        created = dto.created,
        createdStr = dto.createdStr,
        head = dto.head?.let { toHead(it) },
        content = dto.content?.let { toContent(it) },
        targetSource = dto.targetSource?.let { toTargetSource(it) },
        target = dto.target?.let { toTarget(it) },
        emptyInfo = dto.emptyInfo?.let { toEmptyInfo(it) },
        additionalInfo = dto.additionalInfo.map { toAdditionalInfo(it) },
    )

    fun toHead(dto: NotificationHeadDto): NotificationHead = NotificationHead(
        author = dto.author?.let { toAuthor(it) },
        avatarUrl = dto.avatarUrl,
        targetLink = dto.targetLink,
        avatarUrls = dto.avatarUrls,
        labels = dto.labels.map { toLabel(it) },
    )

    fun toLabel(dto: NotificationLabelDto): NotificationLabel = NotificationLabel(text = dto.text)

    fun toAuthor(dto: NotificationAuthorDto): NotificationAuthor = NotificationAuthor(
        id = dto.id,
        name = dto.name,
        urlToken = dto.urlToken,
        headline = dto.headline,
        avatarUrl = dto.avatarUrl,
        messageUserType = dto.messageUserType,
        messageUserSubType = dto.messageUserSubType,
        isForbidden = dto.isForbidden,
        muteStatus = dto.muteStatus,
    )

    fun toContent(dto: NotificationContentDto): NotificationContent = NotificationContent(
        title = dto.title,
        subTitle = dto.subTitle,
        text = dto.text,
        subText = dto.subText,
        abstractText = dto.abstractText,
        targetLink = dto.targetLink,
        subTargetLink = dto.subTargetLink,
        subIcon = dto.subIcon,
        isDeleted = dto.isDeleted,
        isSubDeleted = dto.isSubDeleted,
        hasVideo = dto.hasVideo,
    )

    fun toTargetSource(dto: NotificationTargetSourceDto): NotificationTargetSource = NotificationTargetSource(
        topText = dto.topText,
        subText = dto.subText,
        text = dto.text,
        fullText = dto.fullText,
        targetLink = dto.targetLink,
        isDeleted = dto.isDeleted,
        hasVideo = dto.hasVideo,
        image = dto.image,
        objectId = dto.objectId,
        subTextIsLink = dto.subTextIsLink,
    )

    fun toTarget(dto: NotificationTargetDto): NotificationTarget = NotificationTarget(
        id = dto.id,
        type = dto.type,
        name = dto.name,
        title = dto.title,
        urlToken = dto.urlToken,
        headline = dto.headline,
        avatarUrl = dto.avatarUrl,
        url = dto.url,
        myAnswerUrl = dto.myAnswerUrl,
        hasAnswer = dto.hasAnswer,
        followNum = dto.followNum,
    )

    fun toEmptyInfo(dto: NotificationEmptyInfoDto): NotificationEmptyInfo = NotificationEmptyInfo(
        text = dto.text,
        number = dto.number,
    )

    fun toAdditionalInfo(dto: NotificationAdditionalInfoDto): NotificationAdditionalInfo = NotificationAdditionalInfo(
        id = dto.id,
        text = dto.text,
        icon = dto.icon,
    )

    fun toPrivateMessage(dto: PrivateMessageDto): PrivateMessage = PrivateMessage(
        id = dto.id,
        type = dto.type,
        contentType = dto.contentType,
        content = dto.content,
        createdTime = dto.createdTime,
        sender = dto.sender?.let { toAuthor(it) },
        receiver = dto.receiver?.let { toAuthor(it) },
        plugin = dto.plugin?.let { toPrivateMessagePlugin(it) },
    )

    fun toPrivateMessagePlugin(dto: PrivateMessagePluginDto): PrivateMessagePlugin = PrivateMessagePlugin(
        pluginType = dto.pluginType,
        pluginContent = dto.pluginContent,
        excerpt = dto.excerpt,
    )
}

fun NotificationColumnHeadDto.toDomain(): NotificationColumnHead = NotificationMapper.toColumnHead(this)

fun NotificationImageDto.toDomain(): NotificationImage = NotificationMapper.toImage(this)

fun NotificationTimelineItemDto.toDomain(): NotificationTimelineItem = NotificationMapper.toTimelineItem(this)

fun NotificationHeadDto.toDomain(): NotificationHead = NotificationMapper.toHead(this)

fun NotificationLabelDto.toDomain(): NotificationLabel = NotificationMapper.toLabel(this)

fun NotificationAuthorDto.toDomain(): NotificationAuthor = NotificationMapper.toAuthor(this)

fun NotificationContentDto.toDomain(): NotificationContent = NotificationMapper.toContent(this)

fun NotificationTargetSourceDto.toDomain(): NotificationTargetSource = NotificationMapper.toTargetSource(this)

fun NotificationTargetDto.toDomain(): NotificationTarget = NotificationMapper.toTarget(this)

fun NotificationEmptyInfoDto.toDomain(): NotificationEmptyInfo = NotificationMapper.toEmptyInfo(this)

fun NotificationAdditionalInfoDto.toDomain(): NotificationAdditionalInfo = NotificationMapper.toAdditionalInfo(this)

fun PrivateMessageDto.toDomain(): PrivateMessage = NotificationMapper.toPrivateMessage(this)

fun PrivateMessagePluginDto.toDomain(): PrivateMessagePlugin = NotificationMapper.toPrivateMessagePlugin(this)

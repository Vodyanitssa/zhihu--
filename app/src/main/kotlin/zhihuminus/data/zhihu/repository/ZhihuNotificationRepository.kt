package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuNotificationApi
import com.zhihuminus.data.zhihu.mapper.toDomain
import com.zhihuminus.feature.notification.NotificationAuthor
import com.zhihuminus.feature.notification.NotificationCategory
import com.zhihuminus.feature.notification.NotificationOverviewResult
import com.zhihuminus.feature.notification.NotificationRepository
import com.zhihuminus.feature.notification.NotificationTimelineResult
import com.zhihuminus.feature.notification.PrivateMessage
import com.zhihuminus.feature.notification.PrivateMessagePageResult

class ZhihuNotificationRepository(
    private val api: ZhihuNotificationApi,
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

        val isEnd = dto.paging?.hasMore != true
        return NotificationOverviewResult(
            unreadCounts = unreadCounts,
            invitation = invitation,
            items = items,
            nextUrl = dto.paging?.nextUrl,
            isEnd = isEnd,
        )
    }

    override suspend fun getNotificationTimeline(entryName: String, nextUrl: String?): NotificationTimelineResult {
        val dto = api.getNotificationTimeline(entryName, nextUrl)
        val items = dto.data.map { it.toDomain() }
        val isEnd = dto.paging?.hasMore != true
        return NotificationTimelineResult(
            items = items,
            nextUrl = dto.paging?.nextUrl,
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
        val isEnd = !dto.paging.hasMore
        return PrivateMessagePageResult(
            items = items,
            nextUrl = dto.paging.nextUrl,
            isEnd = isEnd,
        )
    }

    override suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthor? =
        runCatching { api.getPrivateMessagePeer(peerId).toDomain() }.getOrNull()

    override suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessage =
        api.sendPrivateMessage(peerId, content).toDomain()
}

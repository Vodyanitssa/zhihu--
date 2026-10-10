package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationOverviewDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePageDto
import com.zhihuminus.data.zhihu.dto.ZhihuMeNotificationsDto

interface ZhihuNotificationApi {
    /**
     * 获取通知中心概览（分类未读数、邀请卡片、会话列表）
     * @param nextUrl 分页续页 URL，为 null 时从首页加载
     */
    suspend fun getNotificationOverview(nextUrl: String? = null): NotificationOverviewDto

    /**
     * 获取指定分类的时间线通知列表
     * @param entryName 分类标识（comment, like, favlist_me, follow, invite 等）
     * @param nextUrl 分页续页 URL，为 null 时从首页加载
     */
    suspend fun getNotificationTimeline(entryName: String, nextUrl: String? = null): NotificationOverviewDto

    /**
     * 将指定分类的通知全部标记为已读
     * @param entryName 分类标识
     */
    suspend fun markNotificationCategoryRead(entryName: String): Boolean

    /**
     * 获取与特定用户的私信会话记录
     * @param peerId 对方用户 ID
     * @param nextUrl 分页续页 URL
     */
    suspend fun getPrivateMessages(peerId: String, nextUrl: String? = null): PrivateMessagePageDto

    /**
     * 获取私信对话方的用户信息
     * @param peerId 对方用户 ID
     */
    suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthorDto

    /**
     * 发送私信
     * @param peerId 接收方用户 ID
     * @param content 私信内容
     */
    suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessageDto

    /**
     * 获取个人未读通知数汇总（用于首页红点）
     */
    suspend fun getMeNotifications(): ZhihuMeNotificationsDto
}

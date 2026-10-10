package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.api.MOMENTS_FEED_URL
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.feature.follow.FollowFeedPage
import com.zhihuminus.feature.follow.FollowRepository
import com.zhihuminus.feature.follow.FollowingUser

class ZhihuFollowRepository(
    private val api: ZhihuFeedApi,
    private val postCache: PostContentCache = PostContentCache,
) : FollowRepository {
    override suspend fun fetchFollowFeed(nextUrl: String?): FollowFeedPage {
        val feedPage = api.fetchFeedPage(url = nextUrl ?: MOMENTS_FEED_URL, include = "")
        feedPage.items.forEach { feed ->
            feed.target?.let { postCache.putFromFeed(it) }
        }
        val displayItems = feedPage.items.flattenFeeds().map { feed ->
            val item = feed.toDisplayItem()
            val target = feed.target
            if (feed.sourceLabel != null && target?.detailsText != null) {
                item.copy(details = target.detailsText)
            } else {
                item
            }
        }
        return FollowFeedPage(
            items = displayItems,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd || feedPage.nextUrl == null,
        )
    }

    override suspend fun fetchRecentFollowingUsers(): List<FollowingUser> = api.getRecentFollowingUsers().map { dto ->
        FollowingUser(
            id = dto.actor.id,
            urlToken = dto.actor.urlToken,
            name = dto.actor.name,
            avatarUrl = dto.actor.avatarUrl,
            unreadCount = dto.unreadCount,
        )
    }
}

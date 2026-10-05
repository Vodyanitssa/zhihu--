package com.zhihuminus.data.zhihu

import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.sourceLabel
import com.zhihuminus.data.target
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.feature.follow.FollowFeedPage
import com.zhihuminus.feature.follow.FollowRepository
import com.zhihuminus.feature.follow.FollowingUser

class ZhihuFollowRepository(
    private val api: ZhihuApi,
) : FollowRepository {
    override suspend fun fetchFollowFeed(nextUrl: String?): FollowFeedPage {
        val feedPage = api.fetchFeedPage(url = nextUrl ?: MOMENTS_FEED_URL, include = "")
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

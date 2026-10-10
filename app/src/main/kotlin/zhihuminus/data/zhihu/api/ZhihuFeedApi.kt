package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowingUserItemDto

const val FEED_INCLUDE = "data[*].content,excerpt,headline,target.author.badge_v2"

interface ZhihuFeedApi {
    /**
     * 按完整 URL 拉取一页 feed 条目（问题回答流等），并解析续页游标。
     * @param include feed 字段 include 表达式，空串表示不传（桌面推荐流默认返回全量字段）
     */
    suspend fun fetchFeedPage(
        url: String,
        include: String = FEED_INCLUDE,
    ): FeedPage

    /**
     * 获取最近有动态的已关注用户列表。
     */
    suspend fun getRecentFollowingUsers(): List<FollowingUserItemDto>
}

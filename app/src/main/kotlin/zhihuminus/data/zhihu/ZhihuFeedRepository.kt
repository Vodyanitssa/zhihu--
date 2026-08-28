package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.dto.FeedPage

/**
 * 推荐流/关注流仓库；底层是桌面 Web v3 接口——无需 include 即返回完整 target.content。
 * 推荐流附带 reaction（赞同态）等字段；关注流 target 无 reaction（faved 未知）。
 */
class ZhihuFeedRepository(
    private val api: ZhihuApi,
) {
    /**
     * 拉取一页推荐流。
     * @param url 续页 URL（上一页响应的 paging.next）；null 表示请求第一页
     */
    suspend fun fetchRecommendFeedPage(url: String? = null): FeedPage =
        api.fetchFeedPage(url ?: RECOMMEND_FEED_URL, include = "")

    /**
     * 拉取一页关注流（动态子页）。
     * @param url 续页 URL（上一页响应的 paging.next）；null 表示请求第一页
     */
    suspend fun fetchMomentsFeedPage(url: String? = null): FeedPage =
        api.fetchFeedPage(url ?: MOMENTS_FEED_URL, include = "")
}

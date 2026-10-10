package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.HotSearchItemDto
import com.zhihuminus.data.zhihu.dto.SearchResponseDto

interface ZhihuSearchApi {
    /**
     * 搜索知乎内容/用户/话题
     *
     * @param query 搜索关键词
     * @param tab 搜索 Tab: general, people, topic
     * @param sort 排序方式
     * @param vertical 内容类型
     * @param timeInterval 时间范围
     * @param restrictedMemberHashId 限定用户 hash ID
     * @param nextUrl 分页续页 URL
     */
    suspend fun search(
        query: String,
        tab: String,
        sort: String = "",
        vertical: String = "",
        timeInterval: String = "",
        restrictedMemberHashId: String = "",
        nextUrl: String? = null,
    ): SearchResponseDto

    /**
     * 按完整 URL 加载下一页搜索结果
     */
    suspend fun fetchSearchPage(url: String): SearchResponseDto

    /**
     * 获取热搜词条列表
     */
    suspend fun getHotSearches(): List<HotSearchItemDto>

    /**
     * 关注/取消关注话题
     */
    suspend fun followTopic(topicId: String, follow: Boolean)

    /**
     * 关注/取消关注用户
     */
    suspend fun followMember(urlToken: String, follow: Boolean)
}

package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse

interface ZhihuDailyApi {
    /**
     * 获取最新一期知乎日报
     */
    suspend fun getDailyLatest(): DailyStoriesResponse

    /**
     * 获取指定日期之前的知乎日报
     * @param date 日期字符串，格式 yyyyMMdd
     */
    suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse
}

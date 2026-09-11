package com.zhihuminus.data.zhihu

import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.feature.daily.DailyRepository

class ZhihuDailyRepository(
    private val api: ZhihuApi,
) : DailyRepository {
    override suspend fun getLatestDaily(): DailyStoriesResponse =
        api.getDailyLatest()

    override suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse =
        api.getDailyStoriesBefore(date)
}

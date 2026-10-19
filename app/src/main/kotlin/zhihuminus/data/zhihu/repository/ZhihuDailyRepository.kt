package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuDailyApi
import com.zhihuminus.data.zhihu.mapper.toDailyStoriesResult
import com.zhihuminus.feature.daily.DailyRepository
import com.zhihuminus.feature.daily.DailyStoriesResult

class ZhihuDailyRepository(
    private val api: ZhihuDailyApi,
) : DailyRepository {
    override suspend fun getLatestDaily(): DailyStoriesResult =
        api.getDailyLatest().toDailyStoriesResult()

    override suspend fun getDailyStoriesBefore(date: String): DailyStoriesResult =
        api.getDailyStoriesBefore(date).toDailyStoriesResult()
}

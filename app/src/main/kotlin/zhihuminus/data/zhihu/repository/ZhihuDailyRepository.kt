package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuDailyApi
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.DailyStoryDto
import com.zhihuminus.feature.daily.DailyRepository
import com.zhihuminus.feature.daily.DailyStoriesResult
import com.zhihuminus.feature.daily.DailyStory

class ZhihuDailyRepository(
    private val api: ZhihuDailyApi,
) : DailyRepository {
    override suspend fun getLatestDaily(): DailyStoriesResult =
        api.getDailyLatest().toDomain()

    override suspend fun getDailyStoriesBefore(date: String): DailyStoriesResult =
        api.getDailyStoriesBefore(date).toDomain()
}

private fun DailyStoryDto.toDomain() = DailyStory(
    id = id,
    title = title,
    url = url,
    hint = hint,
    images = images,
    type = type,
)

private fun DailyStoriesResponse.toDomain() = DailyStoriesResult(
    date = date,
    stories = stories.map { it.toDomain() },
)

package com.zhihuminus.feature.daily

import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.DailyStoryDto

data class DailySection(
    val date: String,
    val stories: List<DailyStoryDto>,
)

interface DailyRepository {
    suspend fun getLatestDaily(): DailyStoriesResponse

    suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse
}

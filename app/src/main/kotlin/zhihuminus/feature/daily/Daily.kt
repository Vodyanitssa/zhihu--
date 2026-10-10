package com.zhihuminus.feature.daily

data class DailyStory(
    val id: Long,
    val title: String,
    val url: String,
    val hint: String,
    val images: List<String>,
    val type: Int,
)

data class DailyStoriesResult(
    val date: String,
    val stories: List<DailyStory>,
)

data class DailySection(
    val date: String,
    val stories: List<DailyStory>,
)

interface DailyRepository {
    suspend fun getLatestDaily(): DailyStoriesResult

    suspend fun getDailyStoriesBefore(date: String): DailyStoriesResult
}

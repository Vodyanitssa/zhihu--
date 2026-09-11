package com.zhihuminus.data.zhihu.dto

import kotlinx.serialization.Serializable

@Serializable
data class DailyStoriesResponse(
    val date: String,
    val stories: List<DailyStoryDto>,
)

@Serializable
data class DailyStoryDto(
    val id: Long,
    val title: String,
    val url: String,
    val hint: String,
    val images: List<String>,
    val type: Int,
)

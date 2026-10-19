package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.DailyStoryDto
import com.zhihuminus.feature.daily.DailyStoriesResult
import com.zhihuminus.feature.daily.DailyStory

/**
 * 知乎日报数据模型映射器。
 */
object DailyMapper {
    fun toDailyStory(dto: DailyStoryDto): DailyStory = DailyStory(
        id = dto.id,
        title = dto.title,
        url = dto.url,
        hint = dto.hint,
        images = dto.images,
        type = dto.type,
    )

    fun toDailyStoriesResult(dto: DailyStoriesResponse): DailyStoriesResult = DailyStoriesResult(
        date = dto.date,
        stories = dto.stories.map { toDailyStory(it) },
    )
}

fun DailyStoryDto.toDailyStory(): DailyStory = DailyMapper.toDailyStory(this)

fun DailyStoriesResponse.toDailyStoriesResult(): DailyStoriesResult = DailyMapper.toDailyStoriesResult(this)

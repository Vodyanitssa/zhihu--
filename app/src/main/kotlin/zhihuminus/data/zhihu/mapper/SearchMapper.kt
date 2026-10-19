package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.HotSearchItemDto
import com.zhihuminus.data.zhihu.dto.TopicSearchDto
import com.zhihuminus.feature.search.HotSearchItem
import com.zhihuminus.feature.search.SearchTopicItem
import com.zhihuminus.feature.search.TopicSearchResult
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * 知乎搜索数据模型映射器。
 */
object SearchMapper {
    fun toHotSearchItem(dto: HotSearchItemDto): HotSearchItem = HotSearchItem(
        query = dto.query,
        hotShow = dto.hotShow,
        label = dto.label,
    )

    fun decodeTopicSearchResult(obj: JsonElement?): TopicSearchResult? {
        val objectJson = obj as? JsonObject ?: return null
        val decoded = runCatching { ZhihuJson.decodeJson<TopicSearchDto>(objectJson) }.getOrNull() ?: return null
        if (decoded.type != "topic") return null
        return TopicSearchResult(
            topic = SearchTopicItem(
                id = decoded.id,
                name = decoded.name.replace("<em>", "").replace("</em>", ""),
                avatarUrl = decoded.avatarUrl,
                topicType = decoded.topicType,
                url = decoded.url,
            ),
            excerpt = decoded.excerpt.replace("<em>", "").replace("</em>", ""),
            visitCount = decoded.visitCount,
            discussCount = decoded.topAnswerCount,
            isFollowing = decoded.isFollowing,
        )
    }
}

fun HotSearchItemDto.toHotSearchItem(): HotSearchItem = SearchMapper.toHotSearchItem(this)

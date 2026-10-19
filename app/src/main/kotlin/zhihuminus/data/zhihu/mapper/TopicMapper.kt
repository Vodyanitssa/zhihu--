package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.dto.TopicPinFeedDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import kotlinx.serialization.json.JsonElement

/**
 * 知乎话题数据模型映射器。
 */
object TopicMapper {
    fun decodeTopicPinFeeds(items: List<JsonElement>): List<FeedDisplayItem> =
        items.mapNotNull { element ->
            runCatching { ZhihuJson.decodeJson<TopicPinFeedDto>(element) }.getOrNull()?.target?.let { target ->
                val pinId = target.id.content.toLongOrNull() ?: return@let null
                FeedDisplayItem(
                    title = target.title.ifBlank { "想法" },
                    summary = target.plainContent
                        .ifBlank { target.excerpt.ifBlank { target.content } }
                        .takeIf(String::isNotBlank),
                    details = "想法 · ${target.counter.applaud} 赞 · ${target.counter.comment} 评论",
                    feed = null,
                    navDestinationJson = PostDestination(
                        type = PostType.Pin,
                        id = pinId,
                    ).toFeedDisplayItemNavDestinationJson(),
                    avatarSrc = target.author.avatarUrl,
                    authorName = target.author.name,
                    contentTypeLabel = "想法",
                )
            }
        }
}

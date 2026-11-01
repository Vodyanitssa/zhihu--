package com.zhihuminus.feature.watchlater

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class WatchLaterItem(
    val id: Long,
    val type: String,
    val title: String,
    val excerpt: String = "",
    val authorName: String? = null,
    val authorAvatar: String? = null,
    val coverImage: String? = null,
    val addedAt: Long = 0L,
) {
    val actionUrl: String
        get() = when (type.lowercase()) {
            "answer" -> "zhihu://answers/$id"
            "article" -> "zhihu://articles/$id"
            "pin" -> "zhihu://pin/$id"
            else -> "zhihu://$type/$id"
        }

    val contentTypeLabel: String
        get() = when (type.lowercase()) {
            "answer" -> "回答"
            "article" -> "文章"
            "pin" -> "想法"
            else -> "内容"
        }

    fun toDestination(): PostDestination {
        val postType = when (type.lowercase()) {
            "article" -> PostType.Article
            "pin" -> PostType.Pin
            else -> PostType.Answer
        }
        return PostDestination(
            id = id,
            type = postType,
            title = title,
            authorName = authorName.orEmpty(),
            avatarSrc = authorAvatar,
            excerpt = excerpt,
            readingQueueSourceId = "watch_later",
        )
    }
}

interface WatchLaterRepository {
    val itemsFlow: StateFlow<List<WatchLaterItem>>

    suspend fun getItems(): List<WatchLaterItem>

    suspend fun addItem(item: WatchLaterItem)

    suspend fun deleteItem(id: Long, type: String)

    suspend fun clearAll()
}

/**
 * 将信息流卡片解析为稍后再看条目；问题、专栏、收藏夹、广告等无法定位到回答/文章/想法的条目返回 null。
 */
fun FeedDisplayItem.toWatchLaterItemOrNull(): WatchLaterItem? {
    val (type, id) = when (val destination = navDestination) {
        is PostDestination -> destination.type.toWatchLaterType() to destination.id
        else -> when (val target = feed?.target) {
            is AnswerTargetDto -> "answer" to target.id
            is ArticleTargetDto -> "article" to target.id
            is PinTargetDto -> "pin" to target.id
            else -> return null
        }
    }
    return WatchLaterItem(
        id = id,
        type = type,
        title = title,
        excerpt = summary.orEmpty(),
        authorName = authorName,
        authorAvatar = avatarSrc,
        coverImage = images.firstOrNull(),
        addedAt = System.currentTimeMillis(),
    )
}

private fun PostType.toWatchLaterType(): String = when (this) {
    PostType.Answer -> "answer"
    PostType.Article -> "article"
    PostType.Pin -> "pin"
}

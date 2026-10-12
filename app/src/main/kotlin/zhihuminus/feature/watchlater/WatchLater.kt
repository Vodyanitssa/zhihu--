package com.zhihuminus.feature.watchlater

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

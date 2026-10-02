package com.zhihuminus.feature.collection

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.Person

data class Collection(
    val id: String,
    val isFavorited: Boolean = false,
    val type: String = "collection",
    val title: String = "",
    val isPublic: Boolean = false,
    val url: String = "",
    val description: String = "",
    val followerCount: Int = 0,
    val answerCount: Int = 0,
    val itemCount: Int = 0,
    val likeCount: Int = 0,
    val viewCount: Int = 0,
    val commentCount: Int = 0,
    val isFollowing: Boolean = false,
    val isLiking: Boolean = false,
    val createdTime: Long = 0L,
    val updatedTime: Long = 0L,
    val creator: Person? = null,
    val isDefault: Boolean = false,
)

data class CollectionsPageResult(
    val items: List<Collection>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

data class CollectionContentPageResult(
    val items: List<FeedDisplayItem>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

interface CollectionRepository {
    suspend fun getUserCollections(urlToken: String, nextUrl: String? = null): CollectionsPageResult

    suspend fun getCollection(collectionId: String): Collection

    suspend fun getCollectionItems(
        collectionId: String,
        offset: Int? = null,
        limit: Int = 20,
        nextUrl: String? = null,
    ): CollectionContentPageResult

    suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection

    suspend fun deleteCollection(collectionId: String): Boolean

    suspend fun getCollectionsForContent(type: String, id: Long): List<Collection>

    suspend fun addToCollection(type: String, id: Long, collectionId: String)

    suspend fun removeFromCollection(type: String, id: Long, collectionId: String)
}

val Collection.subtitleText: String
    get() = "$itemCount 内容·${if (isPublic) "公开" else "仅自己可见"}"

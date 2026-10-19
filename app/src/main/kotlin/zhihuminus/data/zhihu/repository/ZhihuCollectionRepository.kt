package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.zhihu.api.ZhihuCollectionApi
import com.zhihuminus.data.zhihu.mapper.toDomain
import com.zhihuminus.data.zhihu.mapper.toFeedDisplayItem
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.collection.CollectionContentPageResult
import com.zhihuminus.feature.collection.CollectionRepository
import com.zhihuminus.feature.collection.CollectionsPageResult

class ZhihuCollectionRepository(
    private val api: ZhihuCollectionApi,
) : CollectionRepository {
    override suspend fun getUserCollections(urlToken: String, nextUrl: String?): CollectionsPageResult {
        val response = api.getUserCollections(urlToken, nextUrl)
        return CollectionsPageResult(
            items = response.data.map { it.toDomain() },
            nextUrl = response.paging.nextUrl,
            isEnd = !response.paging.hasMore,
        )
    }

    override suspend fun getCollection(collectionId: String): Collection = api.getCollection(collectionId).toDomain()

    override suspend fun getCollectionItems(
        collectionId: String,
        offset: Int?,
        limit: Int,
        nextUrl: String?,
    ): CollectionContentPageResult {
        val response = api.getCollectionItems(collectionId, offset, limit, nextUrl)
        return CollectionContentPageResult(
            items = response.data.map { it.toFeedDisplayItem() },
            nextUrl = response.paging.nextUrl,
            isEnd = !response.paging.hasMore,
        )
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection = api.createCollection(title, description, isPublic).toDomain()

    override suspend fun deleteCollection(collectionId: String): Boolean = api.deleteCollection(collectionId)

    override suspend fun getCollectionsForContent(type: String, id: Long): List<Collection> = api.getCollections(type, id).data.map { it.toDomain() }

    override suspend fun addToCollection(type: String, id: Long, collectionId: String) {
        api.addToCollection(type, id, collectionId)
    }

    override suspend fun removeFromCollection(type: String, id: Long, collectionId: String) {
        api.removeFromCollection(type, id, collectionId)
    }
}

package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.collection.CollectionContentPageResult
import com.zhihuminus.feature.collection.CollectionRepository
import com.zhihuminus.feature.collection.CollectionsPageResult

class ZhihuCollectionRepository(
    private val api: ZhihuApi,
) : CollectionRepository {
    override suspend fun getUserCollections(urlToken: String, nextUrl: String?): CollectionsPageResult {
        val response = api.getUserCollections(urlToken, nextUrl)
        return CollectionsPageResult(
            items = response.data.map { it.toDomain() },
            nextUrl = response.paging.next.takeIf { !response.paging.isEnd && it.isNotBlank() },
            isEnd = response.paging.isEnd || response.paging.next.isBlank(),
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
            nextUrl = response.paging.next.takeIf { !response.paging.isEnd && it.isNotBlank() },
            isEnd = response.paging.isEnd || response.paging.next.isBlank(),
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

fun CollectionDto.toDomain(): Collection = Collection(
    id = id,
    isFavorited = isFavorited,
    type = type,
    title = title,
    isPublic = isPublic,
    url = url,
    description = description,
    followerCount = followerCount,
    answerCount = answerCount,
    itemCount = itemCount,
    likeCount = likeCount,
    viewCount = viewCount,
    commentCount = commentCount,
    isFollowing = isFollowing,
    isLiking = isLiking,
    createdTime = createdTime,
    updatedTime = updatedTime,
    creator = creator,
    isDefault = isDefault,
)

fun CollectionItemDto.toFeedDisplayItem(): FeedDisplayItem = FeedDisplayItem(
    title = content.title,
    summary = content.excerpt,
    details = content.detailsText,
    navDestinationJson = content.navDestination?.toFeedDisplayItemNavDestinationJson(),
    feed = null,
    authorName = content.author?.name,
    avatarSrc = when (content) {
        is Feed.AnswerTarget -> content.author?.avatarUrl
        is Feed.ArticleTarget -> content.author.avatarUrl
        is Feed.QuestionTarget -> content.author?.avatarUrl
        else -> null
    },
    contentTypeLabel = content.description(),
    publishTimeSeconds = content.createdTime.takeIf { it > 0 },
)

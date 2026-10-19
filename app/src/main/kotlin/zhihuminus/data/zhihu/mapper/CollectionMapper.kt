package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.collection.CollectionCreator

/**
 * 知乎收藏夹与收藏夹内容条目映射器。
 */
object CollectionMapper {
    fun toCollection(dto: CollectionDto): Collection = Collection(
        id = dto.id,
        isFavorited = dto.isFavorited,
        type = dto.type,
        title = dto.title,
        isPublic = dto.isPublic,
        url = dto.url,
        description = dto.description,
        followerCount = dto.followerCount,
        answerCount = dto.answerCount,
        itemCount = dto.itemCount,
        likeCount = dto.likeCount,
        viewCount = dto.viewCount,
        commentCount = dto.commentCount,
        isFollowing = dto.isFollowing,
        isLiking = dto.isLiking,
        createdTime = dto.createdTime,
        updatedTime = dto.updatedTime,
        creator = dto.creator?.let {
            CollectionCreator(
                id = it.id,
                name = it.name,
                avatarUrl = it.avatarUrl,
                headline = it.headline,
            )
        },
        isDefault = dto.isDefault,
    )

    fun toFeedDisplayItem(dto: CollectionItemDto): FeedDisplayItem {
        val (images, layout) = dto.content.resolveFeedImages()
        return FeedDisplayItem(
            title = dto.content.title,
            summary = dto.content.excerpt,
            details = dto.content.detailsText,
            navDestinationJson = dto.content.navDestination?.toFeedDisplayItemNavDestinationJson(),
            feed = null,
            authorName = dto.content.author?.name,
            avatarSrc = dto.content.author?.avatarUrl,
            contentTypeLabel = dto.content.description(),
            publishTimeSeconds = dto.content.createdTime.takeIf { it > 0 },
            images = images,
            imageLayout = layout,
        )
    }
}

fun CollectionDto.toDomain(): Collection = CollectionMapper.toCollection(this)

fun CollectionDto.toCollection(): Collection = CollectionMapper.toCollection(this)

fun CollectionItemDto.toFeedDisplayItem(): FeedDisplayItem = CollectionMapper.toFeedDisplayItem(this)

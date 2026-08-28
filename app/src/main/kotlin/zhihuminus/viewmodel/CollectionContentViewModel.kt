/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.Collection
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.navDestination
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlin.random.Random
import kotlin.reflect.typeOf

suspend fun ZhihuApiEnvironment.fetchCollection(collectionId: String): Collection {
    val json = fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "") ?: error("收藏夹信息为空")
    return ZhihuJson.decodeJson<Collection>(json["collection"] ?: throw IllegalStateException("收藏夹信息为空"))
}

class CollectionContentViewModel(
    val collectionId: String,
) : PaginationViewModel<CollectionItem>(typeOf<CollectionItem>()) {
    private var randomPageOffsets: List<Int>? = null
    private var randomPageCursor = 0
    private var lastRandomFirstOffset: Int? = null
    private var activeRandomSeed: Int? = null
    private var activeRandomItemCount: Int? = null
    private val randomDisplayOrderKeys = mutableListOf<String>()
    internal val retainedRandomOrderKeys: List<String>
        get() = randomDisplayOrderKeys
    val displayItems = mutableStateListOf<FeedDisplayItem>()
    var collection by mutableStateOf<Collection?>(null)
    val title by derivedStateOf {
        collection?.title ?: "收藏夹"
    }
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/collections/$collectionId/items"

    override val isEnd: Boolean
        get() = randomPageOffsets?.let { randomPageCursor >= it.size } ?: super.isEnd

    val nextPageUrl: String
        get() = lastPaging?.next.orEmpty()

    override fun processResponse(environment: PaginationEnvironment, data: List<CollectionItem>, rawData: JsonArray) {
        super.processResponse(environment, data, rawData)
        displayItems.addAll(data.map { createDisplayItem(it) }) // 展示用的已flatten数据
        if (randomPageOffsets != null) {
            randomPageCursor++
        }
    }

    override fun resolvePageUrl(): String {
        val offset = randomPageOffsets?.getOrNull(randomPageCursor) ?: return super.resolvePageUrl()
        return "https://www.zhihu.com/api/v4/collections/$collectionId/items?offset=$offset&limit=$COLLECTION_PAGE_SIZE"
    }

    private fun createDisplayItem(item: CollectionItem): FeedDisplayItem = FeedDisplayItem(
        title = item.content.title,
        summary = item.content.excerpt,
        details = item.content.detailsText,
        navDestinationJson = item.content.navDestination?.toFeedDisplayItemNavDestinationJson(),
        feed = null,
        authorName = item.content.author?.name,
        avatarSrc = when (item.content) {
            is Feed.AnswerTarget -> item.content.author?.avatarUrl
            is Feed.ArticleTarget -> item.content.author.avatarUrl
            is Feed.QuestionTarget -> item.content.author?.avatarUrl
            else -> null
        },
        contentTypeLabel = item.content.description(),
        publishTimeSeconds = item.content.createdTime.takeIf { it > 0 },
    )

    override fun refresh(environment: PaginationEnvironment) {
        activeRandomSeed = null
        activeRandomItemCount = null
        randomDisplayOrderKeys.clear()
        randomPageOffsets = null
        randomPageCursor = 0
        refreshCurrentPagingMode(environment)
    }

    fun refreshRandom(
        environment: PaginationEnvironment,
        itemCount: Int,
        randomSeed: Int,
    ) {
        if (
            shouldReuseCollectionRandomSession(
                activeRandomSeed = activeRandomSeed,
                activeRandomItemCount = activeRandomItemCount,
                requestedRandomSeed = randomSeed,
                requestedItemCount = itemCount,
                hasLoadedItems = displayItems.isNotEmpty(),
                isLoading = isLoading,
                isEnd = isEnd,
            )
        ) {
            return
        }

        activeRandomSeed = randomSeed
        activeRandomItemCount = itemCount
        randomDisplayOrderKeys.clear()
        val offsets = collectionRandomPageOffsets(
            itemCount = itemCount,
            randomSeed = randomSeed,
            previousFirstOffset = lastRandomFirstOffset ?: 0,
        )
        randomPageOffsets = offsets
        randomPageCursor = 0
        lastRandomFirstOffset = offsets.firstOrNull()
        refreshCurrentPagingMode(environment)
    }

    internal fun retainRandomDisplayOrder(keys: List<String>) {
        randomDisplayOrderKeys.clear()
        randomDisplayOrderKeys.addAll(keys)
    }

    private fun refreshCurrentPagingMode(environment: PaginationEnvironment) {
        displayItems.clear()
        viewModelScope.launch {
            collection = environment.fetchCollection(collectionId)
        }
        super.refresh(environment)
    }
}

internal fun collectionRandomPageOffsets(
    itemCount: Int,
    randomSeed: Int,
    previousFirstOffset: Int? = null,
    pageSize: Int = COLLECTION_PAGE_SIZE,
): List<Int> {
    require(pageSize > 0)
    val pageCount = ((itemCount.coerceAtLeast(1) + pageSize - 1) / pageSize)
    val offsets = (0 until pageCount)
        .map { page -> page * pageSize }
        .shuffled(Random(randomSeed))
        .toMutableList()
    if (offsets.size > 1 && offsets.first() == previousFirstOffset) {
        val replacementIndex = offsets.indexOfFirst { it != previousFirstOffset }
        val first = offsets.first()
        offsets[0] = offsets[replacementIndex]
        offsets[replacementIndex] = first
    }
    return offsets
}

internal fun shouldReuseCollectionRandomSession(
    activeRandomSeed: Int?,
    activeRandomItemCount: Int?,
    requestedRandomSeed: Int,
    requestedItemCount: Int,
    hasLoadedItems: Boolean,
    isLoading: Boolean,
    isEnd: Boolean,
): Boolean =
    activeRandomSeed == requestedRandomSeed &&
        activeRandomItemCount == requestedItemCount &&
        (hasLoadedItems || isLoading || isEnd)

private const val COLLECTION_PAGE_SIZE = 20

@Serializable
class CollectionItem(
    val created: String,
    val content: Feed.Target,
)

// Re-export from ui package for backward compatibility with tests

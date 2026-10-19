package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.core.telemetry.ZaFeedExposureItem
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.api.RECOMMEND_FEED_URL
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.api.ZhihuNotificationApi
import com.zhihuminus.data.zhihu.mapper.flattenFeeds
import com.zhihuminus.data.zhihu.mapper.toDisplayItem
import com.zhihuminus.feature.home.FeedExposureRecord
import com.zhihuminus.feature.home.HomeFeedPage
import com.zhihuminus.feature.home.HomeRepository
import com.zhihuminus.feature.home.decodeHomeFeedStartupSnapshot
import com.zhihuminus.feature.home.encodeHomeFeedStartupSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class ZhihuHomeRepository(
    private val feedApi: ZhihuFeedApi,
    private val notificationApi: ZhihuNotificationApi,
    private val startupCacheFile: Path? = null,
    private val postCache: PostContentCache = PostContentCache,
) : HomeRepository {
    override suspend fun fetchRecommendFeed(nextUrl: String?): HomeFeedPage {
        val feedPage = feedApi.fetchFeedPage(nextUrl ?: RECOMMEND_FEED_URL, include = "")
        val flattenedItems = feedPage.items.flattenFeeds()
        flattenedItems.forEach { item ->
            item.target?.let { postCache.putFromFeed(it) }
        }
        val displayItems = flattenedItems.map { it.toDisplayItem() }
        return HomeFeedPage(
            items = displayItems,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd || feedPage.nextUrl == null,
        )
    }

    override suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
        feedApi.reportContentTouch(untouchedItems)
    }

    override suspend fun reportExposures(records: List<FeedExposureRecord>) {
        if (records.isEmpty()) return
        val touches = records.map { it.type to it.id }
        feedApi.reportContentTouch(touches)

        val zaItems = records.mapNotNull { rec ->
            rec.attachedInfo?.takeIf { it.isNotBlank() }?.let { att ->
                ZaFeedExposureItem(attachedInfo = att, cardIndex = rec.index)
            }
        }
        if (zaItems.isNotEmpty()) {
            feedApi.reportZaExposures(zaItems)
        }
    }

    override suspend fun reportContentRead(type: String, id: String) {
        feedApi.reportContentRead(type, id)
    }

    override suspend fun fetchUnreadNotificationCount(): Int = try {
        notificationApi.getMeNotifications().totalCount
    } catch (_: Exception) {
        0
    }

    override suspend fun loadStartupSnapshot(): List<FeedDisplayItem> = withContext(Dispatchers.Default) {
        val file = startupCacheFile ?: return@withContext emptyList()
        val items = runCatching {
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.source(file).buffered().use { source ->
                    decodeHomeFeedStartupSnapshot(source.readString())
                }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
        items.forEach { displayItem ->
            displayItem.feed?.target?.let { target -> postCache.putFromFeed(target) }
        }
        items
    }

    override suspend fun saveStartupSnapshot(items: List<FeedDisplayItem>) {
        val file = startupCacheFile ?: return
        val serialized = encodeHomeFeedStartupSnapshot(items) ?: return
        withContext(Dispatchers.Default) {
            runCatching {
                SystemFileSystem.sink(file).buffered().use { it.writeString(serialized) }
            }
        }
    }

    override suspend fun markUninterested(contentToken: String, contentType: String): Boolean =
        feedApi.markUninterested(contentToken, contentType)
}

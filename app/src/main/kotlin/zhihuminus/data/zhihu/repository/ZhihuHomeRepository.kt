package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.data.zhihu.api.RECOMMEND_FEED_URL
import com.zhihuminus.data.zhihu.api.ZhihuFeedApi
import com.zhihuminus.data.zhihu.api.ZhihuNotificationApi
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
        feedPage.items.forEach { item ->
            item.target?.let { postCache.putFromFeed(it) }
        }
        val displayItems = feedPage.items.flattenFeeds().map { it.toDisplayItem() }
        return HomeFeedPage(
            items = displayItems,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd || feedPage.nextUrl == null,
        )
    }

    override suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
        feedApi.reportContentTouch(untouchedItems)
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
        runCatching {
            if (SystemFileSystem.exists(file)) {
                SystemFileSystem.source(file).buffered().use { source ->
                    decodeHomeFeedStartupSnapshot(source.readString())
                }
            } else {
                emptyList()
            }
        }.getOrDefault(emptyList())
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
}

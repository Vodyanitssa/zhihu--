package com.zhihuminus.feature.home

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.common.ZhihuJson

const val HOME_FEED_STARTUP_CACHE_FILE_NAME = "home_feed_startup_cache.json"

private const val HOME_FEED_STARTUP_SNAPSHOT_MAX_ITEMS = 10

fun homeFeedStartupCacheFileName(): String =
    HOME_FEED_STARTUP_CACHE_FILE_NAME

fun homeFeedStartupCacheFileNames(): List<String> =
    listOf(HOME_FEED_STARTUP_CACHE_FILE_NAME, "WEB")

fun encodeHomeFeedStartupSnapshot(items: List<FeedDisplayItem>): String? {
    val snapshotItems = items.take(HOME_FEED_STARTUP_SNAPSHOT_MAX_ITEMS)
    if (snapshotItems.isEmpty()) return null

    return try {
        ZhihuJson.json.encodeToString(snapshotItems)
    } catch (error: Exception) {
        Log.e("HomeFeedStartupSnapshot", "Failed to encode home feed startup snapshot", error)
        null
    }
}

fun decodeHomeFeedStartupSnapshot(serialized: String?): List<FeedDisplayItem> {
    if (serialized.isNullOrBlank()) return emptyList()

    return runCatching {
        ZhihuJson
            .json
            .decodeFromString<List<FeedDisplayItem>>(serialized)
    }.getOrDefault(emptyList())
}

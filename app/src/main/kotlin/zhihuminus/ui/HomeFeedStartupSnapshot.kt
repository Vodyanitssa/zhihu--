/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.zhihuminus.ui

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson

const val AUTO_REFRESH_HOME_ON_STARTUP_PREFERENCE_KEY = "autoRefreshHomeOnStartup"
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

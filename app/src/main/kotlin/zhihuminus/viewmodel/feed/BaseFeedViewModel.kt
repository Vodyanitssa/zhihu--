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

package com.zhihuminus.viewmodel.feed

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.viewmodel.FeedDisplayEnvironment
import com.zhihuminus.viewmodel.PaginationEnvironment
import com.zhihuminus.viewmodel.PaginationViewModel
import kotlinx.serialization.json.JsonArray
import kotlin.reflect.typeOf

abstract class BaseFeedViewModel : PaginationViewModel<Feed>(typeOf<Feed>()) {
    var displayItems = mutableStateListOf<FeedDisplayItem>()
    internal var latestLoadedDisplayItems = mutableStateOf<List<FeedDisplayItem>>(emptyList())
    var isPullToRefresh by mutableStateOf(false)
        protected set

    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
        super.processResponse(environment, data, rawData)
        val loadedItems = data.flattenFeeds().map { createDisplayItem(environment, it) }
        addDisplayItems(loadedItems)
        latestLoadedDisplayItems.value = loadedItems
    }

    override fun refresh(environment: PaginationEnvironment) {
        displayItems.clear()
        super.refresh(environment)
    }

    suspend fun pullToRefresh(environment: PaginationEnvironment) {
        isPullToRefresh = true
        displayItems.clear()
        if (isLoading) return
        errorMessage = null
        debugData.clear()
        allData.clear()
        lastPaging = null // 重置 lastPaging
        isLoading = true
        try {
            fetchFeeds(environment)
        } catch (e: Exception) {
            errorHandle(e)
        }
        isLoading = false
        isPullToRefresh = false
    }

    open fun createDisplayItem(environment: FeedDisplayEnvironment, feed: Feed): FeedDisplayItem = feed.toDisplayItem()

    fun addDisplayItems(newItems: List<FeedDisplayItem>) {
        newItems.forEach {
            if (displayItems.none { existing -> existing.stableKey == it.stableKey }) {
                displayItems.add(it)
            }
        }
    }
}

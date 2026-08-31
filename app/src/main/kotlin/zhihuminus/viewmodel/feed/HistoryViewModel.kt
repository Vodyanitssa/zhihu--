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

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.HistoryDeletePair
import com.zhihuminus.data.HistoryItem
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuHistoryRepository
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.viewmodel.PaginationEnvironment

class HistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"
    override val shouldLogDecodeFailures: Boolean = false
    private val deletionPairs = mutableMapOf<FeedDisplayItem, HistoryDeletePair>()

    private fun repository(environment: PaginationEnvironment) =
        ZhihuHistoryRepository(ZhihuApiImpl(environment))

    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = resolvePageUrl()
            val result = repository(environment).fetchPage(url)

            addHistoryItems(result.items)
            lastPaging = ZhihuPaging(
                isEnd = result.isEnd || result.nextUrl == null,
                next = result.nextUrl.orEmpty(),
            )
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    private fun addHistoryItems(items: List<HistoryItem>) {
        items.forEach { item ->
            val navDest = try {
                resolveContent(item.actionUrl)
            } catch (e: Exception) {
                null
            }

            val displayItem = FeedDisplayItem(
                title = item.title,
                summary = item.summary,
                details = item.details,
                feed = null,
                navDestinationJson = navDest?.toFeedDisplayItemNavDestinationJson(),
                avatarSrc = null,
                authorName = item.authorName,
                contentTypeLabel = item.contentTypeLabel,
            )
            deletionPairs[displayItem] = HistoryDeletePair(
                contentToken = item.contentToken,
                contentType = item.contentType,
            )
            if (displayItems.none { it.stableKey == displayItem.stableKey }) {
                displayItems.add(displayItem)
            }
        }
    }

    suspend fun deleteItem(environment: PaginationEnvironment, item: FeedDisplayItem) {
        val pair = checkNotNull(deletionPairs[item]) { "在线历史记录缺少删除标识" }
        repository(environment).deleteItem(pair)
        displayItems.remove(item)
        deletionPairs.remove(item)
    }
}

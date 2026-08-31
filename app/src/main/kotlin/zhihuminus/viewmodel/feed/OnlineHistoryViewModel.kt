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
import com.zhihuminus.data.HistoryItem
import com.zhihuminus.data.OnlineHistoryDeletePair
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.data.zhihu.ZhihuHistoryRepository
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.viewmodel.PaginationEnvironment

class OnlineHistoryViewModel : BaseFeedViewModel() {
    override val initialUrl: String = "https://api.zhihu.com/unify-consumption/read_history?offset=0&limit=10"
    override val shouldLogDecodeFailures: Boolean = false
    private val deletionPairs = mutableMapOf<FeedDisplayItem, OnlineHistoryDeletePair>()

    /**
     * 通过 [ZhihuHistoryRepository.fetchPage] 获取历史记录。
     * Repository 返回解析好的 [HistoryItem] 业务对象，ViewModel 不感知 API 细节。
     */
    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val url = resolvePageUrl()
            val result = ZhihuHistoryRepository(environment).fetchPage(url)

            addHistoryItems(environment, result.items)
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

    /**
     * 将 [HistoryItem] 业务对象映射为 UI 层 [FeedDisplayItem] 并填充 [deletionPairs]。
     */
    private fun addHistoryItems(environment: PaginationEnvironment, items: List<HistoryItem>) {
        val localHistory = environment.localHistory()

        items.forEach { item ->
            val navDest = try {
                resolveContent(item.actionUrl)
            } catch (e: Exception) {
                null
            }

            val matchedItem = localHistory.firstOrNull { it == navDest }
            val displayItem = FeedDisplayItem(
                title = item.title,
                summary = item.summary,
                details = item.details,
                feed = null,
                navDestinationJson = navDest?.toFeedDisplayItemNavDestinationJson(),
                avatarSrc = when (matchedItem) {
                    is PostDestination -> matchedItem.avatarSrc
                    else -> null
                },
                authorName = item.authorName,
                contentTypeLabel = item.contentTypeLabel,
            )
            deletionPairs[displayItem] = OnlineHistoryDeletePair(
                contentToken = item.contentToken,
                contentType = item.contentType,
            )
            // 去重
            if (displayItems.none { it.stableKey == displayItem.stableKey }) {
                displayItems.add(displayItem)
            }
        }
    }

    suspend fun deleteItem(environment: PaginationEnvironment, item: FeedDisplayItem) {
        val pair = checkNotNull(deletionPairs[item]) { "在线历史记录缺少删除标识" }
        ZhihuHistoryRepository(environment).deleteHistoryItem(pair)
        displayItems.remove(item)
        deletionPairs.remove(item)
    }
}

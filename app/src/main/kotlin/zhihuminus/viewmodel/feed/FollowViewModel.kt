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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.sourceLabel
import com.zhihuminus.data.target
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuFeedRepository
import com.zhihuminus.viewmodel.FeedDisplayEnvironment
import com.zhihuminus.viewmodel.PaginationEnvironment
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray

class FollowViewModel : BaseFeedViewModel() {
    override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        try {
            val repository = ZhihuFeedRepository(ZhihuApiImpl(environment))
            val page = repository.fetchMomentsFeedPage(url = lastPaging?.next)
            processResponse(environment, page.items, JsonArray(emptyList()))
            lastPaging = ZhihuPaging(
                isEnd = page.isEnd || page.nextUrl == null,
                next = page.nextUrl.orEmpty(),
            )
        } catch (e: Exception) {
            if (e is kotlin.coroutines.cancellation.CancellationException) throw e
            environment.handleFetchFailure(this::class.simpleName, e)
        } finally {
            isLoading = false
        }
    }

    override fun createDisplayItem(environment: FeedDisplayEnvironment, feed: Feed): FeedDisplayItem {
        val item = super.createDisplayItem(environment, feed)
        return if (item.isFiltered || feed.sourceLabel == null) {
            item
        } else {
            item.copy(details = feed.target?.detailsText ?: item.details)
        }
    }
}

class FollowRecommendViewModel : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://api.zhihu.com/moments_v3?feed_type=recommend"
}

class RecentMomentsViewModel : ViewModel() {
    @Serializable
    data class Actor(
        val id: String,
        val urlToken: String,
        val name: String,
        val avatarUrl: String,
    )

    @Serializable
    data class FollowingUserItem(
        val actor: Actor,
        val unreadCount: Int,
    )

    var users = mutableStateListOf<FollowingUserItem>()
    var isLoading by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)

    fun load(environment: ZhihuApiEnvironment) {
        if (isLoading || users.isNotEmpty()) return
        isLoading = true
        viewModelScope.launch {
            try {
                val json = environment.fetchJson("https://api.zhihu.com/moments/recent?type=raw", "") ?: return@launch
                val dataArray = json["data"]?.jsonArray ?: return@launch
                users.addAll(
                    dataArray.mapNotNull { item ->
                        try {
                            ZhihuJson.decodeJson<FollowingUserItem>(item)
                        } catch (e: Exception) {
                            environment.logDecodeFailure("RecentMomentsVM", item, e)
                            null
                        }
                    },
                )
            } catch (e: Exception) {
                environment.handleFetchFailure("RecentMomentsVM", e)
                errorMessage = "加载关注动态失败"
            } finally {
                isLoading = false
            }
        }
    }
}

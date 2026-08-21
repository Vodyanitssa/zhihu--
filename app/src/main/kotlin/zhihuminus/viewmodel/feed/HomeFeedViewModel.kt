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

import androidx.lifecycle.viewModelScope
import com.zhihuminus.data.Feed
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.target
import com.zhihuminus.util.Log
import com.zhihuminus.viewmodel.ContentInteractionEnvironment
import com.zhihuminus.viewmodel.PaginationEnvironment
import com.zhihuminus.viewmodel.postSigned
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray

class HomeFeedViewModel : BaseFeedViewModel() {
    private val reportedTouchedItems = hashSetOf<Pair<String, String>>()

    override val initialUrl: String
        //        get() = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        allowGuestAccess = true
    }

    public override suspend fun fetchFeeds(environment: PaginationEnvironment) {
        markItemsAsTouched(environment)
        super.fetchFeeds(environment)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun processResponse(environment: PaginationEnvironment, data: List<Feed>, rawData: JsonArray) {
        allData.addAll(data)
        debugData.addAll(rawData)

        viewModelScope.launch {
            val newItems = data
                .flattenFeeds()
                .map { feed -> createDisplayItem(environment, feed) }
            withContext(Dispatchers.Main) {
                addDisplayItems(newItems)
                latestLoadedDisplayItems.value = newItems
            }
        }
    }

    /**
     * 记录用户与内容的交互行为
     * 应该在用户点击、点赞等操作时调用
     */
    suspend fun recordContentInteraction(environment: ContentInteractionEnvironment, feed: Feed) {
        try {
            environment.recordContentInteraction(feed)
        } catch (e: Exception) {
            environment.handleFetchFailure("HomeFeedViewModel", e)
        }
    }

    /**
     * 记录用户点击内容
     * 在viewModelScope中运行，使用viewModelScope代替GlobalScope
     */
    fun onUiContentClick(environment: ContentInteractionEnvironment, feed: Feed, item: FeedDisplayItem) {
        viewModelScope.launch(Dispatchers.Default) {
            if (environment.authenticatedCookies()["d_c0"] != null) {
                val payloadItem = when (val target = feed.target) {
                    is Feed.AnswerTarget -> listOf("answer", target.id.toString(), "read")
                    is Feed.ArticleTarget -> listOf("article", target.id.toString(), "read")
                    is Feed.PinTarget -> listOf("pin", target.id.toString(), "read")
                    else -> null
                }
                if (payloadItem != null) {
                    environment.postSigned("https://www.zhihu.com/lastread/touch") {
                        header("x-requested-with", "fetch")
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append("items", ZhihuJson.json.encodeToString(listOf(payloadItem)))
                                },
                            ),
                        )
                    }
                }
            }
            recordContentInteraction(environment, feed)
        }
    }

    private suspend fun markItemsAsTouched(
        environment: ContentInteractionEnvironment,
    ) {
        try {
            if (environment.authenticatedCookies()["d_c0"] == null) return
            val currentTouchItems = displayItems
                .asSequence()
                .filterNot { it.isFiltered }
                .mapNotNull { it.feed?.target }
                .mapNotNull { target ->
                    when (target) {
                        is Feed.AnswerTarget -> "answer" to target.id.toString()
                        is Feed.ArticleTarget -> "article" to target.id.toString()
                        is Feed.PinTarget -> "pin" to target.id.toString()
                        else -> null
                    }
                }.toList()
            val untouchedItemSet = currentTouchItems - reportedTouchedItems

            if (untouchedItemSet.isNotEmpty()) {
                val payload = untouchedItemSet.map { (type, id) -> listOf(type, id, "touch") }
                val response = environment.postSigned("https://www.zhihu.com/lastread/touch") {
                    header("x-requested-with", "fetch")
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("items", ZhihuJson.json.encodeToString(payload))
                            },
                        ),
                    )
                }
                if (response.status.isSuccess()) {
                    reportedTouchedItems.addAll(untouchedItemSet)
                } else {
                    Log.e("Browse-Touch", response.bodyAsText())
                }
            }
        } catch (e: Exception) {
            environment.handleFetchFailure("FeedViewModel", e)
        }
    }

    override fun refresh(environment: PaginationEnvironment) {
        super.refresh(environment)
        reportedTouchedItems.clear()
    }
}

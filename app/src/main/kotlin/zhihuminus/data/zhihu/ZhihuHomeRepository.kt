package com.zhihuminus.data.zhihu

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.feature.home.HomeFeedPage
import com.zhihuminus.feature.home.HomeRepository
import com.zhihuminus.ui.decodeHomeFeedStartupSnapshot
import com.zhihuminus.ui.encodeHomeFeedStartupSnapshot
import com.zhihuminus.util.Log
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

class ZhihuHomeRepository(
    private val api: ZhihuApi,
    private val environment: ZhihuApiEnvironment,
    private val startupCacheFile: Path? = null,
) : HomeRepository {
    override suspend fun fetchRecommendFeed(nextUrl: String?): HomeFeedPage {
        val feedPage = api.fetchFeedPage(nextUrl ?: RECOMMEND_FEED_URL, include = "")
        val displayItems = feedPage.items.flattenFeeds().map { it.toDisplayItem() }
        return HomeFeedPage(
            items = displayItems,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd || feedPage.nextUrl == null,
        )
    }

    override suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
        if (untouchedItems.isEmpty()) return
        if (environment.authenticatedCookies()["d_c0"] == null) return

        try {
            val payload = untouchedItems.map { (type, id) -> listOf(type, id, "touch") }
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
            if (!response.status.isSuccess()) {
                Log.e("ZhihuHomeRepository", "Touch report failed: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            Log.w("ZhihuHomeRepository", "Failed to report content touch", e)
        }
    }

    override suspend fun reportContentRead(type: String, id: String) {
        if (environment.authenticatedCookies()["d_c0"] == null) return

        try {
            val payload = listOf(listOf(type, id, "read"))
            environment.postSigned("https://www.zhihu.com/lastread/touch") {
                header("x-requested-with", "fetch")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("items", ZhihuJson.json.encodeToString(payload))
                        },
                    ),
                )
            }
        } catch (e: Exception) {
            Log.w("ZhihuHomeRepository", "Failed to report content read", e)
        }
    }

    override suspend fun fetchUnreadNotificationCount(): Int = try {
        api.getMeNotifications().totalCount
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

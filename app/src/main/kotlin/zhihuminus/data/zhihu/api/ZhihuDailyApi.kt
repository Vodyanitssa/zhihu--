package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException

private const val DAILY_PRIMARY_API_BASE = "https://news-at.zhihu.com/api/4/stories"

// Zhihu Daily's documented Android API host can fail DNS resolution in some
// overseas networks because of Zhihu-side DNS/server configuration. Keep this
// fallback host as a narrow workaround for host-resolution failures only.
// See https://github.com/zly2006/zhihu-plus-plus/issues/417.
private const val DAILY_FALLBACK_API_BASE = "https://daily.zhihu.com/api/4/stories"

open class ZhihuDailyApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 获取知乎日报最新故事列表
     */
    open suspend fun getDailyLatest(): DailyStoriesResponse =
        fetchDailyStories("/latest")

    /**
     * 获取指定日期之前的知乎日报故事列表
     * @param date 8 位日期字符串，如 "20240401"
     */
    open suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse =
        fetchDailyStories("/before/$date")

    private suspend fun fetchDailyStories(path: String): DailyStoriesResponse {
        val client = environment.httpClient()
        return try {
            client.get("$DAILY_PRIMARY_API_BASE$path").body()
        } catch (e: Exception) {
            if (e is CancellationException || !e.isHostResolutionFailure()) {
                throw e
            }
            client.get("$DAILY_FALLBACK_API_BASE$path").body()
        }
    }
}

private fun Throwable.isHostResolutionFailure(): Boolean =
    this is UnresolvedAddressException ||
        this::class.simpleName == "UnknownHostException" ||
        message?.contains("Unable to resolve host", ignoreCase = true) == true ||
        message?.contains("No address associated with hostname", ignoreCase = true) == true ||
        message?.contains("Name or service not known", ignoreCase = true) == true ||
        message?.contains("nodename nor servname provided", ignoreCase = true) == true ||
        generateSequence(cause) { it.cause }.any {
            it is UnresolvedAddressException ||
                it::class.simpleName == "UnknownHostException"
        }

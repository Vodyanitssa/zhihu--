package com.zhihuminus.data.zhihu

import com.zhihuminus.data.HistoryItem
import com.zhihuminus.data.OnlineHistoryDeletePair
import com.zhihuminus.data.OnlineHistoryItem
import com.zhihuminus.data.ZhihuJson.decodeJson
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import com.zhihuminus.viewmodel.postSigned
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * 在线浏览历史记录的 API 操作封装。
 *
 * 负责与知乎 `read_history` 系列接口交互，包括获取列表、删除单条和清空全部。
 * 返回业务层 [HistoryItem] 对象，ViewModel 不感知 API 细节。
 */
class ZhihuHistoryRepository(
    private val environment: ZhihuApiEnvironment,
) {
    /**
     * 获取一页在线浏览历史记录，返回解析后的业务对象。
     *
     * @param url 分页 URL（首页或续页）
     * @return [HistoryPageResult] 包含业务对象列表、续页 URL 和是否到底
     */
    suspend fun fetchPage(url: String): HistoryPageResult {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://://", "https://"), "")
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))

        val rawData = json["data"] as? JsonArray
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no $.data"))

        val paging = json["paging"]?.jsonObject
        val nextUrl = paging?.get("next")?.jsonPrimitive?.content
        val isEnd = paging
            ?.get("is_end")
            ?.jsonPrimitive
            ?.content
            ?.toBooleanStrictOrNull() ?: true

        val items = rawData.mapNotNull { element ->
            runCatching { decodeJson<OnlineHistoryItem>(element) }.getOrNull()?.toHistoryItem()
        }

        return HistoryPageResult(items, nextUrl, isEnd)
    }

    /**
     * 删除单条在线浏览历史记录。
     * 对应 POST `https://api.zhihu.com/read_history/batch_del`
     */
    internal suspend fun deleteHistoryItem(pair: OnlineHistoryDeletePair) {
        val response = environment.postSigned("https://api.zhihu.com/read_history/batch_del") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put(
                        "pairs",
                        JsonArray(
                            listOf(
                                buildJsonObject {
                                    put("content_token", pair.contentToken)
                                    put("content_type", pair.contentType)
                                },
                            ),
                        ),
                    )
                    put("clear", false)
                }.toString(),
            )
        }
        check(response.status.isSuccess()) { "删除在线历史记录失败: ${response.status}" }
    }

    /**
     * 清空全部在线浏览历史记录。
     * 对应 POST `https://api.zhihu.com/read_history/batch_del` with `clear: true`
     */
    suspend fun clearAllHistory() {
        environment.postSigned("https://api.zhihu.com/read_history/batch_del") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("pairs", JsonArray(emptyList()))
                    put("clear", true)
                }.toString(),
            )
        }
    }

    data class HistoryPageResult(
        val items: List<HistoryItem>,
        val nextUrl: String?,
        val isEnd: Boolean,
    )
}

private fun OnlineHistoryItem.toHistoryItem() = HistoryItem(
    title = data.header.title,
    summary = data.content?.summary ?: "",
    details = data.matrix
        ?.firstOrNull()
        ?.data
        ?.text ?: data.extra.contentType,
    authorName = data.content?.authorName,
    contentTypeLabel = when (data.extra.contentType) {
        "answer" -> "回答"
        "article" -> "文章"
        "pin" -> "想法"
        else -> data.extra.contentType
    },
    actionUrl = data.action.url,
    contentToken = data.extra.contentToken,
    contentType = data.extra.contentType,
)

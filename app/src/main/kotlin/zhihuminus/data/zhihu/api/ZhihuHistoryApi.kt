package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryItemDto
import com.zhihuminus.data.zhihu.dto.HistoryPage
import com.zhihuminus.data.zhihu.dto.PagingDto
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

open class ZhihuHistoryApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 记录阅读历史（read_history/add）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin", "profile", "question"
     */
    open suspend fun addHistory(contentToken: String, contentType: String) {
        val url = "https://www.zhihu.com/api/v4/read_history/add"
        environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("content_token", contentToken)
                    put("content_type", contentType)
                },
            )
        }
    }

    /**
     * 标记已读（lastread/touch）
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容类型: "answer", "article", "pin"
     */
    open suspend fun markAsRead(contentToken: String, contentType: String) {
        val url = "https://www.zhihu.com/lastread/touch"
        val items = listOf(
            listOf(contentType, contentToken, "touch"),
            listOf(contentType, contentToken, "read"),
        )
        environment.postSigned(url) {
            header("x-requested-with", "fetch")
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("items", ZhihuJson.json.encodeToString(items))
                    },
                ),
            )
        }
    }

    /**
     * 获取一页在线浏览历史记录。
     * @param url 分页 URL（首页或续页）
     */
    open suspend fun fetchHistoryPage(url: String): HistoryPage {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://://", "https://"), "")
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))

        val rawData = json["data"] as? JsonArray
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no $.data"))

        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }

        val items = rawData.mapNotNull { element ->
            runCatching { ZhihuJson.decodeJson<HistoryItemDto>(element) }.getOrNull()
        }

        return HistoryPage(items, paging?.nextUrl, paging?.hasMore != true)
    }

    /**
     * 批量删除在线浏览历史记录。
     * @param pairs 要删除的记录标识列表
     */
    open suspend fun deleteHistoryItems(pairs: List<HistoryDeletePairDto>) {
        val response = environment.postSigned("https://api.zhihu.com/read_history/batch_del") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put(
                        "pairs",
                        JsonArray(
                            pairs.map { pair ->
                                buildJsonObject {
                                    put("content_token", pair.contentToken)
                                    put("content_type", pair.contentType)
                                }
                            },
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
     */
    open suspend fun clearHistory() {
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
}

package com.zhihuminus.data.zhihu.api

import com.zhihuminus.account.DEFAULT_ZHIHU_USER_AGENT
import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.core.telemetry.ZaFeedExposureItem
import com.zhihuminus.core.telemetry.ZaProtobufBuilder
import com.zhihuminus.core.telemetry.gzipCompress
import com.zhihuminus.core.util.Log
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.FeedDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowingUserItemDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

const val FEED_INCLUDE =
    "data[*].content,excerpt,headline,target.author.badge_v2,target.author.is_following,target.author.is_followed"

/** 桌面 Web v3 推荐流首页 URL；续页用响应里的 paging.next。 */
const val RECOMMEND_FEED_URL =
    "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"

/** 桌面 Web v3 关注流（动态）首页 URL；续页用响应里的 paging.next。 */
const val MOMENTS_FEED_URL =
    "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"

private const val RECENT_MOMENTS_USERS_URL = "https://api.zhihu.com/moments/recent?type=raw"

/** 已知无法作为独立 feed 条目展示的响应类型与广告类型，解码前直接跳过。 */
private val SKIPPED_FEED_TYPES = setOf(
    "invited_answer",
    "tab_list",
    "feed_item_index_group",
    "feed_advert",
)

open class ZhihuFeedApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 按完整 URL 拉取一页 feed 条目（问题回答流等），并解析续页游标。
     * @param include feed 字段 include 表达式，空串表示不传（桌面推荐流默认返回全量字段）
     */
    open suspend fun fetchFeedPage(
        url: String,
        include: String = FEED_INCLUDE,
    ): FeedPage {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://", "https://"), include)
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))
        val jsonArray = json["data"] as? JsonArray
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no \$.data"))
        val items = jsonArray.mapNotNull { element ->
            if ("type" in element.jsonObject &&
                element.jsonObject["type"]?.jsonPrimitive?.content in SKIPPED_FEED_TYPES
            ) {
                return@mapNotNull null
            }
            try {
                ZhihuJson.decodeJson<FeedDto>(element)
            } catch (e: Exception) {
                Log.e("ZhihuFeedApi", "Failed to decode feed item: $element", e)
                null
            }
        }
        val paging = json["paging"]?.let { ZhihuJson.decodeJson<PagingDto>(it) }
        return FeedPage(
            items = items,
            nextUrl = paging?.nextUrl,
            isEnd = paging?.hasMore != true,
        )
    }

    /**
     * 获取最近有动态的已关注用户列表。
     */
    open suspend fun getRecentFollowingUsers(): List<FollowingUserItemDto> {
        val json = environment.fetchJson(RECENT_MOMENTS_USERS_URL, "")
            ?: return emptyList()
        val dataArray = json["data"]?.jsonArray ?: return emptyList()
        return dataArray.mapNotNull { item ->
            try {
                ZhihuJson.decodeJson<FollowingUserItemDto>(item)
            } catch (e: Exception) {
                environment.logDecodeFailure("ZhihuFeedApi", item, e)
                null
            }
        }
    }

    /**
     * 上报未触碰条目为 touch 状态
     */
    open suspend fun reportContentTouch(untouchedItems: List<Pair<String, String>>) {
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
                Log.e("ZhihuFeedApi", "Touch report failed: ${response.bodyAsText()}")
            }
        } catch (e: Exception) {
            Log.w("ZhihuFeedApi", "Failed to report content touch", e)
        }
    }

    /**
     * 批量向知乎分析服务（ZA）上报推荐模型卡片曝光（携带 attached_info_bytes）
     */
    open suspend fun reportZaExposures(items: List<ZaFeedExposureItem>) {
        if (items.isEmpty()) return
        val cookies = environment.authenticatedCookies()
        val dc0 = cookies["d_c0"] ?: return
        val zap = cookies["_zap"] ?: UUID.randomUUID().toString()

        try {
            val rawProto = ZaProtobufBuilder.buildBatchPayload(
                items = items,
                clientId = zap,
                dc0Cookie = dc0,
                userAgent = DEFAULT_ZHIHU_USER_AGENT,
            )
            val gzipped = rawProto.gzipCompress()
            environment.withAuthenticatedClient { client, _ ->
                val response = client.post("https://zhihu-web-analytics.zhihu.com/api/v3inv2/za/logs/batch") {
                    header("X-ZA-Log-Version", ZaProtobufBuilder.LOG_VERSION)
                    header("X-ZA-Product", "Zhihu")
                    header("X-ZA-Platform", "DesktopWeb")
                    header("X-ZA-ClientID", zap)
                    header("X-ZA-Batch-Size", items.size.toString())
                    header("Content-Type", "application/x-protobuf")
                    header("Content-Encoding", "gzip")
                    header("Origin", "https://www.zhihu.com")
                    header("Referer", "https://www.zhihu.com/")
                    setBody(gzipped)
                }
                if (!response.status.isSuccess()) {
                    Log.w("ZhihuFeedApi", "ZA batch exposure report failed with status: ${response.status}")
                }
            }
        } catch (e: Exception) {
            Log.w("ZhihuFeedApi", "Failed to report ZA exposures", e)
        }
    }

    /**
     * 上报单个条目为 read 状态
     */
    open suspend fun reportContentRead(type: String, id: String) {
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
            Log.w("ZhihuFeedApi", "Failed to report content read", e)
        }
    }

    /**
     * 将推荐内容标记为不感兴趣
     * @param contentToken 内容 token（数字 ID 字符串）
     * @param contentType 内容数字类型编码（1: 回答, 2: 文章, 3: 视频, 5: 想法）
     */
    open suspend fun markUninterested(
        contentToken: String,
        contentType: String,
        uninterestedType: String = "less_similar",
        feedDeliverType: String = "Normal",
        sceneCode: String = "RECOMMEND",
    ): Boolean {
        if (environment.authenticatedCookies()["d_c0"] == null) return false

        return try {
            val response = environment.postSigned("https://www.zhihu.com/api/v4/zrec-feedback/uninterested") {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("scene_code", sceneCode)
                            append("content_type", contentType)
                            append("content_token", contentToken)
                            append("uninterested_type", uninterestedType)
                            append("feed_deliver_type", feedDeliverType)
                            append("desktop", "true")
                        },
                    ),
                )
            }
            if (response.status.isSuccess()) {
                val json = runCatching { response.body<JsonObject>() }.getOrNull()
                json?.get("success")?.jsonPrimitive?.booleanOrNull == true
            } else {
                Log.e("ZhihuFeedApi", "Mark uninterested failed: ${response.bodyAsText()}")
                false
            }
        } catch (e: Exception) {
            Log.w("ZhihuFeedApi", "Failed to mark content uninterested", e)
            false
        }
    }
}

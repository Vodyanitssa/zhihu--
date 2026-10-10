package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.crypto.ZhihuMessageBodyEncryptor
import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationColumnHeadDto
import com.zhihuminus.data.zhihu.dto.NotificationHeadEntryDto
import com.zhihuminus.data.zhihu.dto.NotificationOverviewDto
import com.zhihuminus.data.zhihu.dto.NotificationTimelineItemDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePageDto
import com.zhihuminus.data.zhihu.dto.ZhihuMeNotificationsDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val MOBILE_NOTIFICATION_MESSAGE_URL = "https://api.zhihu.com/notifications/v3/message/v3"
private const val MOBILE_NOTIFICATION_TIMELINE_URL = "https://api.zhihu.com/notifications/v3/timeline/entry"
private const val MOBILE_PRIVATE_MESSAGE_URL = "https://api.zhihu.com/messages"
private const val MOBILE_PRIVATE_MESSAGE_USER_URL = "https://api.zhihu.com/messages/user"

open class ZhihuNotificationApi(
    protected val environment: ZhihuApiEnvironment,
) {
    open suspend fun getNotificationOverview(nextUrl: String? = null): NotificationOverviewDto {
        @Suppress("HttpUrlsUsage")
        val url = (nextUrl ?: "$MOBILE_NOTIFICATION_MESSAGE_URL?limit=20").replace("http://", "https://")
        val response = environment.mobileHttpClient().get(url)
        val json = response.body<JsonObject>()
        val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())
        val items = rawData.mapNotNull {
            runCatching { ZhihuJson.decodeJson<NotificationTimelineItemDto>(it) }.getOrNull()
        }
        val head = json["head"]?.let {
            runCatching { ZhihuJson.decodeJson<List<NotificationHeadEntryDto>>(it) }.getOrNull()
        } ?: emptyList()
        val columnHead = (json["column_head"] ?: json["columnHead"])?.let {
            runCatching { ZhihuJson.decodeJson<List<NotificationColumnHeadDto>>(it) }.getOrNull()
        } ?: emptyList()
        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }
        return NotificationOverviewDto(
            head = head,
            columnHead = columnHead,
            data = items,
            paging = paging,
        )
    }

    open suspend fun getNotificationTimeline(entryName: String, nextUrl: String? = null): NotificationOverviewDto {
        @Suppress("HttpUrlsUsage")
        val url = (
            nextUrl ?: buildString {
                append("$MOBILE_NOTIFICATION_TIMELINE_URL/$entryName?")
                if (entryName == "invite") {
                    append("invite_with_time_slice=1&")
                }
                append("limit=20")
            }
        ).replace("http://", "https://")
        val response = environment.mobileHttpClient().get(url)
        val json = response.body<JsonObject>()
        val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())
        val items = rawData.mapNotNull {
            runCatching { ZhihuJson.decodeJson<NotificationTimelineItemDto>(it) }.getOrNull()
        }
        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }
        return NotificationOverviewDto(
            data = items,
            paging = paging,
        )
    }

    open suspend fun markNotificationCategoryRead(entryName: String): Boolean {
        val url = "$MOBILE_NOTIFICATION_TIMELINE_URL/$entryName/actions/readall"
        val response = environment.mobileHttpClient().post(url)
        return response.status.isSuccess()
    }

    open suspend fun getPrivateMessages(peerId: String, nextUrl: String? = null): PrivateMessagePageDto {
        @Suppress("HttpUrlsUsage")
        val url = (nextUrl ?: "$MOBILE_PRIVATE_MESSAGE_URL?limit=20&sender_id=$peerId").replace("http://", "https://")
        val response = environment.mobileHttpClient().get(url)
        val json = response.body<JsonObject>()
        val rawData = json["data"]?.jsonArray ?: JsonArray(emptyList())
        val items = rawData.mapNotNull {
            runCatching { ZhihuJson.decodeJson<PrivateMessageDto>(it) }.getOrNull()
        }
        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        } ?: PagingDto()
        return PrivateMessagePageDto(
            data = items,
            paging = paging,
        )
    }

    open suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthorDto {
        val url = "$MOBILE_PRIVATE_MESSAGE_USER_URL/$peerId"
        val response = environment.mobileHttpClient().get(url)
        val json = response.body<JsonObject>()
        return ZhihuJson.decodeJson<NotificationAuthorDto>(json)
    }

    open suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessageDto {
        val response = environment.mobileHttpClient().post(MOBILE_PRIVATE_MESSAGE_URL) {
            contentType(ContentType.Application.FormUrlEncoded)
            header("X-Zse-93", "101_1_1.0")
            val form = Parameters
                .build {
                    append("receiver_id", peerId)
                    append("content", content)
                    append("content_type", "0")
                    append("source_type", "message_list")
                }.formUrlEncode()
            setBody(ZhihuMessageBodyEncryptor.encrypt(form))
        }
        if (!response.status.isSuccess()) {
            val responseText = response.bodyAsText()
            val errorMsg = runCatching {
                ZhihuJson.json
                    .parseToJsonElement(responseText)
                    .jsonObject["error"]
                    ?.jsonObject
                    ?.get("message")
                    ?.jsonPrimitive
                    ?.content
            }.getOrNull() ?: "发送失败（${response.status.value}）"
            throw IllegalStateException(errorMsg)
        }
        return ZhihuJson.decodeJson<PrivateMessageDto>(
            ZhihuJson.json.parseToJsonElement(response.bodyAsText()),
        )
    }

    open suspend fun getMeNotifications(): ZhihuMeNotificationsDto {
        val json = environment.fetchJson("https://www.zhihu.com/api/v4/me", "")
            ?: throw IllegalStateException("获取个人通知信息失败")
        return ZhihuJson.decodeJson<ZhihuMeNotificationsDto>(json)
    }
}

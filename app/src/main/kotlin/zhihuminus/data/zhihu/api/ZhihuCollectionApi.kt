package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
import com.zhihuminus.data.zhihu.dto.CollectionItemsPageDto
import com.zhihuminus.data.zhihu.dto.CollectionResponseDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

open class ZhihuCollectionApi(
    protected val environment: ZhihuApiEnvironment,
) {
    /**
     * 获取内容所属的收藏夹列表
     * @param type 内容类型: "answer", "article", "pin"
     * @param id 内容 ID
     */
    open suspend fun getCollections(type: String, id: Long): CollectionResponseDto {
        val url = "https://api.zhihu.com/collections/contents/$type/$id?limit=50"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch collections")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 添加内容到收藏夹
     * @param type 内容类型: "answer", "article", "pin"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    open suspend fun addToCollection(type: String, id: Long, collectionId: String) {
        val url = "https://www.zhihu.com/api/v4/collections/$collectionId/contents?content_id=$id&content_type=$type"
        environment.postSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
        }
    }

    /**
     * 从收藏夹移除内容
     * @param type 内容类型: "answer", "article", "pin"
     * @param id 内容 ID
     * @param collectionId 收藏夹 ID
     */
    open suspend fun removeFromCollection(type: String, id: Long, collectionId: String) {
        val url = "https://www.zhihu.com/api/v4/collections/$collectionId/contents/$id?content_type=$type"
        environment.deleteSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
        }
    }

    /**
     * 创建收藏夹
     * @param title 标题
     * @param description 描述
     * @param isPublic 是否公开
     */
    open suspend fun createCollection(title: String, description: String, isPublic: Boolean): CollectionDto {
        val url = "https://www.zhihu.com/api/v4/collections"
        val response = environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("title", title)
                    put("description", description)
                    put("is_public", isPublic)
                },
            )
        }
        if (!response.status.isSuccess()) {
            error("创建收藏夹失败：${response.status}")
        }
        val responseBody = response.body<JsonObject>()
        val collectionObj = responseBody["collection"] as? JsonObject
        if (responseBody["status"]?.jsonPrimitive?.intOrNull != 100 || collectionObj == null) {
            error(
                responseBody["message"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf(String::isNotBlank)
                    ?: "创建收藏夹失败：响应无效",
            )
        }
        return ZhihuJson.decodeJson(collectionObj)
    }

    /**
     * 获取指定用户的收藏夹列表
     * @param urlToken 用户 urlToken
     * @param nextUrl 分页 URL（为空时加载第一页）
     */
    open suspend fun getUserCollections(urlToken: String, nextUrl: String? = null): CollectionResponseDto {
        val url = nextUrl ?: "https://www.zhihu.com/api/v4/people/$urlToken/collections"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("获取收藏夹列表失败")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * 删除收藏夹
     * @param collectionId 收藏夹 ID
     */
    open suspend fun deleteCollection(collectionId: String): Boolean {
        val response = environment.deleteSigned("https://www.zhihu.com/api/v4/collections/$collectionId")
        if (!response.status.isSuccess()) {
            error("删除收藏夹失败：${response.status}")
        }
        val responseBody = response.body<JsonObject>()
        if (responseBody["success"]?.jsonPrimitive?.booleanOrNull != true) {
            error(
                responseBody["message"]
                    ?.jsonPrimitive
                    ?.contentOrNull
                    ?.takeIf(String::isNotBlank)
                    ?: "删除收藏夹失败：响应无效",
            )
        }
        return true
    }

    /**
     * 获取单个收藏夹详情
     * @param collectionId 收藏夹 ID
     */
    open suspend fun getCollection(collectionId: String): CollectionDto {
        val json = environment.fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "")
            ?: error("收藏夹信息为空")
        val collectionObj = json["collection"] as? JsonObject ?: throw IllegalStateException("收藏夹信息为空")
        return ZhihuJson.decodeJson(collectionObj)
    }

    /**
     * 获取收藏夹内的条目列表（分页）
     * @param collectionId 收藏夹 ID
     * @param offset 偏移量
     * @param limit 每页数量
     * @param nextUrl 分页 URL（优先于 offset/limit）
     */
    open suspend fun getCollectionItems(
        collectionId: String,
        offset: Int? = null,
        limit: Int = 20,
        nextUrl: String? = null,
    ): CollectionItemsPageDto {
        val url = nextUrl ?: if (offset != null) {
            "https://www.zhihu.com/api/v4/collections/$collectionId/items?offset=$offset&limit=$limit"
        } else {
            "https://www.zhihu.com/api/v4/collections/$collectionId/items?limit=$limit"
        }
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("获取收藏夹内容失败")
        val paging = json["paging"]?.let { ZhihuJson.decodeJson<PagingDto>(it) } ?: PagingDto()
        val rawData = json["data"] as? JsonArray ?: JsonArray(emptyList())
        val items = runCatching {
            ZhihuJson.decodeJson<List<CollectionItemDto>>(rawData)
        }.getOrElse {
            rawData.mapNotNull { itemJson ->
                try {
                    ZhihuJson.decodeJson<CollectionItemDto>(itemJson)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return CollectionItemsPageDto(data = items, paging = paging)
    }
}

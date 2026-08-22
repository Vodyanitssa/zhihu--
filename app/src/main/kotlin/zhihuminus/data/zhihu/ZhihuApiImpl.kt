package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.VoteResponse
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import com.zhihuminus.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ZhihuApiImpl(
    private val environment: ZhihuApiEnvironment,
) : ZhihuApi {
    override suspend fun getAnswer(answerId: Long): AnswerDto {
        val url = "https://www.zhihu.com/api/v4/answers/$answerId"
        val include = "content,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch answer $answerId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun getArticle(articleId: Long): ArticleDto {
        val url = "https://www.zhihu.com/api/v4/articles/$articleId"
        val include = "content,topics,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch article $articleId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun getPin(pinId: Long): PinDto {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId"
        val include = "topics"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch pin $pinId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun vote(type: String, id: Long, vote: String): VoteResponse {
        val url = when (type) {
            "answer" -> "https://www.zhihu.com/api/v4/answers/$id/voters"
            "article" -> "https://www.zhihu.com/api/v4/articles/$id/voters"
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        val response = environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to vote))
        }

        return response.body()
    }

    override suspend fun getCollections(type: String, id: Long): CollectionResponse {
        val url = "https://api.zhihu.com/collections/contents/$type/$id?limit=50"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch collections")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun addToCollection(type: String, id: Long, collectionId: String) {
        val url = "https://api.zhihu.com/collections/contents/$type/$id"
        environment.postSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("add_collections=$collectionId")
        }
    }

    override suspend fun removeFromCollection(type: String, id: Long, collectionId: String) {
        val url = "https://api.zhihu.com/collections/contents/$type/$id"
        environment.postSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("remove_collections=$collectionId")
        }
    }

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): Collection {
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
        return response.body()
    }
}

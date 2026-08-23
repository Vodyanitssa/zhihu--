package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.VoteResponse
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import com.zhihuminus.viewmodel.deleteSigned
import com.zhihuminus.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class ZhihuApiImpl(
    private val environment: ZhihuApiEnvironment,
) : ZhihuApi {
    override suspend fun getAnswer(answerId: Long): AnswerDto {
        val url = "https://www.zhihu.com/api/v4/answers/$answerId"
        val include =
            "content,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch answer $answerId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun getArticle(articleId: Long): ArticleDto {
        val url = "https://www.zhihu.com/api/v4/articles/$articleId"
        val include =
            "content,topics,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
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

    override suspend fun likePin(pinId: Long): Int {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId/voters/up"
        val response = environment.postSigned(url)
        val json: JsonObject = response.body()
        return json["liked_count"]?.jsonPrimitive?.intOrNull ?: -1
    }

    override suspend fun submitPinPollVote(pollId: String, optionId: String) {
        val url = "https://www.zhihu.com/api/v4/polls/$pollId"
        val body = buildJsonObject {
            putJsonArray("options") {
                add(optionId)
            }
        }
        environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(body.toString())
        }
    }

    override suspend fun fetchVoters(url: String): JsonObject = environment.fetchJson(url.replace("http://", "https://"), "")
        ?: error("赞同者信息为空")

    override suspend fun followMember(urlToken: String) {
        environment.postSigned("https://www.zhihu.com/api/v4/members/$urlToken/followers")
    }

    override suspend fun unfollowMember(urlToken: String) {
        environment.deleteSigned("https://www.zhihu.com/api/v4/members/$urlToken/followers")
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
        val url = "https://www.zhihu.com/api/v4/collections/$collectionId/contents?content_id=$id&content_type=$type"
        environment.postSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
        }
    }

    override suspend fun removeFromCollection(type: String, id: Long, collectionId: String) {
        val url = "https://www.zhihu.com/api/v4/collections/$collectionId/contents/$id?content_type=$type"
        environment.deleteSigned(url) {
            contentType(ContentType.Application.FormUrlEncoded)
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

    override suspend fun fetchCommentsPage(url: String): JsonObject =
        environment.fetchJson(url, "data[*].content,excerpt,headline,target.author.badge_v2")
            ?: throw IllegalStateException("Failed to fetch comments page")

    override suspend fun getRootComments(
        contentType: String,
        contentId: Long,
        orderBy: String,
        offset: Int,
        limit: Int,
    ): JsonObject {
        val url = "https://www.zhihu.com/api/v4/comment_v5/${contentType}s/$contentId/root_comment" +
            "?order_by=$orderBy"
        return environment.fetchJson(url, "data[*].content,excerpt,headline,target.author.badge_v2")
            ?: throw IllegalStateException("Failed to fetch root comments")
    }

    override suspend fun getChildComments(commentId: String, offset: Int, limit: Int): JsonObject {
        val url = "https://www.zhihu.com/api/v4/comment_v5/comment/$commentId/child_comment" +
            "?offset=$offset&limit=$limit"
        return environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch child comments")
    }

    override suspend fun getComment(commentId: String): JsonObject {
        val url = "https://www.zhihu.com/api/v4/comment_v5/comment/$commentId"
        return environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch comment $commentId")
    }

    override suspend fun submitComment(url: String, body: JsonObject): JsonObject {
        val response = environment.postSigned(url) {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.body()
    }

    override suspend fun likeComment(commentId: String): HttpResponse =
        environment.postSigned("https://www.zhihu.com/api/v4/comments/$commentId/like")

    override suspend fun unlikeComment(commentId: String): HttpResponse =
        environment.deleteSigned("https://www.zhihu.com/api/v4/comments/$commentId/like")

    override suspend fun deleteComment(commentId: String): HttpResponse =
        environment.deleteSigned("https://www.zhihu.com/api/v4/comment_v5/comment/$commentId")

    override suspend fun addHistory(contentToken: String, contentType: String) {
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

    override suspend fun markAsRead(contentToken: String, contentType: String) {
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
}

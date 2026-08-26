package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Collection
import com.zhihuminus.data.CollectionResponse
import com.zhihuminus.data.Feed
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.util.Log
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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
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

    override suspend fun getQuestion(questionId: Long): QuestionDto {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId"
        val include =
            "read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch question $questionId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun fetchFeedPage(url: String): FeedPage {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://", "https://"), FEED_INCLUDE)
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
                ZhihuJson.decodeJson<Feed>(element)
            } catch (e: Exception) {
                Log.e("ZhihuApiImpl", "Failed to decode feed item: $element", e)
                null
            }
        }
        val paging = json["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) }
        return FeedPage(
            items = items,
            nextUrl = paging?.next?.takeIf { it.isNotEmpty() },
            isEnd = paging?.isEnd == true,
        )
    }

    override suspend fun followQuestion(questionId: Long, follow: Boolean) {
        val url = "https://www.zhihu.com/api/v4/questions/$questionId/followers"
        if (follow) {
            environment.postSigned(url)
        } else {
            environment.deleteSigned(url)
        }
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
        return json["liked_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }

    override suspend fun unlikePin(pinId: Long): Int {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId/voters/up"
        val response = environment.deleteSigned(url)
        val json: JsonObject = response.body()
        return json["liked_count"]?.jsonPrimitive?.intOrNull
            ?: -1
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

    override suspend fun voteAnswer(answerId: Long, vote: String): Int {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/answers/$answerId/voters") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to vote))
        }
        val json: JsonObject = response.body()
        return json["voteup_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }

    override suspend fun voteArticle(articleId: Long, vote: String): Int {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/articles/$articleId/voters") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("voting" to if (vote == "up") 1 else 0))
        }
        val json: JsonObject = response.body()
        return json["voteup_count"]?.jsonPrimitive?.intOrNull
            ?: -1
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

private const val FEED_INCLUDE = "data[*].content,excerpt,headline,target.author.badge_v2"

/** 已知无法作为独立 feed 条目展示的响应类型，解码前直接跳过。 */
private val SKIPPED_FEED_TYPES = setOf(
    "invited_answer",
    "tab_list",
    "feed_item_index_group",
)

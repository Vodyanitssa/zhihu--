package com.zhihuminus.data.zhihu

import com.zhihuminus.data.Feed
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.ZhihuJson.decodeJson
import com.zhihuminus.data.ZhihuPaging
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
import com.zhihuminus.data.zhihu.dto.CollectionItemsPageDto
import com.zhihuminus.data.zhihu.dto.CollectionResponseDto
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryItemDto
import com.zhihuminus.data.zhihu.dto.HistoryPage
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.util.Log
import com.zhihuminus.util.raiseForStatus
import com.zhihuminus.viewmodel.ZhihuApiEnvironment
import com.zhihuminus.viewmodel.deleteSigned
import com.zhihuminus.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
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

    override suspend fun fetchFeedPage(
        url: String,
        include: String,
    ): FeedPage {
        @Suppress("HttpUrlsUsage")
        val json = environment.fetchJson(url.replace("http://", "https://"), include)
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: not json object."))
        val jsonArray = json["data"] as? JsonArray
            ?: throw RuntimeException("您可能已被风控，请重新登录。", Exception("cause: no \$.data"))
        val items = jsonArray.mapNotNull { element ->
            // feed target 自带全文，预热内容缓存（仅回答/文章，内部对异常形态静默跳过）
            ((element as? JsonObject)?.get("target") as? JsonObject)?.let {
                PostContentCache.putFromFeedTarget(it)
            }
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

    override suspend fun getCollections(type: String, id: Long): CollectionResponseDto {
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

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): CollectionDto {
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

    override suspend fun getUserCollections(urlToken: String, nextUrl: String?): CollectionResponseDto {
        val url = nextUrl ?: "https://www.zhihu.com/api/v4/people/$urlToken/collections"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("获取收藏夹列表失败")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun deleteCollection(collectionId: String): Boolean {
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

    override suspend fun getCollection(collectionId: String): CollectionDto {
        val json = environment.fetchJson("https://www.zhihu.com/api/v4/collections/$collectionId", "")
            ?: error("收藏夹信息为空")
        val collectionObj = json["collection"] as? JsonObject ?: throw IllegalStateException("收藏夹信息为空")
        return ZhihuJson.decodeJson(collectionObj)
    }

    override suspend fun getCollectionItems(
        collectionId: String,
        offset: Int?,
        limit: Int,
        nextUrl: String?,
    ): CollectionItemsPageDto {
        val url = nextUrl ?: if (offset != null) {
            "https://www.zhihu.com/api/v4/collections/$collectionId/items?offset=$offset&limit=$limit"
        } else {
            "https://www.zhihu.com/api/v4/collections/$collectionId/items?limit=$limit"
        }
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("获取收藏夹内容失败")
        val paging = json["paging"]?.let { ZhihuJson.decodeJson<ZhihuPaging>(it) } ?: ZhihuPaging(isEnd = true, next = "")
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

    override suspend fun fetchHistoryPage(url: String): HistoryPage {
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
            runCatching { decodeJson<HistoryItemDto>(element) }.getOrNull()
        }

        return HistoryPage(items, nextUrl, isEnd)
    }

    override suspend fun deleteHistoryItems(pairs: List<HistoryDeletePairDto>) {
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

    override suspend fun clearHistory() {
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

    override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage {
        val url = nextUrl
            ?: "https://www.zhihu.com/api/v4/columns/$columnId/items?limit=10&offset=0&ws_qiangzhisafe=0"
        val json = environment.fetchJson(url, "")
            ?: throw IllegalStateException("Failed to fetch column articles for $columnId")
        return ZhihuJson.decodeJson(json)
    }

    override suspend fun getDailyLatest(): DailyStoriesResponse =
        fetchDailyStories("/latest")

    override suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse =
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

    override suspend fun getTopicDetail(topicId: String): JsonObject {
        val url = "https://www.zhihu.com/api/v5.1/topics/$topicId"
        val include = "name,excerpt,avatar_url,followers_count,questions_count,is_following,topic_id,total_pv,discuss_count"
        return environment.fetchJson(url, include)
            ?: throw IllegalStateException("话题详情响应为空")
    }

    override suspend fun getTopicFeed(url: String, include: String): JsonObject {
        @Suppress("HttpUrlsUsage")
        return environment.fetchJson(url.replace("http://", "https://"), include)
            ?: throw IllegalStateException("话题内容响应为空")
    }

    override suspend fun followTopic(topicId: String, follow: Boolean) {
        val url = "https://www.zhihu.com/api/v4/topics/$topicId/followers"
        val response = if (follow) {
            environment.postSigned(url)
        } else {
            environment.deleteSigned(url)
        }
        response.raiseForStatus()
    }
}

internal const val FEED_INCLUDE = "data[*].content,excerpt,headline,target.author.badge_v2"

/** 桌面 Web v3 推荐流首页 URL；续页用响应里的 paging.next。 */
internal const val RECOMMEND_FEED_URL =
    "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"

/** 桌面 Web v3 关注流（动态）首页 URL；续页用响应里的 paging.next。 */
internal const val MOMENTS_FEED_URL =
    "https://www.zhihu.com/api/v3/moments?limit=10&desktop=true"

/** 已知无法作为独立 feed 条目展示的响应类型与广告类型，解码前直接跳过。 */
private val SKIPPED_FEED_TYPES = setOf(
    "invited_answer",
    "tab_list",
    "feed_item_index_group",
    "feed_advert",
)

private const val DAILY_PRIMARY_API_BASE = "https://news-at.zhihu.com/api/4/stories"

// Zhihu Daily's documented Android API host can fail DNS resolution in some
// overseas networks because of Zhihu-side DNS/server configuration. Keep this
// fallback host as a narrow workaround for host-resolution failures only.
// See https://github.com/zly2006/zhihu-plus-plus/issues/417.
private const val DAILY_FALLBACK_API_BASE = "https://daily.zhihu.com/api/4/stories"

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

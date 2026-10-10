package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.PinDto
import io.ktor.client.call.body
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.putJsonArray

open class ZhihuPostApi(
    protected val environment: ZhihuApiEnvironment,
) {
    open suspend fun getAnswer(answerId: Long): AnswerDto {
        val url = "https://www.zhihu.com/api/v4/answers/$answerId"
        val include =
            "content,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch answer $answerId")
        return ZhihuJson.decodeJson(json)
    }

    open suspend fun getArticle(articleId: Long): ArticleDto {
        val url = "https://www.zhihu.com/api/v4/articles/$articleId"
        val include =
            "content,topics,excerpt,thanks_count,voteup_count,comment_count,ip_info,reaction,reaction.relation.voting,author.badge_v2,segment_infos"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch article $articleId")
        return ZhihuJson.decodeJson(json)
    }

    open suspend fun getPin(pinId: Long): PinDto {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId"
        val include = "topics"
        val json = environment.fetchJson(url, include)
            ?: throw IllegalStateException("Failed to fetch pin $pinId")
        return ZhihuJson.decodeJson(json)
    }

    /**
     * Pin 点赞
     * @param pinId Pin ID
     * @return 点赞后的赞数
     */
    open suspend fun likePin(pinId: Long): Int {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId/voters/up"
        val response = environment.postSigned(url)
        val json: JsonObject = response.body()
        return json["liked_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }

    /**
     * Pin 取消点赞
     * @param pinId Pin ID
     * @return 取消点赞后的赞数
     */
    open suspend fun unlikePin(pinId: Long): Int {
        val url = "https://www.zhihu.com/api/v4/pins/$pinId/voters/up"
        val response = environment.deleteSigned(url)
        val json: JsonObject = response.body()
        return json["liked_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }

    /**
     * Pin 投票
     * @param pollId 投票 ID
     * @param optionId 选项 ID
     */
    open suspend fun submitPinPollVote(pollId: String, optionId: String) {
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

    /**
     * 加载赞同者列表
     * @param url 赞同者 API URL
     * @return 原始 JSON 响应（包含 data 和 paging）
     */
    open suspend fun fetchVoters(url: String): JsonObject =
        environment.fetchJson(url.replace("http://", "https://"), "")
            ?: error("赞同者信息为空")

    /**
     * 回答投票
     * @param answerId 回答 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票后的赞同数
     */
    open suspend fun voteAnswer(answerId: Long, vote: String): Int {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/answers/$answerId/voters") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("type" to vote))
        }
        val json: JsonObject = response.body()
        return json["voteup_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }

    /**
     * 文章投票
     * @param articleId 文章 ID
     * @param vote 投票类型: "up", "down", "neutral"
     * @return 投票后的赞同数
     */
    open suspend fun voteArticle(articleId: Long, vote: String): Int {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/articles/$articleId/voters") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("voting" to if (vote == "up") 1 else 0))
        }
        val json: JsonObject = response.body()
        return json["voteup_count"]?.jsonPrimitive?.intOrNull
            ?: -1
    }
}

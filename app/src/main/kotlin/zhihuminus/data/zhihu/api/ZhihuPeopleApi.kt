package com.zhihuminus.data.zhihu.api

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import com.zhihuminus.core.environment.deleteSigned
import com.zhihuminus.core.environment.postSigned
import com.zhihuminus.core.util.Log
import com.zhihuminus.core.util.raiseForStatus
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowedQuestionDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicDto
import com.zhihuminus.data.zhihu.dto.MemberColumnItemDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.MemberProfileDto
import com.zhihuminus.data.zhihu.dto.PagingDto
import com.zhihuminus.data.zhihu.dto.PeoplePageDto
import com.zhihuminus.data.zhihu.dto.PinDto
import io.ktor.client.call.body
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val PEOPLE_PROFILE_INCLUDE =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"
private const val MEMBER_ANSWERS_INCLUDE =
    "data[*].is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,collapsed_by,suggest_edit,comment_count,thanks_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,excerpt,paid_info,reaction_instruction,is_labeled,label_info,relationship.is_authorized,voting,is_author,is_thanked,is_nothelp,author.badge_v2"
private const val MEMBER_ARTICLES_INCLUDE =
    "data[*].comment_count,suggest_edit,is_normal,thumbnail_extra_info,thumbnail,can_comment,comment_permission,admin_closed_comment,content,voteup_count,created,updated,upvoted_followees,voting,review_info,reaction_instruction,is_labeled,label_info,author.badge_v2;data[*].vessay_info;data[*].author.badge[?(type=best_answerer)].topics;"
private const val MEMBER_PINS_INCLUDE =
    "data[*].like_count,comment_count,created,updated,content"
private const val MEMBER_QUESTIONS_INCLUDE =
    "data[*].created,answer_count,follower_count,author,visit_count,comment_count,detail,excerpt,relationship,topics,voteup_count"
private const val MEMBER_COLLECTIONS_INCLUDE =
    "data[*].updated_time,answer_count,follower_count,creator"
private const val MEMBER_COLUMNS_INCLUDE =
    "data[*].articles_count,followers,author"
private const val MEMBER_FOLLOWERS_INCLUDE =
    "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge_v2,badge[?(type=best_answerer)].topics"
private const val MEMBER_FOLLOWING_INCLUDE =
    "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge_v2,badge[?(type=best_answerer)].topics"

open class ZhihuPeopleApi(
    protected val environment: ZhihuApiEnvironment,
    protected val feedApi: ZhihuFeedApi = ZhihuFeedApi(environment),
) {
    open suspend fun fetchMemberProfile(userTokenOrId: String): MemberProfileDto {
        val json = environment.fetchJson(
            "https://api.zhihu.com/people/$userTokenOrId",
            PEOPLE_PROFILE_INCLUDE,
        ) ?: error("用户资料为空")
        return ZhihuJson.decodeJson<MemberProfileDto>(json)
    }

    open suspend fun fetchMemberProfileDetail(userTokenOrId: String): MemberProfileDto? {
        val json = environment.fetchJson("https://api.zhihu.com/people/$userTokenOrId/profile/detail", "")
            ?: return null
        return ZhihuJson.decodeJson<MemberProfileDto>(json)
    }

    open suspend fun followMember(urlToken: String): Int? {
        val response = environment.postSigned("https://www.zhihu.com/api/v4/members/$urlToken/followers")
        val json = response.raiseForStatus().body<JsonObject>()
        return json["follower_count"]?.jsonPrimitive?.intOrNull
    }

    open suspend fun unfollowMember(urlToken: String): Int? {
        val response = environment.deleteSigned("https://www.zhihu.com/api/v4/members/$urlToken/followers")
        val json = response.raiseForStatus().body<JsonObject>()
        return json["follower_count"]?.jsonPrimitive?.intOrNull
    }

    open suspend fun followMember(urlToken: String, follow: Boolean) {
        val endpoint = "https://www.zhihu.com/api/v4/members/$urlToken/followers"
        val response = if (follow) environment.postSigned(endpoint) else environment.deleteSigned(endpoint)
        response.raiseForStatus()
    }

    open suspend fun blockMember(urlToken: String) {
        environment.postSigned("https://www.zhihu.com/api/v4/members/$urlToken/actions/block").raiseForStatus()
    }

    open suspend fun unblockMember(urlToken: String) {
        environment.deleteSigned("https://www.zhihu.com/api/v4/members/$urlToken/actions/block").raiseForStatus()
    }

    open suspend fun fetchMemberActivities(
        userTokenOrId: String,
        nextUrl: String?,
    ): FeedPage =
        feedApi.fetchFeedPage(
            url = nextUrl ?: "https://www.zhihu.com/api/v3/moments/$userTokenOrId/activities",
            include = "",
        )

    open suspend fun fetchMemberAnswers(
        userTokenOrId: String,
        sortBy: String = "created",
        nextUrl: String? = null,
    ): PeoplePageDto<AnswerDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/answers?sort_by=$sortBy",
            include = MEMBER_ANSWERS_INCLUDE,
        )

    open suspend fun fetchMemberArticles(
        userTokenOrId: String,
        sortBy: String = "created",
        nextUrl: String? = null,
    ): PeoplePageDto<ArticleDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/articles?sort_by=$sortBy",
            include = MEMBER_ARTICLES_INCLUDE,
        )

    open suspend fun fetchMemberPins(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<PinDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/v2/pins/$userTokenOrId/moments",
            include = MEMBER_PINS_INCLUDE,
        )

    open suspend fun fetchMemberQuestions(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<FollowedQuestionDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/questions",
            include = MEMBER_QUESTIONS_INCLUDE,
        )

    open suspend fun fetchMemberCollections(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<CollectionDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/favlists",
            include = MEMBER_COLLECTIONS_INCLUDE,
        )

    open suspend fun fetchMemberColumns(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<MemberColumnItemDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/column-contributions",
            include = MEMBER_COLUMNS_INCLUDE,
        )

    open suspend fun fetchMemberFollowers(
        memberId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<MemberItemDto> =
        fetchPaged(
            url = nextUrl ?: "https://api.zhihu.com/people/$memberId/followers",
            include = MEMBER_FOLLOWERS_INCLUDE,
        )

    open suspend fun fetchMemberFollowing(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<MemberItemDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/followees",
            include = MEMBER_FOLLOWING_INCLUDE,
        )

    open suspend fun fetchMemberFollowingColumns(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<MemberColumnItemDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/following-columns",
            include = MEMBER_COLUMNS_INCLUDE,
        )

    open suspend fun fetchMemberFollowingTopics(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<FollowedTopicDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/following-topic-contributions",
            include = "",
        )

    open suspend fun fetchMemberFollowingQuestions(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<FollowedQuestionDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/following-questions",
            include = MEMBER_QUESTIONS_INCLUDE,
        )

    open suspend fun fetchMemberFollowingCollections(
        userTokenOrId: String,
        nextUrl: String? = null,
    ): PeoplePageDto<CollectionDto> =
        fetchPaged(
            url = nextUrl ?: "https://www.zhihu.com/api/v4/members/$userTokenOrId/following-favlists",
            include = MEMBER_COLLECTIONS_INCLUDE,
        )

    private suspend inline fun <reified T> fetchPaged(
        url: String,
        include: String = "",
    ): PeoplePageDto<T> {
        val json = environment.fetchJson(url.replace("http://", "https://"), include)
            ?: throw IllegalStateException("获取数据失败")
        val rawData = json["data"] as? JsonArray ?: JsonArray(emptyList())
        val items = rawData.mapNotNull { element ->
            try {
                ZhihuJson.decodeJson<T>(element)
            } catch (e: Exception) {
                Log.e("ZhihuPeopleApi", "Failed to decode ${T::class.simpleName}: $element", e)
                null
            }
        }
        val paging = json["paging"]?.let {
            runCatching { ZhihuJson.decodeJson<PagingDto>(it) }.getOrNull()
        }
        return PeoplePageDto(
            items = items,
            nextUrl = paging?.nextUrl,
            isEnd = paging?.hasMore != true,
        )
    }
}

package com.zhihuminus.data.zhihu.api

import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowedQuestionDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicDto
import com.zhihuminus.data.zhihu.dto.MemberColumnItemDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.MemberProfileDto
import com.zhihuminus.data.zhihu.dto.PeoplePageDto
import com.zhihuminus.data.zhihu.dto.PinDto

interface ZhihuPeopleApi {
    /**
     * 关注用户
     * @param urlToken 用户 urlToken
     * @return 最新的粉丝数（若响应包含）
     */
    suspend fun followMember(urlToken: String): Int?

    /**
     * 取消关注用户
     * @param urlToken 用户 urlToken
     * @return 最新的粉丝数（若响应包含）
     */
    suspend fun unfollowMember(urlToken: String): Int?

    /**
     * 拉黑用户
     * @param urlToken 用户 urlToken
     */
    suspend fun blockMember(urlToken: String)

    /**
     * 取消拉黑用户
     * @param urlToken 用户 urlToken
     */
    suspend fun unblockMember(urlToken: String)

    /**
     * 获取用户基本资料
     */
    suspend fun fetchMemberProfile(userTokenOrId: String): MemberProfileDto

    /**
     * 获取用户详细资料（用于提取 GitHub 等社交媒体信息）
     */
    suspend fun fetchMemberProfileDetail(userTokenOrId: String): MemberProfileDto?

    /**
     * 获取用户的动态列表（活动流）
     */
    suspend fun fetchMemberActivities(userTokenOrId: String, nextUrl: String? = null): FeedPage

    /**
     * 获取用户的回答列表
     */
    suspend fun fetchMemberAnswers(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePageDto<AnswerDto>

    /**
     * 获取用户的文章列表
     */
    suspend fun fetchMemberArticles(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePageDto<ArticleDto>

    /**
     * 获取用户的想法列表
     */
    suspend fun fetchMemberPins(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<PinDto>

    /**
     * 获取用户的提问列表
     */
    suspend fun fetchMemberQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<FollowedQuestionDto>

    /**
     * 获取用户创建的收藏夹列表
     */
    suspend fun fetchMemberCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<CollectionDto>

    /**
     * 获取用户的专栏列表
     */
    suspend fun fetchMemberColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<MemberColumnItemDto>

    /**
     * 获取用户的粉丝列表
     */
    suspend fun fetchMemberFollowers(memberId: String, nextUrl: String? = null): PeoplePageDto<MemberItemDto>

    /**
     * 获取用户关注的人列表
     */
    suspend fun fetchMemberFollowing(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<MemberItemDto>

    /**
     * 获取用户订阅的专栏列表
     */
    suspend fun fetchMemberFollowingColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<MemberColumnItemDto>

    /**
     * 获取用户关注的话题列表
     */
    suspend fun fetchMemberFollowingTopics(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<FollowedTopicDto>

    /**
     * 获取用户关注的问题列表
     */
    suspend fun fetchMemberFollowingQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<FollowedQuestionDto>

    /**
     * 获取用户关注的收藏夹列表
     */
    suspend fun fetchMemberFollowingCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePageDto<CollectionDto>
}

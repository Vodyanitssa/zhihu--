package com.zhihuminus.data

import com.zhihuminus.data.zhihu.ZhihuApi
import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.CollectionDto
import com.zhihuminus.data.zhihu.dto.CollectionItemsPageDto
import com.zhihuminus.data.zhihu.dto.CollectionResponseDto
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import com.zhihuminus.data.zhihu.dto.DailyStoriesResponse
import com.zhihuminus.data.zhihu.dto.FeedPage
import com.zhihuminus.data.zhihu.dto.FollowedQuestionDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicDto
import com.zhihuminus.data.zhihu.dto.FollowingUserItemDto
import com.zhihuminus.data.zhihu.dto.HistoryDeletePairDto
import com.zhihuminus.data.zhihu.dto.HistoryPage
import com.zhihuminus.data.zhihu.dto.MemberColumnItemDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.MemberProfileDto
import com.zhihuminus.data.zhihu.dto.NotificationAuthorDto
import com.zhihuminus.data.zhihu.dto.NotificationOverviewDto
import com.zhihuminus.data.zhihu.dto.PeoplePageDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.data.zhihu.dto.PrivateMessageDto
import com.zhihuminus.data.zhihu.dto.PrivateMessagePageDto
import com.zhihuminus.data.zhihu.dto.QuestionDto
import com.zhihuminus.data.zhihu.dto.ZhihuMeNotificationsDto
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.JsonObject

open class FakeZhihuApi : ZhihuApi {
    override suspend fun getAnswer(answerId: Long): AnswerDto = TODO()

    override suspend fun getQuestion(questionId: Long): QuestionDto = TODO()

    override suspend fun fetchFeedPage(url: String, include: String): FeedPage = TODO()

    override suspend fun getRecentFollowingUsers(): List<FollowingUserItemDto> = emptyList()

    override suspend fun followQuestion(questionId: Long, follow: Boolean) = TODO()

    override suspend fun getArticle(articleId: Long): ArticleDto = TODO()

    override suspend fun getPin(pinId: Long): PinDto = TODO()

    override suspend fun likePin(pinId: Long): Int = TODO()

    override suspend fun unlikePin(pinId: Long): Int = TODO()

    override suspend fun submitPinPollVote(pollId: String, optionId: String) = TODO()

    override suspend fun fetchVoters(url: String): JsonObject = TODO()

    override suspend fun followMember(urlToken: String): Int? = TODO()

    override suspend fun unfollowMember(urlToken: String): Int? = TODO()

    override suspend fun blockMember(urlToken: String) = TODO()

    override suspend fun unblockMember(urlToken: String) = TODO()

    override suspend fun voteAnswer(answerId: Long, vote: String): Int = TODO()

    override suspend fun voteArticle(articleId: Long, vote: String): Int = TODO()

    override suspend fun getCollections(type: String, id: Long): CollectionResponseDto = TODO()

    override suspend fun addToCollection(type: String, id: Long, collectionId: String) = TODO()

    override suspend fun removeFromCollection(type: String, id: Long, collectionId: String) = TODO()

    override suspend fun createCollection(title: String, description: String, isPublic: Boolean): CollectionDto = TODO()

    override suspend fun getUserCollections(urlToken: String, nextUrl: String?): CollectionResponseDto = TODO()

    override suspend fun deleteCollection(collectionId: String): Boolean = TODO()

    override suspend fun getCollection(collectionId: String): CollectionDto = TODO()

    override suspend fun getCollectionItems(
        collectionId: String,
        offset: Int?,
        limit: Int,
        nextUrl: String?,
    ): CollectionItemsPageDto = TODO()

    override suspend fun fetchCommentsPage(url: String): JsonObject = TODO()

    override suspend fun getRootComments(
        contentType: String,
        contentId: Long,
        orderBy: String,
        offset: Int,
        limit: Int,
    ): JsonObject = TODO()

    override suspend fun getChildComments(commentId: String, offset: Int, limit: Int): JsonObject = TODO()

    override suspend fun getComment(commentId: String): JsonObject = TODO()

    override suspend fun submitComment(url: String, body: JsonObject): JsonObject = TODO()

    override suspend fun likeComment(commentId: String): HttpResponse = TODO()

    override suspend fun unlikeComment(commentId: String): HttpResponse = TODO()

    override suspend fun deleteComment(commentId: String): HttpResponse = TODO()

    override suspend fun addHistory(contentToken: String, contentType: String) = TODO()

    override suspend fun markAsRead(contentToken: String, contentType: String) = TODO()

    override suspend fun fetchHistoryPage(url: String): HistoryPage = TODO()

    override suspend fun deleteHistoryItems(pairs: List<HistoryDeletePairDto>) = TODO()

    override suspend fun clearHistory() = TODO()

    override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage = TODO()

    override suspend fun getDailyLatest(): DailyStoriesResponse = TODO()

    override suspend fun getDailyStoriesBefore(date: String): DailyStoriesResponse = TODO()

    override suspend fun getTopicDetail(topicId: String): JsonObject = TODO()

    override suspend fun getTopicFeed(url: String, include: String): JsonObject = TODO()

    override suspend fun followTopic(topicId: String, follow: Boolean) = TODO()

    override suspend fun getNotificationOverview(nextUrl: String?): NotificationOverviewDto = TODO()

    override suspend fun getNotificationTimeline(entryName: String, nextUrl: String?): NotificationOverviewDto = TODO()

    override suspend fun markNotificationCategoryRead(entryName: String): Boolean = TODO()

    override suspend fun getPrivateMessages(peerId: String, nextUrl: String?): PrivateMessagePageDto = TODO()

    override suspend fun getPrivateMessagePeer(peerId: String): NotificationAuthorDto = TODO()

    override suspend fun sendPrivateMessage(peerId: String, content: String): PrivateMessageDto = TODO()

    override suspend fun getMeNotifications(): ZhihuMeNotificationsDto = TODO()

    override suspend fun fetchMemberProfile(userTokenOrId: String): MemberProfileDto = TODO()

    override suspend fun fetchMemberProfileDetail(userTokenOrId: String): MemberProfileDto? = TODO()

    override suspend fun fetchMemberActivities(userTokenOrId: String, nextUrl: String?): FeedPage = TODO()

    override suspend fun fetchMemberAnswers(userTokenOrId: String, sortBy: String, nextUrl: String?): PeoplePageDto<AnswerDto> = TODO()

    override suspend fun fetchMemberArticles(userTokenOrId: String, sortBy: String, nextUrl: String?): PeoplePageDto<ArticleDto> = TODO()

    override suspend fun fetchMemberPins(userTokenOrId: String, nextUrl: String?): PeoplePageDto<PinDto> = TODO()

    override suspend fun fetchMemberQuestions(userTokenOrId: String, nextUrl: String?): PeoplePageDto<FollowedQuestionDto> = TODO()

    override suspend fun fetchMemberCollections(userTokenOrId: String, nextUrl: String?): PeoplePageDto<CollectionDto> = TODO()

    override suspend fun fetchMemberColumns(userTokenOrId: String, nextUrl: String?): PeoplePageDto<MemberColumnItemDto> = TODO()

    override suspend fun fetchMemberFollowers(memberId: String, nextUrl: String?): PeoplePageDto<MemberItemDto> = TODO()

    override suspend fun fetchMemberFollowing(userTokenOrId: String, nextUrl: String?): PeoplePageDto<MemberItemDto> = TODO()

    override suspend fun fetchMemberFollowingColumns(userTokenOrId: String, nextUrl: String?): PeoplePageDto<MemberColumnItemDto> = TODO()

    override suspend fun fetchMemberFollowingTopics(userTokenOrId: String, nextUrl: String?): PeoplePageDto<FollowedTopicDto> = TODO()

    override suspend fun fetchMemberFollowingQuestions(userTokenOrId: String, nextUrl: String?): PeoplePageDto<FollowedQuestionDto> = TODO()

    override suspend fun fetchMemberFollowingCollections(userTokenOrId: String, nextUrl: String?): PeoplePageDto<CollectionDto> = TODO()
}

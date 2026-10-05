package com.zhihuminus.data.zhihu

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.officialBadge
import com.zhihuminus.data.officialBadgeDetails
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.PeoplePage
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.feature.people.PeopleRepository
import com.zhihuminus.feature.people.githubSocialUiState
import com.zhihuminus.feature.people.toPeopleAnswerDisplayItem
import com.zhihuminus.feature.people.toPeopleArticleDisplayItem
import com.zhihuminus.feature.people.toPeoplePinDisplayItem

class ZhihuPeopleRepository(
    private val api: ZhihuApi,
) : PeopleRepository {
    override suspend fun getProfile(userTokenOrId: String): PeopleProfile {
        val raw = api.fetchMemberProfile(userTokenOrId)
        runCatching {
            api.addHistory(raw.id, "profile")
        }
        val detail = runCatching {
            api.fetchMemberProfileDetail(userTokenOrId)
        }.onFailure {
            Log.e("ZhihuPeopleRepository", "Failed to load optional profile detail", it)
        }.getOrNull()

        val social = detail?.githubSocialUiState() ?: raw.githubSocialUiState()
        return PeopleProfile(
            id = raw.id,
            urlToken = raw.urlToken ?: "",
            name = raw.name,
            avatarUrl = raw.avatarUrl,
            headline = raw.headline,
            officialBadge = raw.badgeV2.officialBadge(),
            officialBadgeDetails = raw.badgeV2.officialBadgeDetails(),
            githubSocial = social,
            followerCount = raw.followerCount,
            followingCount = raw.followingCount,
            answerCount = raw.answerCount,
            articleCount = raw.articlesCount,
            isFollowing = raw.isFollowing,
            isBlocking = raw.isBlocking,
        )
    }

    override suspend fun follow(urlToken: String): Int? = api.followMember(urlToken)

    override suspend fun unfollow(urlToken: String): Int? = api.unfollowMember(urlToken)

    override suspend fun block(urlToken: String) {
        api.blockMember(urlToken)
    }

    override suspend fun unblock(urlToken: String) {
        api.unblockMember(urlToken)
    }

    override suspend fun getAnswers(
        userTokenOrId: String,
        sortBy: String,
        nextUrl: String?,
    ): PeoplePage<FeedDisplayItem> {
        val dto = api.fetchMemberAnswers(userTokenOrId, sortBy, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleAnswerDisplayItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getArticles(
        userTokenOrId: String,
        sortBy: String,
        nextUrl: String?,
    ): PeoplePage<FeedDisplayItem> {
        val dto = api.fetchMemberArticles(userTokenOrId, sortBy, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleArticleDisplayItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getActivities(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FeedDisplayItem> {
        val feedPage = api.fetchMemberActivities(userTokenOrId, nextUrl)
        val items = feedPage.items.flattenFeeds().map { it.toDisplayItem() }
        return PeoplePage(
            items = items,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd,
        )
    }

    override suspend fun getPins(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FeedDisplayItem> {
        val dto = api.fetchMemberPins(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeoplePinDisplayItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getCollections(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.Collection> {
        val dto = api.fetchMemberCollections(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getQuestions(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.Question> {
        val dto = api.fetchMemberQuestions(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getColumns(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.Column> {
        val dto = api.fetchMemberColumns(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowers(
        memberId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.People> {
        val dto = api.fetchMemberFollowers(memberId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowing(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.People> {
        val dto = api.fetchMemberFollowing(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingColumns(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.Column> {
        val dto = api.fetchMemberFollowingColumns(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingTopics(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FollowedTopic> {
        val dto = api.fetchMemberFollowingTopics(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map {
                FollowedTopic(
                    id = it.displayId,
                    name = it.displayName,
                    avatarUrl = it.displayAvatarUrl,
                )
            },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingQuestions(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FollowedQuestion> {
        val dto = api.fetchMemberFollowingQuestions(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map {
                FollowedQuestion(
                    id = it.id,
                    title = it.title,
                    type = it.type,
                    url = it.url,
                    questionType = it.questionType,
                    created = it.created,
                    updatedTime = it.updatedTime,
                )
            },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingCollections(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<DataHolder.Collection> {
        val dto = api.fetchMemberFollowingCollections(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items,
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }
}

package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.cache.PostContentCache
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.mapper.PeopleMapper
import com.zhihuminus.data.zhihu.mapper.flattenFeeds
import com.zhihuminus.data.zhihu.mapper.toDisplayItem
import com.zhihuminus.data.zhihu.mapper.toDomain
import com.zhihuminus.data.zhihu.mapper.toFollowedQuestion
import com.zhihuminus.data.zhihu.mapper.toFollowedTopic
import com.zhihuminus.data.zhihu.mapper.toPeopleColumnItem
import com.zhihuminus.data.zhihu.mapper.toPeopleCreationItem
import com.zhihuminus.data.zhihu.mapper.toPeopleMemberItem
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.PeopleColumnItem
import com.zhihuminus.feature.people.PeopleCreationItem
import com.zhihuminus.feature.people.PeopleMemberItem
import com.zhihuminus.feature.people.PeoplePage
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.feature.people.PeopleRepository
import com.zhihuminus.feature.people.PeopleSortOrder

class ZhihuPeopleRepository(
    private val api: ZhihuPeopleApi,
    private val historyApi: ZhihuHistoryApi? = null,
    private val postCache: PostContentCache = PostContentCache,
) : PeopleRepository {
    override suspend fun getProfile(userTokenOrId: String): PeopleProfile {
        val raw = api.fetchMemberProfile(userTokenOrId)
        runCatching {
            historyApi?.addHistory(raw.id, "profile")
        }
        val detail = runCatching {
            api.fetchMemberProfileDetail(userTokenOrId)
        }.onFailure {
            Log.e("ZhihuPeopleRepository", "Failed to load optional profile detail", it)
        }.getOrNull()

        return PeopleMapper.toProfile(raw, detail)
    }

    override suspend fun follow(urlToken: String): Int? {
        val result = api.followMember(urlToken)
        postCache.updateAuthorFollowing(urlToken, true)
        return result
    }

    override suspend fun unfollow(urlToken: String): Int? {
        val result = api.unfollowMember(urlToken)
        postCache.updateAuthorFollowing(urlToken, false)
        return result
    }

    override suspend fun block(urlToken: String) {
        api.blockMember(urlToken)
    }

    override suspend fun unblock(urlToken: String) {
        api.unblockMember(urlToken)
    }

    override suspend fun getAnswers(
        userTokenOrId: String,
        sortBy: PeopleSortOrder,
        nextUrl: String?,
    ): PeoplePage<PeopleCreationItem> {
        val dto = api.fetchMemberAnswers(userTokenOrId, sortBy.key, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleCreationItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getArticles(
        userTokenOrId: String,
        sortBy: PeopleSortOrder,
        nextUrl: String?,
    ): PeoplePage<PeopleCreationItem> {
        val dto = api.fetchMemberArticles(userTokenOrId, sortBy.key, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleCreationItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getActivities(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FeedDisplayItem> {
        val feedPage = api.fetchMemberActivities(userTokenOrId, nextUrl)
        val items = feedPage.items.flattenFeeds().map { feed ->
            val item = feed.toDisplayItem()
            val target = feed.target
            if (feed.sourceLabel != null && target?.detailsText != null) {
                item.copy(details = target.detailsText)
            } else {
                item
            }
        }
        return PeoplePage(
            items = items,
            nextUrl = feedPage.nextUrl,
            isEnd = feedPage.isEnd,
        )
    }

    override suspend fun getPins(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<PeopleCreationItem> {
        val dto = api.fetchMemberPins(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleCreationItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getCollections(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<Collection> {
        val dto = api.fetchMemberCollections(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toDomain() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getQuestions(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<FollowedQuestion> {
        val dto = api.fetchMemberQuestions(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toFollowedQuestion() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getColumns(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<PeopleColumnItem> {
        val dto = api.fetchMemberColumns(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleColumnItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowers(
        memberId: String,
        nextUrl: String?,
    ): PeoplePage<PeopleMemberItem> {
        val dto = api.fetchMemberFollowers(memberId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleMemberItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowing(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<PeopleMemberItem> {
        val dto = api.fetchMemberFollowing(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleMemberItem() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingColumns(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<PeopleColumnItem> {
        val dto = api.fetchMemberFollowingColumns(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toPeopleColumnItem() },
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
            items = dto.items.map { it.toFollowedTopic() },
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
            items = dto.items.map { it.toFollowedQuestion() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getFollowingCollections(
        userTokenOrId: String,
        nextUrl: String?,
    ): PeoplePage<Collection> {
        val dto = api.fetchMemberFollowingCollections(userTokenOrId, nextUrl)
        return PeoplePage(
            items = dto.items.map { it.toDomain() },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }
}

package com.zhihuminus.data.zhihu

import com.zhihuminus.core.util.Log
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.flattenFeeds
import com.zhihuminus.data.toDisplayItem
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.PeopleColumnItem
import com.zhihuminus.feature.people.PeopleCreationItem
import com.zhihuminus.feature.people.PeopleMemberItem
import com.zhihuminus.feature.people.PeoplePage
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.feature.people.PeopleRepository
import com.zhihuminus.feature.post.PostType
import org.jsoup.Jsoup

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

        val social = detail?.socialMedias?.firstNotNullOfOrNull { it.toGithubSocialUiState() }
            ?: raw.socialMedias.firstNotNullOfOrNull { it.toGithubSocialUiState() }
        return PeopleProfile(
            id = raw.id,
            urlToken = raw.urlToken ?: "",
            name = raw.name,
            avatarUrl = raw.avatarUrl,
            headline = raw.headline,
            officialBadge = raw.badgeV2?.toOfficialBadge(),
            officialBadgeDetails = raw.badgeV2?.toOfficialBadgeDetails().orEmpty(),
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
    ): PeoplePage<PeopleCreationItem> {
        val dto = api.fetchMemberAnswers(userTokenOrId, sortBy, nextUrl)
        return PeoplePage(
            items = dto.items.map {
                PeopleCreationItem(
                    id = it.id,
                    type = PostType.Answer,
                    title = it.question.title,
                    summary = it.excerpt,
                    details = "回答 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                    authorName = it.author.name,
                    authorBio = it.author.headline,
                    avatarUrl = it.author.avatarUrl,
                    publishTimeSeconds = it.createdTime.takeIf { t -> t > 0 },
                )
            },
            nextUrl = dto.nextUrl,
            isEnd = dto.isEnd,
        )
    }

    override suspend fun getArticles(
        userTokenOrId: String,
        sortBy: String,
        nextUrl: String?,
    ): PeoplePage<PeopleCreationItem> {
        val dto = api.fetchMemberArticles(userTokenOrId, sortBy, nextUrl)
        return PeoplePage(
            items = dto.items.map {
                PeopleCreationItem(
                    id = it.id,
                    type = PostType.Article,
                    title = it.title,
                    summary = it.excerpt,
                    details = "文章 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                    authorName = it.author.name,
                    authorBio = it.author.headline,
                    avatarUrl = it.author.avatarUrl,
                    publishTimeSeconds = it.created.takeIf { t -> t > 0 },
                )
            },
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
            items = dto.items.map {
                val text = it.excerptTitle.takeIf { s -> s.isNotBlank() }?.let { s -> Jsoup.parse(s).text() }
                    ?: it.contentHtml.takeIf { s -> s.isNotBlank() }?.let { s -> Jsoup.parse(s).text() }
                    ?: ""
                val images = it.content.filter { c -> c.type == "image" }.mapNotNull { c -> c.thumbnail ?: c.url }
                PeopleCreationItem(
                    id = it.id.toLongOrNull() ?: 0L,
                    type = PostType.Pin,
                    title = "",
                    summary = text,
                    details = "想法 · ${it.likeCount} 赞 · ${it.commentCount} 评论",
                    authorName = it.author.name,
                    authorBio = it.author.headline,
                    avatarUrl = it.author.avatarUrl,
                    publishTimeSeconds = it.created.takeIf { t -> t > 0 },
                    pinImages = images,
                )
            },
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
            items = dto.items.map {
                FollowedQuestion(
                    id = it.id,
                    title = it.title,
                    type = it.type,
                    url = it.url,
                    questionType = it.questionType,
                    created = it.created,
                    updatedTime = it.updatedTime,
                    answerCount = it.answerCount,
                    followerCount = it.followerCount,
                    authorName = it.author?.name.orEmpty(),
                    avatarUrl = it.author?.avatarUrl.orEmpty(),
                    excerpt = it.excerpt.ifEmpty {
                        it.detail
                            .takeIf { d -> d.isNotBlank() }
                            ?.let { d -> Jsoup.parse(d).text() }
                            .orEmpty()
                    },
                )
            },
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
            items = dto.items.map {
                PeopleColumnItem(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    articleCount = it.articlesCount,
                    followerCount = it.followerCount.coerceAtLeast(it.followers),
                    url = it.url,
                )
            },
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
            items = dto.items.map {
                PeopleColumnItem(
                    id = it.id,
                    title = it.title,
                    description = it.description,
                    articleCount = it.articlesCount,
                    followerCount = it.followerCount.coerceAtLeast(it.followers),
                    url = it.url,
                )
            },
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
                    answerCount = it.answerCount,
                    followerCount = it.followerCount,
                    authorName = it.author?.name.orEmpty(),
                    avatarUrl = it.author?.avatarUrl.orEmpty(),
                    excerpt = it.excerpt.ifEmpty {
                        it.detail
                            .takeIf { d -> d.isNotBlank() }
                            ?.let { d -> Jsoup.parse(d).text() }
                            .orEmpty()
                    },
                )
            },
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

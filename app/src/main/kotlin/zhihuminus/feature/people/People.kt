package com.zhihuminus.feature.people

import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.OfficialBadge
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.collection.Collection
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question

data class PeopleProfile(
    val id: String = "",
    val urlToken: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val headline: String = "",
    val officialBadge: OfficialBadge? = null,
    val officialBadgeDetails: List<OfficialBadge> = emptyList(),
    val githubSocial: GithubSocialUiState? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val answerCount: Int = 0,
    val articleCount: Int = 0,
    val isFollowing: Boolean = false,
    val isBlocking: Boolean = false,
) {
    val userTokenOrId: String
        get() = urlToken.takeIf { it.isNotBlank() } ?: id
}

data class GithubSocialUiState(
    val title: String,
    val starCount: String,
    val profileUrl: String,
    val iconUrl: String? = null,
)

data class PeoplePage<T>(
    val items: List<T>,
    val nextUrl: String?,
    val isEnd: Boolean,
)

data class FollowedTopic(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
)

data class FollowedQuestion(
    val id: String,
    val title: String,
    val type: String = "question",
    val url: String = "",
    val questionType: String = "",
    val created: Long = 0L,
    val updatedTime: Long = 0L,
    val answerCount: Int = 0,
    val followerCount: Int = 0,
    val authorName: String = "",
    val avatarUrl: String = "",
    val excerpt: String = "",
) {
    fun toFeedDisplayItem(
        fallbackAuthorName: String? = null,
        fallbackAvatarUrl: String? = null,
    ): FeedDisplayItem = FeedDisplayItem(
        title = title,
        summary = excerpt.takeIf { it.isNotBlank() },
        details = "问题 · $followerCount 关注 · $answerCount 回答",
        avatarSrc = avatarUrl.ifEmpty { fallbackAvatarUrl.orEmpty() }.takeIf { it.isNotBlank() },
        authorName = authorName.ifEmpty { fallbackAuthorName.orEmpty() }.takeIf { it.isNotBlank() },
        contentTypeLabel = "问题",
        publishTimeSeconds = created.takeIf { it > 0 },
        navDestinationJson = id.toLongOrNull()?.let { qId ->
            Question(questionId = qId, title = title).toFeedDisplayItemNavDestinationJson()
        },
    )
}

data class PeopleCreationItem(
    val id: Long,
    val type: PostType,
    val title: String,
    val summary: String,
    val details: String,
    val authorName: String = "",
    val authorBio: String = "",
    val avatarUrl: String = "",
    val publishTimeSeconds: Long? = null,
    val pinImages: List<String> = emptyList(),
) {
    fun toDestination(): PostDestination = PostDestination(
        type = type,
        id = id,
        title = title,
        authorName = authorName,
        authorBio = authorBio,
        avatarSrc = avatarUrl,
        excerpt = summary,
    )
}

data class PeopleMemberItem(
    val id: String,
    val urlToken: String = "",
    val name: String,
    val avatarUrl: String = "",
    val headline: String = "",
    val officialBadge: OfficialBadge? = null,
    val answerCount: Int = 0,
    val articleCount: Int = 0,
    val followerCount: Int = 0,
    val isFollowing: Boolean = false,
)

data class PeopleColumnItem(
    val id: String,
    val title: String,
    val description: String = "",
    val articleCount: Int = 0,
    val followerCount: Int = 0,
    val url: String = "",
) {
    val webUrl: String
        get() = when {
            url.contains("/api/v4/columns/") ->
                url.replace("http://", "https://").replace("/api/v4/columns/", "/column/")
            url.startsWith("http") && !url.contains("/api/") -> url.replace("http://", "https://")
            else -> "https://www.zhihu.com/column/$id"
        }
}

enum class PeoplePrimaryTab(
    val title: String,
) {
    Creations("创作"),
    Activities("动态"),
    Collections("收藏"),
    FollowingSubscriptions("关注订阅"),
    ;

    companion object {
        val TITLES = entries.map { it.title }
    }
}

enum class PeopleCreationTab(
    val title: String,
) {
    Answers("回答"),
    Articles("文章"),
    Pins("想法"),
    Columns("专栏"),
    Questions("提问"),
    ;

    companion object {
        val TITLES = entries.map { it.title }
    }
}

enum class PeopleUserListType(
    val title: String,
) {
    Following("关注"),
    Followers("粉丝"),
    ;

    companion object {
        val TITLES = entries.map { it.title }
    }
}

data class PeopleInitialSelection(
    val primaryTab: PeoplePrimaryTab = PeoplePrimaryTab.Activities,
    val creationTab: PeopleCreationTab = PeopleCreationTab.Answers,
    val initialUserListType: PeopleUserListType? = null,
)

fun resolvePeopleInitialSelection(jumpTo: String): PeopleInitialSelection = when (jumpTo) {
    "回答" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Answers)
    "文章" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Articles)
    "想法" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Pins)
    "专栏" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Columns)
    "提问" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Questions)
    "创作" -> PeopleInitialSelection(PeoplePrimaryTab.Creations, PeopleCreationTab.Answers)
    "动态" -> PeopleInitialSelection(PeoplePrimaryTab.Activities)
    "收藏" -> PeopleInitialSelection(PeoplePrimaryTab.Collections)
    "关注订阅" -> PeopleInitialSelection(PeoplePrimaryTab.FollowingSubscriptions)
    "关注" -> PeopleInitialSelection(PeoplePrimaryTab.Activities, initialUserListType = PeopleUserListType.Following)
    "粉丝" -> PeopleInitialSelection(PeoplePrimaryTab.Activities, initialUserListType = PeopleUserListType.Followers)
    else -> PeopleInitialSelection(PeoplePrimaryTab.Activities)
}

@Deprecated("Use PeoplePrimaryTab and PeopleCreationTab instead")
enum class PeopleTab(
    val title: String,
) {
    Answers("回答"),
    Articles("文章"),
    Activities("动态"),
    Collections("收藏"),
    Questions("提问"),
    Pins("想法"),
    Columns("专栏"),
    Followers("粉丝"),
    Following("关注"),
    FollowingSubscriptions("关注订阅"),
    ;

    companion object {
        val TITLES = entries.map { it.title }
    }
}

enum class PeopleSubscriptionTab(
    val title: String,
) {
    Columns("我订阅的专栏"),
    Topics("关注的话题"),
    Questions("关注的问题"),
    Collections("关注的收藏夹"),
    ;

    companion object {
        val TITLES = entries.map { it.title }
    }
}

val OfficialBadge.peopleDetailTitle: String
    get() = when {
        title == "认证" || title == "已认证的个人" -> "认证信息"
        else -> title
    }

interface PeopleRepository {
    suspend fun getProfile(userTokenOrId: String): PeopleProfile

    suspend fun follow(urlToken: String): Int?

    suspend fun unfollow(urlToken: String): Int?

    suspend fun block(urlToken: String)

    suspend fun unblock(urlToken: String)

    suspend fun getAnswers(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePage<PeopleCreationItem>

    suspend fun getArticles(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePage<PeopleCreationItem>

    suspend fun getActivities(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FeedDisplayItem>

    suspend fun getPins(userTokenOrId: String, nextUrl: String? = null): PeoplePage<PeopleCreationItem>

    suspend fun getCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePage<Collection>

    suspend fun getQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FollowedQuestion>

    suspend fun getColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePage<PeopleColumnItem>

    suspend fun getFollowers(memberId: String, nextUrl: String? = null): PeoplePage<PeopleMemberItem>

    suspend fun getFollowing(userTokenOrId: String, nextUrl: String? = null): PeoplePage<PeopleMemberItem>

    suspend fun getFollowingColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePage<PeopleColumnItem>

    suspend fun getFollowingTopics(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FollowedTopic>

    suspend fun getFollowingQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FollowedQuestion>

    suspend fun getFollowingCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePage<Collection>
}

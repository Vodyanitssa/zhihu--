package com.zhihuminus.feature.people

import com.zhihuminus.data.DataHolder
import com.zhihuminus.data.FeedDisplayItem
import com.zhihuminus.data.OfficialBadge
import com.zhihuminus.data.feedThumbnailUrl
import com.zhihuminus.data.toFeedDisplayItemNavDestinationJson
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import org.jsoup.Jsoup

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
)

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

interface PeopleRepository {
    suspend fun getProfile(userTokenOrId: String): PeopleProfile

    suspend fun follow(urlToken: String): Int?

    suspend fun unfollow(urlToken: String): Int?

    suspend fun block(urlToken: String)

    suspend fun unblock(urlToken: String)

    suspend fun getAnswers(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePage<FeedDisplayItem>

    suspend fun getArticles(userTokenOrId: String, sortBy: String, nextUrl: String? = null): PeoplePage<FeedDisplayItem>

    suspend fun getActivities(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FeedDisplayItem>

    suspend fun getPins(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FeedDisplayItem>

    suspend fun getCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.Collection>

    suspend fun getQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.Question>

    suspend fun getColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.Column>

    suspend fun getFollowers(memberId: String, nextUrl: String? = null): PeoplePage<DataHolder.People>

    suspend fun getFollowing(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.People>

    suspend fun getFollowingColumns(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.Column>

    suspend fun getFollowingTopics(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FollowedTopic>

    suspend fun getFollowingQuestions(userTokenOrId: String, nextUrl: String? = null): PeoplePage<FollowedQuestion>

    suspend fun getFollowingCollections(userTokenOrId: String, nextUrl: String? = null): PeoplePage<DataHolder.Collection>
}

fun DataHolder.Answer.toPeopleAnswerDisplayItem(): FeedDisplayItem {
    val destination = PostDestination(
        type = PostType.Answer,
        id = id,
        title = question.title,
        authorName = author.name,
        authorBio = author.headline,
        avatarSrc = author.avatarUrl,
        excerpt = excerpt,
    )
    return FeedDisplayItem(
        title = question.title,
        summary = excerpt,
        details = "回答 · $voteupCount 赞同 · $commentCount 评论",
        feed = null,
        navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
        raw = this,
        contentTypeLabel = "回答",
        publishTimeSeconds = createdTime.takeIf { it > 0 },
    )
}

fun DataHolder.Article.toPeopleArticleDisplayItem(): FeedDisplayItem {
    val destination = PostDestination(
        type = PostType.Article,
        id = id,
        title = title,
        authorName = author.name,
        authorBio = author.headline,
        avatarSrc = author.avatarUrl,
        excerpt = excerpt,
    )
    return FeedDisplayItem(
        title = title,
        summary = excerpt,
        details = "文章 · $voteupCount 赞同 · $commentCount 评论",
        feed = null,
        navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
        raw = this,
        contentTypeLabel = "文章",
        publishTimeSeconds = created.takeIf { it > 0 },
    )
}

fun DataHolder.Pin.toPeoplePinDisplayItem(): FeedDisplayItem {
    val text = excerptTitle.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it).text() }
        ?: contentHtml.takeIf { it.isNotBlank() }?.let { Jsoup.parse(it).text() }
        ?: ""
    val destination = PostDestination(
        type = PostType.Pin,
        id = id.toLongOrNull() ?: 0L,
        authorName = author.name,
        authorBio = author.headline,
        avatarSrc = author.avatarUrl,
        excerpt = text,
    )
    val pinImages = content.filterIsInstance<DataHolder.Pin.ContentImage>().map { it.feedThumbnailUrl }
    return FeedDisplayItem(
        title = "",
        summary = text,
        details = "想法 · $likeCount 赞 · $commentCount 评论",
        avatarSrc = author.avatarUrl,
        authorName = author.name,
        feed = null,
        navDestinationJson = destination.toFeedDisplayItemNavDestinationJson(),
        raw = this,
        contentTypeLabel = "想法",
        publishTimeSeconds = created.takeIf { it > 0 },
        pinImages = pinImages,
    )
}

fun DataHolder.Column.webUrl(): String = when {
    url.contains("/api/v4/columns/") ->
        url
            .replace("http://", "https://")
            .replace("/api/v4/columns/", "/column/")

    url.startsWith("http") && !url.contains("/api/") -> url.replace("http://", "https://")
    else -> "https://www.zhihu.com/column/$id"
}

val OfficialBadge.peopleDetailTitle: String
    get() = when {
        title == "认证" || title == "已认证的个人" -> "认证信息"
        else -> title
    }

fun DataHolder.People.githubSocialUiState(): GithubSocialUiState? =
    socialMedias.firstNotNullOfOrNull { media ->
        if (!media.title.startsWith("GitHub", ignoreCase = true)) {
            return@firstNotNullOfOrNull null
        }
        val starCount = media.modules
            .firstOrNull { it.title.equals("stars", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() }
            ?: return@firstNotNullOfOrNull null
        val profileLink = media.link.takeIf { it.isNotBlank() }
            ?: return@firstNotNullOfOrNull null
        val profileUrl = if (profileLink.startsWith("zhihu://", ignoreCase = true)) {
            val username = media.title
                .substringAfter('·', missingDelimiterValue = "")
                .trim()
                .takeIf { it.isNotBlank() }
                ?: return@firstNotNullOfOrNull null
            "https://github.com/$username"
        } else {
            profileLink
        }

        GithubSocialUiState(
            title = media.title,
            starCount = starCount,
            profileUrl = profileUrl,
            iconUrl = media.icon.takeIf { it.isNotBlank() },
        )
    }

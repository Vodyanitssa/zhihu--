package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.zhihu.dto.AnswerDto
import com.zhihuminus.data.zhihu.dto.ArticleDto
import com.zhihuminus.data.zhihu.dto.FollowedQuestionDto
import com.zhihuminus.data.zhihu.dto.FollowedTopicDto
import com.zhihuminus.data.zhihu.dto.MemberColumnItemDto
import com.zhihuminus.data.zhihu.dto.MemberItemDto
import com.zhihuminus.data.zhihu.dto.MemberProfileDto
import com.zhihuminus.data.zhihu.dto.MemberSocialMediaDto
import com.zhihuminus.data.zhihu.dto.PinDto
import com.zhihuminus.feature.people.FollowedQuestion
import com.zhihuminus.feature.people.FollowedTopic
import com.zhihuminus.feature.people.GithubSocialUiState
import com.zhihuminus.feature.people.PeopleColumnItem
import com.zhihuminus.feature.people.PeopleCreationItem
import com.zhihuminus.feature.people.PeopleMemberItem
import com.zhihuminus.feature.people.PeopleProfile
import com.zhihuminus.feature.post.PostType
import org.jsoup.Jsoup

/**
 * 知乎用户与主页相关数据模型映射器。
 *
 * 负责将用户资料、社交账号、各类创作（回答、文章、想法）及关注内容等 DTO 转换为领域模型。
 */
object PeopleMapper {
    fun toSocial(dto: MemberSocialMediaDto): GithubSocialUiState? {
        if (!dto.title.startsWith("GitHub", ignoreCase = true)) return null
        val starCount = dto.modules
            .firstOrNull { it.title.equals("stars", ignoreCase = true) }
            ?.value
            ?.takeIf { it.isNotBlank() } ?: return null
        val profileLink = dto.link.takeIf { it.isNotBlank() } ?: return null
        val profileUrl = if (profileLink.startsWith("zhihu://", ignoreCase = true)) {
            val username = dto.title
                .substringAfter('·', "")
                .trim()
                .takeIf { it.isNotBlank() } ?: return null
            "https://github.com/$username"
        } else {
            profileLink
        }
        return GithubSocialUiState(
            title = dto.title,
            starCount = starCount,
            profileUrl = profileUrl,
            iconUrl = dto.icon.takeIf { it.isNotBlank() },
        )
    }

    fun toPeopleMemberItem(dto: MemberItemDto): PeopleMemberItem = PeopleMemberItem(
        id = dto.id,
        urlToken = dto.urlToken ?: "",
        name = dto.name,
        avatarUrl = dto.avatarUrl,
        headline = dto.headline,
        officialBadge = dto.badgeV2?.toOfficialBadge(),
        answerCount = dto.answerCount,
        articleCount = dto.articlesCount,
        followerCount = dto.followerCount,
        isFollowing = dto.isFollowing,
    )

    fun toProfile(raw: MemberProfileDto, detail: MemberProfileDto?): PeopleProfile {
        val social = detail?.socialMedias?.firstNotNullOfOrNull { toSocial(it) }
            ?: raw.socialMedias.firstNotNullOfOrNull { toSocial(it) }
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
            pinsCount = raw.pinsCount,
            questionCount = raw.questionCount,
            isFollowing = raw.isFollowing,
            isFollowed = raw.isFollowed,
            isBlocking = raw.isBlocking,
            voteupCount = raw.voteupCount,
            thankedCount = raw.thankedCount,
            favoritedCount = raw.favoritedCount,
            ipInfo = raw.ipInfo,
            description = raw.description,
            gender = raw.gender,
        )
    }

    fun toCreationItem(dto: AnswerDto): PeopleCreationItem {
        val thumbnail = dto.thumbnail?.takeIf { s -> s.isNotBlank() }
        return PeopleCreationItem(
            id = dto.id,
            type = PostType.Answer,
            title = dto.question.title,
            summary = dto.excerpt,
            details = "回答 · ${dto.voteupCount} 赞同 · ${dto.commentCount} 评论",
            authorName = dto.author.name,
            authorBio = dto.author.headline,
            avatarUrl = dto.author.avatarUrl,
            publishTimeSeconds = dto.createdTime.takeIf { t -> t > 0 },
            images = listOfNotNull(thumbnail),
        )
    }

    fun toCreationItem(dto: ArticleDto): PeopleCreationItem {
        val thumbnail = (dto.thumbnail ?: dto.imageUrl)?.takeIf { s -> s.isNotBlank() }
        return PeopleCreationItem(
            id = dto.id,
            type = PostType.Article,
            title = dto.title,
            summary = dto.excerpt,
            details = "文章 · ${dto.voteupCount} 赞同 · ${dto.commentCount} 评论",
            authorName = dto.author.name,
            authorBio = dto.author.headline,
            avatarUrl = dto.author.avatarUrl,
            publishTimeSeconds = dto.created.takeIf { t -> t > 0 },
            images = listOfNotNull(thumbnail),
        )
    }

    fun toCreationItem(dto: PinDto): PeopleCreationItem {
        val text = dto.excerptTitle.takeIf { s -> s.isNotBlank() }?.let { s -> Jsoup.parse(s).text() }
            ?: dto.contentHtml.takeIf { s -> s.isNotBlank() }?.let { s -> Jsoup.parse(s).text() }
            ?: ""
        val images = dto.content
            .filter { c -> c.type == "image" }
            .mapNotNull { c -> c.thumbnail?.takeIf { s -> s.isNotBlank() } ?: c.url?.takeIf { s -> s.isNotBlank() } }
        return PeopleCreationItem(
            id = dto.id.toLongOrNull() ?: 0L,
            type = PostType.Pin,
            title = "",
            summary = text,
            details = "想法 · ${dto.likeCount} 赞 · ${dto.commentCount} 评论",
            authorName = dto.author.name,
            authorBio = dto.author.headline,
            avatarUrl = dto.author.avatarUrl,
            publishTimeSeconds = dto.created.takeIf { t -> t > 0 },
            images = images,
        )
    }

    fun toColumnItem(dto: MemberColumnItemDto): PeopleColumnItem = PeopleColumnItem(
        id = dto.id,
        title = dto.title,
        description = dto.description,
        articleCount = dto.articlesCount,
        followerCount = dto.followerCount.coerceAtLeast(dto.followers),
        url = dto.url,
    )

    fun toFollowedTopic(dto: FollowedTopicDto): FollowedTopic = FollowedTopic(
        id = dto.displayId,
        name = dto.displayName,
        avatarUrl = dto.displayAvatarUrl,
    )

    fun toFollowedQuestion(dto: FollowedQuestionDto): FollowedQuestion = FollowedQuestion(
        id = dto.id,
        title = dto.title,
        type = dto.type,
        url = dto.url,
        questionType = dto.questionType,
        created = dto.created,
        updatedTime = dto.updatedTime,
        answerCount = dto.answerCount,
        followerCount = dto.followerCount,
        authorName = dto.author?.name.orEmpty(),
        avatarUrl = dto.author?.avatarUrl.orEmpty(),
        excerpt = dto.excerpt.ifEmpty {
            dto.detail
                .takeIf { d -> d.isNotBlank() }
                ?.let { d -> Jsoup.parse(d).text() }
                .orEmpty()
        },
    )
}

fun MemberSocialMediaDto.toGithubSocialUiState(): GithubSocialUiState? = PeopleMapper.toSocial(this)

fun MemberItemDto.toPeopleMemberItem(): PeopleMemberItem = PeopleMapper.toPeopleMemberItem(this)

fun AnswerDto.toPeopleCreationItem(): PeopleCreationItem = PeopleMapper.toCreationItem(this)

fun ArticleDto.toPeopleCreationItem(): PeopleCreationItem = PeopleMapper.toCreationItem(this)

fun PinDto.toPeopleCreationItem(): PeopleCreationItem = PeopleMapper.toCreationItem(this)

fun MemberColumnItemDto.toPeopleColumnItem(): PeopleColumnItem = PeopleMapper.toColumnItem(this)

fun FollowedTopicDto.toFollowedTopic(): FollowedTopic = PeopleMapper.toFollowedTopic(this)

fun FollowedQuestionDto.toFollowedQuestion(): FollowedQuestion = PeopleMapper.toFollowedQuestion(this)

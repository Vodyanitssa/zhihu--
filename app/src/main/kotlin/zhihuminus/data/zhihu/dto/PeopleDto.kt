package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.OfficialBadge
import kotlinx.serialization.Serializable

@Serializable
data class MemberBadgeItemDto(
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val icon: String = "",
    val nightIcon: String = "",
    val url: String = "",
    val detailType: String = "",
    val badgeStatus: String? = null,
)

@Serializable
data class MemberBadgeV2Dto(
    val title: String = "",
    val icon: String = "",
    val nightIcon: String = "",
    val detailBadges: List<MemberBadgeItemDto>? = null,
    val mergedBadges: List<MemberBadgeItemDto>? = null,
) {
    fun toOfficialBadge(): OfficialBadge? {
        val details = toOfficialBadgeDetails()
        val primary = details.firstOrNull { it.type != "identity" && it.iconUrl.isNotBlank() }
            ?: details.firstOrNull { it.iconUrl.isNotBlank() }
            ?: return null
        return primary.copy(
            iconUrl = icon.ifBlank { primary.iconUrl },
            nightIconUrl = nightIcon.ifBlank { primary.nightIconUrl },
        )
    }

    fun toOfficialBadgeDetails(): List<OfficialBadge> {
        val badges = detailBadges?.takeIf { it.isNotEmpty() } ?: mergedBadges.orEmpty()
        return badges.mapNotNull { badge ->
            if (badge.badgeStatus != null && badge.badgeStatus != "passed") return@mapNotNull null
            val bTitle = badge.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            OfficialBadge(
                title = bTitle,
                description = badge.description.ifBlank { bTitle },
                iconUrl = badge.icon,
                nightIconUrl = badge.nightIcon,
                url = badge.url,
                type = badge.type,
                detailType = badge.detailType,
            )
        }
    }
}

@Serializable
data class MemberSocialMediaModuleDto(
    val title: String = "",
    val value: String = "",
)

@Serializable
data class MemberSocialMediaDto(
    val type: String = "",
    val title: String = "",
    val link: String = "",
    val icon: String = "",
    val modules: List<MemberSocialMediaModuleDto> = emptyList(),
)

@Serializable
data class MemberProfileDto(
    val id: String = "",
    val urlToken: String? = null,
    val name: String = "",
    val avatarUrl: String = "",
    val headline: String = "",
    val badgeV2: MemberBadgeV2Dto? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val answerCount: Int = 0,
    val articlesCount: Int = 0,
    val isFollowing: Boolean = false,
    val isBlocking: Boolean = false,
    val socialMedias: List<MemberSocialMediaDto> = emptyList(),
)

@Serializable
data class MemberItemDto(
    val id: String = "",
    val urlToken: String? = null,
    val name: String = "",
    val avatarUrl: String = "",
    val headline: String = "",
    val badgeV2: MemberBadgeV2Dto? = null,
    val answerCount: Int = 0,
    val articlesCount: Int = 0,
    val followerCount: Int = 0,
    val isFollowing: Boolean = false,
)

@Serializable
data class MemberColumnItemDto(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val articlesCount: Int = 0,
    val followers: Int = 0,
    val followerCount: Int = 0,
    val url: String = "",
)

@Serializable
data class FollowedQuestionDto(
    val id: String,
    val type: String = "question",
    val url: String = "",
    val title: String = "",
    val questionType: String = "",
    val created: Long = 0L,
    val updatedTime: Long = 0L,
    val answerCount: Int = 0,
    val followerCount: Int = 0,
    val author: AuthorDto? = null,
    val excerpt: String = "",
    val detail: String = "",
)

@Serializable
data class FollowedTopicItemDto(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
)

@Serializable
data class FollowedTopicDto(
    val id: String = "",
    val type: String = "topic",
    val url: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val topic: FollowedTopicItemDto? = null,
) {
    val displayId: String get() = topic?.id?.takeIf { it.isNotBlank() } ?: id
    val displayName: String get() = topic?.name?.takeIf { it.isNotBlank() } ?: name
    val displayAvatarUrl: String? get() = topic?.avatarUrl ?: avatarUrl
}

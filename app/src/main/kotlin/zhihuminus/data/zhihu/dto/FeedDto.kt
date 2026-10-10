package com.zhihuminus.data.zhihu.dto

import com.zhihuminus.data.common.BooleanCompatSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonNames

@Serializable
sealed interface FeedDto {
    val target: FeedTargetDto?
        get() = null

    val actionText: String?
        get() = null

    val sourceLabel: String?
        get() = null
}

@Serializable
sealed interface FeedTargetDto {
    fun description(): String = when (this) {
        is AnswerTargetDto -> "回答"
        is VideoTargetDto -> "视频"
        is ArticleTargetDto -> "文章"
        is PinTargetDto -> "想法"
        is QuestionTargetDto -> "问题"
    }

    val detailsText: String
    val title: String
    val url: String
    val excerpt: String?
    val author: FeedAuthorDto?
    val createdTime: Long
    val updatedTime: Long
}

internal object LegacyFeedAuthorSerCompat : KSerializer<FeedAuthorDto?> {
    private val delegate = FeedAuthorDto.serializer().nullable

    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(
        encoder: Encoder,
        value: FeedAuthorDto?,
    ) {
        delegate.serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): FeedAuthorDto? = try {
        delegate.deserialize(decoder)
    } catch (_: Exception) {
        // consume string -- legacy api compatibility when author is string (e.g. empty/anonymous)
        decoder.decodeString()
        null
    }
}

@Serializable
@SerialName("answer")
data class AnswerTargetDto(
    val id: Long,
    override val url: String = "",
    @Serializable(with = LegacyFeedAuthorSerCompat::class)
    override val author: FeedAuthorDto? = null,
    override val createdTime: Long = -1,
    override val updatedTime: Long = -1,
    val voteupCount: Int = -1,
    val thanksCount: Int = -1,
    val commentCount: Int = -1,
    val isCopyable: Boolean = false,
    val question: QuestionTargetDto,
    val thumbnail: String? = null,
    override val excerpt: String? = null,
    val reshipmentSettings: String = "",
    val content: String = "",
    val relationship: FeedRelationshipDto? = null,
    val isLabeled: Boolean = false,
    val visitedCount: Int = 0,
    val thumbnails: List<String> = emptyList(),
    val favoriteCount: Int = 0,
    val answerType: String? = null,
    val segmentInfos: List<SegmentInfoParagraph> = emptyList(),
    @Serializable(with = BooleanCompatSerializer::class)
    val allowSegmentInteraction: Boolean = false,
) : FeedTargetDto {
    override val detailsText = "回答 · $voteupCount 赞同 · $commentCount 评论"
    override val title: String
        get() = question.title
}

@Serializable
@SerialName("zvideo")
data class VideoTargetDto(
    val id: Long,
    override val url: String = "",
    override val author: FeedAuthorDto? = null,
    val voteCount: Int = -1,
    val commentCount: Int = 0,
    override val title: String = "",
    val description: String = "",
    override val excerpt: String = "",
) : FeedTargetDto {
    override val detailsText = "视频 · $voteCount 赞 · $commentCount 评论"

    override val createdTime: Long
        get() = -1

    override val updatedTime: Long
        get() = -1
}

@Serializable
@SerialName("article")
data class ArticleTargetDto(
    val id: Long,
    override val url: String = "",
    override val author: FeedAuthorDto? = null,
    val voteupCount: Int = 0,
    val commentCount: Int = 0,
    override val title: String = "",
    @SerialName("excerpt")
    val rawExcerpt: String = "",
    val excerptTitle: String = "",
    val content: String = "",
    val created: Long = 0,
    val updated: Long = 0,
    val isLabeled: Boolean = false,
    val visitedCount: Int = 0,
    val favoriteCount: Int = 0,
    val segmentInfos: List<SegmentInfoParagraph> = emptyList(),
    @Serializable(with = BooleanCompatSerializer::class)
    val allowSegmentInteraction: Boolean = false,
    val voting: Int? = null,
) : FeedTargetDto {
    constructor(
        id: Long,
        url: String,
        author: FeedAuthorDto?,
        voteupCount: Int = 0,
        commentCount: Int = 0,
        title: String = "",
        excerpt: String = "",
        content: String = "",
        created: Long = 0,
        updated: Long = 0,
        isLabeled: Boolean = false,
        visitedCount: Int = 0,
        favoriteCount: Int = 0,
        segmentInfos: List<SegmentInfoParagraph> = emptyList(),
        allowSegmentInteraction: Boolean = false,
        voting: Int? = null,
    ) : this(
        id = id,
        url = url,
        author = author,
        voteupCount = voteupCount,
        commentCount = commentCount,
        title = title,
        rawExcerpt = excerpt,
        excerptTitle = "",
        content = content,
        created = created,
        updated = updated,
        isLabeled = isLabeled,
        visitedCount = visitedCount,
        favoriteCount = favoriteCount,
        segmentInfos = segmentInfos,
        allowSegmentInteraction = allowSegmentInteraction,
        voting = voting,
    )

    override val detailsText = "文章 · $voteupCount 赞 · $commentCount 评论"

    override val excerpt: String
        get() = rawExcerpt.ifEmpty { excerptTitle }

    override val updatedTime: Long
        get() = updated

    override val createdTime: Long
        get() = created
}

@Serializable
@SerialName("pin")
data class PinTargetDto(
    val id: Long,
    override val url: String = "",
    override val author: FeedAuthorDto? = null,
    val commentCount: Int = 0,
    val content: List<PinContentItemDto> = emptyList(),
    val likeCount: Int = 0,
    val excerptTitle: String = "",
    val contentHtml: String = "",
    val created: Long = 0,
    val updated: Long = 0,
    val reactionCount: Int = 0,
    val favoriteCount: Int = 0,
) : FeedTargetDto {
    override val detailsText = "想法 · $likeCount 赞 · $commentCount 评论"
    override val title: String
        get() = "想法"
    override val excerpt: String?
        get() = excerptTitle.ifEmpty { null }

    override val updatedTime: Long
        get() = updated

    override val createdTime: Long
        get() = created
}

@Suppress("PropertyName")
@Serializable
@SerialName("question")
data class QuestionTargetDto(
    val id: Long,
    @SerialName("name")
    val _name: String? = null,
    @SerialName("title")
    val _title: String? = null,
    override val url: String = "",
    val type: String = "question",
    val questionType: String = "",
    val created: Long = 0,
    val answerCount: Int = 0,
    val commentCount: Int = 0,
    val followerCount: Int = 0,
    val detail: String = "",
    override val excerpt: String = "",
    val boundTopicIds: List<Long> = emptyList(),
    val relationship: FeedRelationshipDto? = null,
    val isFollowing: Boolean = false,
    @Serializable(with = LegacyFeedAuthorSerCompat::class)
    override val author: FeedAuthorDto? = null,
) : FeedTargetDto {
    override val title: String
        get() = _title ?: _name.orEmpty()

    override val detailsText = "问题 · $followerCount 关注 · $answerCount 回答"

    override val updatedTime: Long
        get() = -1
    override val createdTime: Long
        get() = created
}

@Serializable
data class FeedBadgeDto(
    val type: String = "",
    val description: String = "",
)

@Serializable
@SerialName("feed_advert")
class AdvertisementFeedDto(
    override val actionText: String = "",
    val ad: Ad,
) : FeedDto {
    @Serializable
    class ExtInfo(
        val title: String,
        val description: String,
        val thumbnail: String? = null,
    )

    @Serializable
    class Ad(
        val creatives: List<Creative> = emptyList(),
    )

    @Serializable
    class Creative(
        val landingUrl: String,
        val title: String,
        val description: String,
    )
}

@Serializable
@SerialName("feed_group")
class GroupFeedDto(
    val id: String = "",
    val attachedInfo: String = "",
    val brief: String = "",
    val groupText: String = "",
    val list: List<CommonFeedDto> = emptyList(),
    val styleType: Int = 0,
) : FeedDto {
    override val actionText: String
        get() = groupText
}

@Serializable
@SerialName("question_feed_card")
class QuestionFeedCardDto(
    val position: Int = 0,
    override val target: FeedTargetDto,
    val cursor: String = "",
    val targetType: String = "",
    val isJumpNative: Boolean = false,
    val skipCount: Boolean = false,
) : FeedDto

@Serializable
@SerialName("feed")
data class CommonFeedDto(
    val id: String = "",
    val verb: String = "possibly ads, filter me",
    val createdTime: Long = -1,
    val updatedTime: Long = -1,
    override val target: FeedTargetDto? = null,
    val brief: String = "<none>",
    val attachedInfo: String = "",
    val actionCard: Boolean = false,
    val promotionExtra: String? = null,
    val cursor: String = "",
    override val actionText: String? = null,
) : FeedDto {
    override val sourceLabel: String?
        get() = actionText?.trim()?.takeIf { it.isNotEmpty() }
}

@Serializable
@SerialName("topic_feed")
data class TopicFeedDto(
    val id: String = "",
    override val target: FeedTargetDto,
    val targetDescription: String? = null,
) : FeedDto {
    override val actionText: String?
        get() = targetDescription

    override val sourceLabel: String?
        get() = targetDescription?.trim()?.takeIf { it.isNotEmpty() }
}

@Serializable
@SerialName("feed_item_index_group")
data class FeedItemIndexGroupDto(
    val id: String = "",
    override val target: FeedTargetDto,
    val actors: List<FeedAuthorDto> = emptyList(),
    override val actionText: String = "",
    val actionTime: Long = 0L,
) : FeedDto {
    override val sourceLabel: String?
        get() = actionText.trim().takeIf { it.isNotEmpty() }
}

@Serializable
@SerialName("moments_feed")
data class MomentsFeedDto(
    val id: String = "",
    val momentDesc: String = "",
    override val target: FeedTargetDto,
    val targetType: String = "",
) : FeedDto {
    override val sourceLabel: String?
        get() = momentDesc.trim().takeIf { it.isNotEmpty() }

    @Serializable
    class FeedSource(
        val actionText: String,
        val actor: FeedSourceActor,
    )

    @Serializable
    class FeedSourceActor(
        val name: String,
        val id: String,
        val urlToken: String,
    )
}

@Serializable
data class FeedAuthorDto(
    val id: String = "",
    val url: String = "",
    val userType: String = "",
    val urlToken: String? = null,
    val name: String = "",
    val headline: String = "",
    val avatarUrl: String = "",
    val isOrg: Boolean = false,
    val gender: Int = 0,
    @OptIn(ExperimentalSerializationApi::class)
    @JsonNames("followerCount")
    val followersCount: Int = 0,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val badge: List<FeedBadgeDto>? = null,
    val badgeV2: MemberBadgeV2Dto? = null,
)

val FeedTargetDto.questionAuthor: FeedAuthorDto?
    get() = when (this) {
        is AnswerTargetDto -> question.author
        is QuestionTargetDto -> author
        else -> null
    }

@Serializable
data class FeedRelationshipDto(
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val voting: Int? = null,
)

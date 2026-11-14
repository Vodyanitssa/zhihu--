package com.zhihuminus.data.zhihu.mapper

import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.AdvertisementFeedDto
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.CommonFeedDto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.dto.GroupFeedDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionFeedCardDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import com.zhihuminus.data.zhihu.dto.TopicFeedDto
import com.zhihuminus.data.zhihu.dto.VideoInfoDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistDto
import com.zhihuminus.data.zhihu.dto.VideoPlaylistItemDto
import com.zhihuminus.data.zhihu.dto.VideoTargetDto
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.VideoKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FeedMapperTest {
    @Test
    fun testAnswerFeedToDisplayItem() {
        val answerTarget = AnswerTargetDto(
            id = 1001L,
            excerpt = "这是回答摘要",
            author = FeedAuthorDto(name = "答主小明", avatarUrl = "https://example.com/avatar.jpg"),
            question = QuestionTargetDto(id = 2002L, _title = "如何评价知乎--？"),
            voteupCount = 88,
            commentCount = 12,
            thumbnail = "https://example.com/thumb.jpg",
        )
        val feed = CommonFeedDto(
            target = answerTarget,
            actionText = "赞同了该回答",
        )

        val displayItem = feed.toDisplayItem()
        assertEquals("如何评价知乎--？", displayItem.title)
        assertEquals("这是回答摘要", displayItem.summary)
        assertEquals("答主小明", displayItem.authorName)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, displayItem.imageLayout)
        assertEquals(listOf("https://example.com/thumb.jpg"), displayItem.images)
        assertEquals("answer:1001", answerTarget.stableTargetKey)

        val destination = answerTarget.navDestination as? PostDestination
        assertNotNull(destination)
        assertEquals(1001L, destination.id)
        assertEquals(PostType.Answer, destination.type)
        assertEquals("答主小明", destination.authorName)
    }

    @Test
    fun testArticleFeedToDisplayItem() {
        val articleTarget = ArticleTargetDto(
            id = 3003L,
            title = "精读 Kotlin 协程",
            rawExcerpt = "深入浅出分析协程设计",
            author = FeedAuthorDto(name = "技术博主"),
            imageUrl = "https://example.com/cover.png",
        )
        val feed = CommonFeedDto(
            target = articleTarget,
            actionText = "发表了文章",
        )

        val displayItem = feed.toDisplayItem()
        assertEquals("精读 Kotlin 协程", displayItem.title)
        assertEquals("深入浅出分析协程设计", displayItem.summary)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, displayItem.imageLayout)
        assertEquals(listOf("https://example.com/cover.png"), displayItem.images)

        val destination = articleTarget.navDestination as? PostDestination
        assertNotNull(destination)
        assertEquals(3003L, destination.id)
        assertEquals(PostType.Article, destination.type)
    }

    @Test
    fun testPinFeedToDisplayItem() {
        val pinTarget = PinTargetDto(
            id = 4004L,
            excerptTitle = "想法摘录",
            content = listOf(
                PinContentItemDto(type = "text", content = "<p>正文文本</p>", title = "想法标题"),
                PinContentItemDto(type = "image", url = "https://example.com/pin1.jpg"),
                PinContentItemDto(type = "image", thumbnail = "https://example.com/pin2_thumb.jpg"),
            ),
            author = FeedAuthorDto(name = "想法发布者"),
        )
        val feed = CommonFeedDto(
            target = pinTarget,
        )

        val displayItem = feed.toDisplayItem()
        assertEquals("想法标题", displayItem.title)
        assertEquals("正文文本", displayItem.summary)
        assertEquals(FeedImageLayout.BOTTOM_GALLERY, displayItem.imageLayout)
        assertEquals(listOf("https://example.com/pin1.jpg", "https://example.com/pin2_thumb.jpg"), displayItem.images)

        val destination = pinTarget.navDestination as? PostDestination
        assertNotNull(destination)
        assertEquals(4004L, destination.id)
        assertEquals(PostType.Pin, destination.type)
    }

    @Test
    fun testSingleImagePinFeedUsesRightThumbnail() {
        val pinTarget = PinTargetDto(
            id = 4005L,
            content = listOf(
                PinContentItemDto(type = "image", url = "https://example.com/pin-single.jpg"),
            ),
        )
        val feed = CommonFeedDto(
            target = pinTarget,
        )

        val displayItem = feed.toDisplayItem()
        assertEquals(listOf("https://example.com/pin-single.jpg"), displayItem.images)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, displayItem.imageLayout)
    }

    @Test
    fun testAdvertisementFeedToDisplayItem() {
        val adFeed = AdvertisementFeedDto(
            actionText = "推广",
            ad = AdvertisementFeedDto.Ad(
                creatives = listOf(
                    AdvertisementFeedDto.Creative(
                        landingUrl = "https://example.com/ad",
                        title = "推广卡片标题",
                        description = "推广卡片描述",
                    ),
                ),
            ),
        )

        val displayItem = adFeed.toDisplayItem()
        assertEquals("推广卡片标题", displayItem.title)
        assertEquals("推广卡片描述", displayItem.summary)
        assertEquals("https://example.com/ad", displayItem.content)
        assertEquals("推广广告", displayItem.details)
    }

    @Test
    fun testQuestionFeedCardToDisplayItem() {
        val target = AnswerTargetDto(
            id = 5005L,
            question = QuestionTargetDto(id = 6006L, _title = "问题卡片问题"),
            excerpt = "回答卡片摘要",
            author = FeedAuthorDto(name = "知乎用户"),
        )
        val feed = QuestionFeedCardDto(
            target = target,
        )

        val displayItem = feed.toDisplayItem()
        assertEquals("问题卡片问题", displayItem.title)
        assertEquals("回答卡片摘要", displayItem.summary)
        assertEquals(target.detailsText, displayItem.details)
    }

    @Test
    fun testFlattenFeeds() {
        val f1 = CommonFeedDto(target = AnswerTargetDto(id = 1L, question = QuestionTargetDto(id = 10L)))
        val f2 = CommonFeedDto(target = AnswerTargetDto(id = 2L, question = QuestionTargetDto(id = 20L)))
        val f3 = CommonFeedDto(target = AnswerTargetDto(id = 3L, question = QuestionTargetDto(id = 30L)))
        val group = GroupFeedDto(list = listOf(f2, f3))

        val flattened = listOf(f1, group).flattenFeeds()
        assertEquals(3, flattened.size)
        assertEquals(1L, (flattened[0].target as? AnswerTargetDto)?.id)
        assertEquals(2L, (flattened[1].target as? AnswerTargetDto)?.id)
        assertEquals(3L, (flattened[2].target as? AnswerTargetDto)?.id)
    }

    @Test
    fun testVideoFeedToDisplayItem() {
        val videoTarget = VideoTargetDto(
            id = 1916898524035019106L,
            url = "https://www.zhihu.com/zvideo/1916898524035019106",
            title = "价格优势突出的RTX 5060游戏本",
            description = "本期视频我们一起聊一款价格非常有诚意的RTX 5060游戏本",
            author = FeedAuthorDto(
                name = "科技方法派",
                avatarUrl = "https://example.com/video-author.jpg",
            ),
            voteupCount = 0,
            commentCount = 0,
            playCount = 399,
            createdAt = 1749804222L,
            video = VideoInfoDto(
                thumbnail = "https://example.com/video-thumb.jpg",
                playlist = VideoPlaylistDto(
                    fhd = VideoPlaylistItemDto(url = "https://example.com/fhd.mp4", bitrate = 724),
                    hd = VideoPlaylistItemDto(url = "https://example.com/hd.mp4", bitrate = 429),
                    ld = VideoPlaylistItemDto(url = "https://example.com/ld.mp4", bitrate = 273),
                ),
            ),
        )
        val feed = TopicFeedDto(target = videoTarget)

        val displayItem = feed.toDisplayItem()
        assertEquals("价格优势突出的RTX 5060游戏本", displayItem.title)
        assertEquals("本期视频我们一起聊一款价格非常有诚意的RTX 5060游戏本", displayItem.summary)
        assertEquals("科技方法派", displayItem.authorName)
        assertEquals("https://example.com/video-author.jpg", displayItem.avatarSrc)
        assertEquals(1749804222L, displayItem.publishTimeSeconds)
        assertEquals(listOf("https://example.com/video-thumb.jpg"), displayItem.images)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, displayItem.imageLayout)
        assertEquals("视频 · 399 播放 · 0 赞 · 0 评论", displayItem.details)

        val destination = videoTarget.navDestination as? Video
        assertNotNull(destination)
        assertEquals(1916898524035019106L, destination.id)
        assertEquals(VideoKind.ZVIDEO, destination.kind)
        assertEquals("video:1916898524035019106", videoTarget.stableTargetKey)
    }

    @Test
    fun testVideoFeedParsesTopicJson() {
        val json =
            """
            {
              "type": "topic_feed",
              "target_description": "",
              "target": {
                "description": "本期视频我们一起聊一款RTX 5060游戏本。",
                "author": {
                  "id": "8f868972082064084580ea909476cd8b",
                  "name": "科技方法派",
                  "headline": "面向大众的消费级科技数码视频栏目",
                  "avatar_url": "https://example.com/avatar.jpg",
                  "is_following": false
                },
                "created_at": 1749804222,
                "title": "价格优势突出的RTX 5060游戏本",
                "updated_at": 1749810287,
                "video": {
                  "width": 1920,
                  "height": 1080,
                  "duration": 762.6,
                  "thumbnail": "https://example.com/video-thumb.jpg",
                  "playlist": {
                    "fhd": {"url": "https://example.com/fhd.mp4", "bitrate": 724},
                    "hd": {"url": "https://example.com/hd.mp4", "bitrate": 429}
                  }
                },
                "type": "zvideo",
                "id": "1916898524035019106",
                "play_count": 399,
                "comment_count": 0,
                "voteup_count": 0
              }
            }
            """.trimIndent()

        val feed = ZhihuJson.decodeJson<TopicFeedDto>(ZhihuJson.json.parseToJsonElement(json))
        val displayItem = feed.toDisplayItem()
        assertEquals("价格优势突出的RTX 5060游戏本", displayItem.title)
        assertEquals("本期视频我们一起聊一款RTX 5060游戏本。", displayItem.summary)
        assertEquals("科技方法派", displayItem.authorName)
        assertEquals(1749804222L, displayItem.publishTimeSeconds)
        assertEquals(listOf("https://example.com/video-thumb.jpg"), displayItem.images)
        assertEquals("视频 · 399 播放 · 0 赞 · 0 评论", displayItem.details)

        val destination = (feed.target as VideoTargetDto).navDestination as? Video
        assertNotNull(destination)
        assertEquals(1916898524035019106L, destination.id)
        assertEquals(VideoKind.ZVIDEO, destination.kind)
    }

    @Test
    fun testNavDestinationAndStableKey() {
        val qTarget = QuestionTargetDto(id = 7007L, _title = "纯问题")
        val qDest = qTarget.navDestination as? Question
        assertNotNull(qDest)
        assertEquals(7007L, qDest.questionId)
        assertEquals("纯问题", qDest.title)
        assertEquals("question:7007", qTarget.stableTargetKey)

        val vTarget = VideoTargetDto(id = 8008L)
        val vDest = vTarget.navDestination as? Video
        assertNotNull(vDest)
        assertEquals(8008L, vDest.id)
        assertEquals(VideoKind.ZVIDEO, vDest.kind)
        assertEquals("video:8008", vTarget.stableTargetKey)
    }
}

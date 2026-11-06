package com.zhihuminus.ui.components

import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.data.feedImageLayoutOf
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import com.zhihuminus.data.zhihu.mapper.resolveFeedImages
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedCardTest {
    @Test
    fun testFeedGalleryImageLayoutMapping() {
        assertNull(feedGalleryImageLayout(0))
        assertNull(feedGalleryImageLayout(1))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(2))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(3))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(4))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(5))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(9))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(15))
    }

    @Test
    fun testFeedImageLayoutMapping() {
        assertEquals(FeedImageLayout.NONE, feedImageLayoutOf(emptyList()))
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, feedImageLayoutOf(listOf("a")))
        assertEquals(FeedImageLayout.BOTTOM_GALLERY, feedImageLayoutOf(listOf("a", "b")))
        assertEquals(FeedImageLayout.BOTTOM_GALLERY, feedImageLayoutOf(List(9) { "img$it" }))
    }

    @Test
    fun testResolveFeedImagesMapping() {
        val questionTarget = QuestionTargetDto(id = 100L)
        val qImages = questionTarget.resolveFeedImages()
        assertEquals(emptyList(), qImages)
        assertEquals(FeedImageLayout.NONE, feedImageLayoutOf(qImages))

        val answerWithThumb = AnswerTargetDto(
            id = 101L,
            question = questionTarget,
            thumbnail = "https://example.com/answer.jpg",
        )
        val ansImages = answerWithThumb.resolveFeedImages()
        assertEquals(listOf("https://example.com/answer.jpg"), ansImages)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, feedImageLayoutOf(ansImages))

        val articleWithThumb = ArticleTargetDto(
            id = 102L,
            thumbnail = "https://example.com/article.jpg",
        )
        val artImages = articleWithThumb.resolveFeedImages()
        assertEquals(listOf("https://example.com/article.jpg"), artImages)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, feedImageLayoutOf(artImages))

        val pinWithImages = PinTargetDto(
            id = 103L,
            content = listOf(
                PinContentItemDto(type = "text", content = "hello"),
                PinContentItemDto(type = "image", thumbnail = "https://example.com/p1.jpg"),
                PinContentItemDto(type = "image", url = "https://example.com/p2.jpg"),
            ),
        )
        val pinImages = pinWithImages.resolveFeedImages()
        assertEquals(listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"), pinImages)
        assertEquals(FeedImageLayout.BOTTOM_GALLERY, feedImageLayoutOf(pinImages))

        val pinWithSingleImage = PinTargetDto(
            id = 104L,
            content = listOf(
                PinContentItemDto(type = "image", url = "https://example.com/pin-single.jpg"),
            ),
        )
        val singlePinImages = pinWithSingleImage.resolveFeedImages()
        assertEquals(listOf("https://example.com/pin-single.jpg"), singlePinImages)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, feedImageLayoutOf(singlePinImages))
    }

    @Test
    fun testFeedCardConfigDefaults() {
        val defaultConfig = FeedCardConfig()
        assertEquals(100, defaultConfig.fontSizePercent)
        assertEquals(160, defaultConfig.lineHeightPercent)
    }

    @Test
    fun testFeedCardConfigCustomValues() {
        val customConfig = FeedCardConfig(
            fontSizePercent = 120,
            lineHeightPercent = 180,
        )
        assertEquals(120, customConfig.fontSizePercent)
        assertEquals(180, customConfig.lineHeightPercent)
    }

    @Test
    fun testFormatStatsText() {
        assertEquals("主页", formatStatsText("主页", ""))
        assertEquals("主页", formatStatsText("主页", "主页"))
        assertEquals("主页 · 42 粉丝", formatStatsText("主页", "42 粉丝"))
        assertEquals("回答 · 10 赞同", formatStatsText("回答", "10 赞同"))
        assertEquals("回答", formatStatsText("回答", ""))
        assertEquals("10 赞同", formatStatsText(null, "10 赞同"))
    }
}

package com.zhihuminus.ui.components

import com.zhihuminus.data.FeedImageLayout
import com.zhihuminus.data.resolveFeedImages
import com.zhihuminus.data.zhihu.dto.AnswerTargetDto
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.PinContentItemDto
import com.zhihuminus.data.zhihu.dto.PinTargetDto
import com.zhihuminus.data.zhihu.dto.QuestionTargetDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedCardTest {
    @Test
    fun testFeedGalleryImageLayoutMapping() {
        assertNull(feedGalleryImageLayout(0))
        assertEquals(FeedGalleryImageLayout.SINGLE, feedGalleryImageLayout(1))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(2))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(3))
        assertEquals(FeedGalleryImageLayout.MULTI_ROW, feedGalleryImageLayout(4))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(5))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(9))
        assertEquals(FeedGalleryImageLayout.NINE_GRID, feedGalleryImageLayout(15))
    }

    @Test
    fun testResolveFeedImagesMapping() {
        val questionTarget = QuestionTargetDto(id = 100L)
        val (qImages, qLayout) = questionTarget.resolveFeedImages()
        assertEquals(emptyList(), qImages)
        assertEquals(FeedImageLayout.NONE, qLayout)

        val answerWithThumb = AnswerTargetDto(
            id = 101L,
            question = questionTarget,
            thumbnail = "https://example.com/answer.jpg",
        )
        val (ansImages, ansLayout) = answerWithThumb.resolveFeedImages()
        assertEquals(listOf("https://example.com/answer.jpg"), ansImages)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, ansLayout)

        val articleWithThumb = ArticleTargetDto(
            id = 102L,
            thumbnail = "https://example.com/article.jpg",
        )
        val (artImages, artLayout) = articleWithThumb.resolveFeedImages()
        assertEquals(listOf("https://example.com/article.jpg"), artImages)
        assertEquals(FeedImageLayout.RIGHT_THUMBNAIL, artLayout)

        val pinWithImages = PinTargetDto(
            id = 103L,
            content = listOf(
                PinContentItemDto(type = "text", content = "hello"),
                PinContentItemDto(type = "image", thumbnail = "https://example.com/p1.jpg"),
                PinContentItemDto(type = "image", url = "https://example.com/p2.jpg"),
            ),
        )
        val (pinImages, pinLayout) = pinWithImages.resolveFeedImages()
        assertEquals(listOf("https://example.com/p1.jpg", "https://example.com/p2.jpg"), pinImages)
        assertEquals(FeedImageLayout.BOTTOM_GALLERY, pinLayout)
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
}

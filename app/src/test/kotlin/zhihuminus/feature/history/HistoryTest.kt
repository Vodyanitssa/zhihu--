package com.zhihuminus.feature.history

import com.zhihuminus.data.zhihu.dto.HistoryActionDto
import com.zhihuminus.data.zhihu.dto.HistoryContentDto
import com.zhihuminus.data.zhihu.dto.HistoryDataDto
import com.zhihuminus.data.zhihu.dto.HistoryExtraDto
import com.zhihuminus.data.zhihu.dto.HistoryHeaderDto
import com.zhihuminus.data.zhihu.dto.HistoryItemDto
import com.zhihuminus.data.zhihu.dto.HistoryMatrixDataDto
import com.zhihuminus.data.zhihu.dto.HistoryMatrixItemDto
import com.zhihuminus.data.zhihu.repository.toHistoryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class HistoryTest {
    @Test
    fun testHistoryItemCoverImageMapping() {
        val dto = HistoryItemDto(
            cardType = "normal",
            data = HistoryDataDto(
                header = HistoryHeaderDto(icon = "", title = "Test Title"),
                content = HistoryContentDto(
                    authorName = "Author",
                    summary = "Summary text",
                    coverImage = "https://example.com/cover.jpg",
                ),
                action = HistoryActionDto(type = "link", url = "https://www.zhihu.com/question/1/answer/2"),
                extra = HistoryExtraDto(
                    contentToken = "2",
                    contentType = "answer",
                    readTime = 123L,
                    questionToken = "1",
                ),
            ),
        )
        val item = dto.toHistoryItem()
        assertEquals("https://example.com/cover.jpg", item.coverImage)
        assertEquals("Test Title", item.title)
        assertEquals("Summary text", item.summary)
        assertEquals("Author", item.authorName)
        assertEquals("回答", item.contentTypeLabel)
        assertEquals("", item.details)
    }

    @Test
    fun testHistoryItemProfileMapping() {
        val dto = HistoryItemDto(
            cardType = "normal",
            data = HistoryDataDto(
                header = HistoryHeaderDto(icon = "", title = "用户昵称"),
                content = HistoryContentDto(
                    authorName = null,
                    summary = "个人签名",
                    coverImage = null,
                ),
                action = HistoryActionDto(type = "link", url = "zhihu://people/some-user"),
                extra = HistoryExtraDto(
                    contentToken = "some-user",
                    contentType = "profile",
                    readTime = 123L,
                    questionToken = "",
                ),
            ),
        )
        val item = dto.toHistoryItem()
        assertEquals("用户昵称", item.title)
        assertEquals("个人签名", item.summary)
        assertEquals("主页", item.contentTypeLabel)
        assertEquals("", item.details)
        assertEquals("profile", item.contentType)
    }

    @Test
    fun testHistoryItemWithMatrixDetails() {
        val dto = HistoryItemDto(
            cardType = "normal",
            data = HistoryDataDto(
                header = HistoryHeaderDto(icon = "", title = "问题标题"),
                action = HistoryActionDto(type = "link", url = "https://www.zhihu.com/question/123"),
                extra = HistoryExtraDto(
                    contentToken = "123",
                    contentType = "question",
                    readTime = 123L,
                    questionToken = "123",
                ),
                matrix = listOf(
                    HistoryMatrixItemDto(
                        type = "text",
                        data = HistoryMatrixDataDto(text = "100 关注 · 50 回答"),
                    ),
                ),
            ),
        )
        val item = dto.toHistoryItem()
        assertEquals("问题", item.contentTypeLabel)
        assertEquals("100 关注 · 50 回答", item.details)
    }
}

package com.zhihuminus.feature.history

import com.zhihuminus.data.zhihu.dto.HistoryActionDto
import com.zhihuminus.data.zhihu.dto.HistoryContentDto
import com.zhihuminus.data.zhihu.dto.HistoryDataDto
import com.zhihuminus.data.zhihu.dto.HistoryExtraDto
import com.zhihuminus.data.zhihu.dto.HistoryHeaderDto
import com.zhihuminus.data.zhihu.dto.HistoryItemDto
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
    }
}

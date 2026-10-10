package com.zhihuminus.feature.collection

import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.dto.ArticleTargetDto
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
import com.zhihuminus.data.zhihu.dto.FeedAuthorDto
import com.zhihuminus.data.zhihu.toFeedDisplayItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CollectionTest {
    @Test
    fun testCollectionSubtitleText() {
        val publicCollection = Collection(id = "1", title = "Public Col", itemCount = 5, isPublic = true)
        assertEquals("5 内容·公开", publicCollection.subtitleText)

        val privateCollection = Collection(id = "2", title = "Private Col", itemCount = 5, isPublic = false)
        assertEquals("5 内容·仅自己可见", privateCollection.subtitleText)

        val zeroItemCollection = Collection(id = "3", title = "Empty Col", itemCount = 0, isPublic = true)
        assertEquals("0 内容·公开", zeroItemCollection.subtitleText)
    }

    @Test
    fun testCollectionItemDtoDeserialization() {
        val json =
            """
            {
                "created": "1727786400",
                "content": {
                    "type": "article",
                    "id": 718304910,
                    "title": "测试文章标题",
                    "url": "https://zhuanlan.zhihu.com/p/718304910",
                    "excerpt": "其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。",
                    "voteup_count": 12,
                    "comment_count": 3,
                    "author": {
                        "id": "abc",
                        "name": "测试作者",
                        "headline": "",
                        "avatar_url": "https://example.com/avatar.jpg"
                    }
                }
            }
            """.trimIndent()

        val itemDto = ZhihuJson.decodeFromString<CollectionItemDto>(json)
        assertIs<ArticleTargetDto>(itemDto.content)
        val article = itemDto.content
        assertEquals("其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。", article.excerpt)

        val displayItem = itemDto.toFeedDisplayItem()
        assertEquals("其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。", displayItem.summary)
    }

    @Test
    fun testArticleTargetExcerptFallback() {
        val author = FeedAuthorDto(id = "1", name = "Test")
        val articleWithExcerpt = ArticleTargetDto(
            id = 1,
            url = "",
            author = author,
            title = "Article 1",
            rawExcerpt = "Explicit excerpt",
            excerptTitle = "Excerpt title",
        )
        assertEquals("Explicit excerpt", articleWithExcerpt.excerpt)

        val articleWithTitleOnly = ArticleTargetDto(
            id = 2,
            url = "",
            author = author,
            title = "Article 2",
            rawExcerpt = "",
            excerptTitle = "Excerpt title fallback",
        )
        assertEquals("Excerpt title fallback", articleWithTitleOnly.excerpt)

        val articleViaSecondaryConstructor = ArticleTargetDto(
            id = 3,
            url = "",
            author = author,
            title = "Article 3",
            excerpt = "Legacy secondary constructor excerpt",
        )
        assertEquals("Legacy secondary constructor excerpt", articleViaSecondaryConstructor.excerpt)
    }
}

package com.zhihuminus.feature.collection

import com.zhihuminus.data.Feed
import com.zhihuminus.data.Person
import com.zhihuminus.data.ZhihuJson
import com.zhihuminus.data.zhihu.dto.CollectionItemDto
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

        val zeroItemCollection = Collection(id = "3", title = "Zero Col", itemCount = 0, isPublic = false)
        assertEquals("0 内容·仅自己可见", zeroItemCollection.subtitleText)
    }

    @Test
    fun testCollectionArticleExcerptTitleDeserialization() {
        val json =
            """
            {
                "created": 1768716969,
                "content": {
                    "id": 1995952847381095524,
                    "type": "article",
                    "title": "“起”",
                    "excerpt_title": "其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。",
                    "url": "https://zhuanlan.zhihu.com/p/1995952847381095524",
                    "author": {
                        "id": "7ae4196ffcea1159d4419154b3817a7f",
                        "name": "尾巴",
                        "url_token": "weiba-wo",
                        "avatar_url": "https://picx.zhimg.com/v2-59a25d417e7045d0eb4d09ca04188973_l.jpg",
                        "url": "https://www.zhihu.com/people/7ae4196ffcea1159d4419154b3817a7f",
                        "user_type": "people",
                        "headline": "芙门"
                    }
                }
            }
            """.trimIndent()

        val itemDto = ZhihuJson.decodeFromString<CollectionItemDto>(json)
        assertIs<Feed.ArticleTarget>(itemDto.content)
        val article = itemDto.content
        assertEquals("其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。", article.excerpt)

        val displayItem = itemDto.toFeedDisplayItem()
        assertEquals("其实在月之四版本过后，最想写的就是哥伦比娅与桑多涅。", displayItem.summary)
    }

    @Test
    fun testArticleTargetExcerptFallback() {
        val person = Person(id = "1", url = "", userType = "people", name = "Test", headline = "", avatarUrl = "")
        val articleWithExcerpt = Feed.ArticleTarget(
            id = 1,
            url = "",
            author = person,
            title = "Article 1",
            rawExcerpt = "Explicit excerpt",
            excerptTitle = "Excerpt title",
        )
        assertEquals("Explicit excerpt", articleWithExcerpt.excerpt)

        val articleWithTitleOnly = Feed.ArticleTarget(
            id = 2,
            url = "",
            author = person,
            title = "Article 2",
            rawExcerpt = "",
            excerptTitle = "Excerpt title fallback",
        )
        assertEquals("Excerpt title fallback", articleWithTitleOnly.excerpt)

        val articleViaSecondaryConstructor = Feed.ArticleTarget(
            id = 3,
            url = "",
            author = person,
            title = "Article 3",
            excerpt = "Legacy secondary constructor excerpt",
        )
        assertEquals("Legacy secondary constructor excerpt", articleViaSecondaryConstructor.excerpt)
    }
}

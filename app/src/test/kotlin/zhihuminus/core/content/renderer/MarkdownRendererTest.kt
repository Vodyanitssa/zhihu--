package com.zhihuminus.core.content.renderer

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.feature.post.Author
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostType
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownRendererTest {
    @Test
    fun rendersInlineNodes() {
        val paragraph = ContentNode.Paragraph(
            listOf(
                InlineNode.Text("看 "),
                InlineNode.Bold(listOf(InlineNode.Text("粗"))),
                InlineNode.Text(" "),
                InlineNode.Italic(listOf(InlineNode.Text("斜"))),
                InlineNode.Text(" "),
                InlineNode.Code("a`b"),
                InlineNode.Text(" "),
                InlineNode.Link("知乎", "https://www.zhihu.com/"),
                InlineNode.Text(" "),
                InlineNode.Emoji("[赞同]"),
                InlineNode.Text(" "),
                InlineNode.Formula("//pic1.zhimg.com/f.svg"),
                InlineNode.Text(" "),
                InlineNode.Footnote(label = "[1]", text = "注", url = "https://a.com"),
            ),
        )

        assertEquals(
            "看 **粗** *斜* ``a`b`` [知乎](https://www.zhihu.com/) [赞同] " +
                "![公式](https://pic1.zhimg.com/f.svg) [[1]](https://a.com)",
            MarkdownRenderer.render(listOf(paragraph)),
        )
    }

    @Test
    fun rendersLineBreakAsHardBreak() {
        val paragraph = ContentNode.Paragraph(
            listOf(InlineNode.Text("一"), InlineNode.LineBreak, InlineNode.Text("二")),
        )
        assertEquals("一  \n二", MarkdownRenderer.render(listOf(paragraph)))
    }

    @Test
    fun escapesLeadingStructuralMarkers() {
        val paragraph = ContentNode.Paragraph(listOf(InlineNode.Text("- 看起来像列表")))
        assertEquals("\\- 看起来像列表", MarkdownRenderer.render(listOf(paragraph)))

        val text = ContentNode.Paragraph(listOf(InlineNode.Text("a\\b`c")))
        assertEquals("a\\\\b\\`c", MarkdownRenderer.render(listOf(text)))
    }

    @Test
    fun rendersHeadingSplitterAndFormula() {
        assertEquals("### 小标题", MarkdownRenderer.render(listOf(ContentNode.Heading(3, "小标题"))))
        assertEquals("---", MarkdownRenderer.render(listOf(ContentNode.Splitter)))
        assertEquals(
            "![公式](https://pic1.zhimg.com/f.svg)",
            MarkdownRenderer.render(listOf(ContentNode.Formula("//pic1.zhimg.com/f.svg"))),
        )
    }

    @Test
    fun rendersLinksAndImages() {
        val card = ContentNode.Link(content = "卡片", url = "https://www.zhihu.com/p/1", isCard = true)
        assertEquals("> [卡片](https://www.zhihu.com/p/1)", MarkdownRenderer.render(listOf(card)))

        val inline = ContentNode.Link(content = "普通", url = "https://example.com/a b", isCard = false)
        assertEquals("[普通](<https://example.com/a b>)", MarkdownRenderer.render(listOf(inline)))

        val image = ContentNode.Image(url = "//pic1.zhimg.com/a.jpg", caption = "图 1]")
        assertEquals("![图 1\\]](https://pic1.zhimg.com/a.jpg)", MarkdownRenderer.render(listOf(image)))
        assertEquals("![](https://pic1.zhimg.com/a.jpg)", MarkdownRenderer.render(listOf(ContentNode.Image("https://pic1.zhimg.com/a.jpg", null))))
    }

    @Test
    fun rendersVideoAndQuote() {
        val video = ContentNode.Video(videoId = 123, caption = "片段", coverUrl = null)
        assertEquals("[片段](https://www.zhihu.com/video/123)", MarkdownRenderer.render(listOf(video)))

        val quote = ContentNode.Quote(
            listOf(InlineNode.Text("第一行"), InlineNode.LineBreak, InlineNode.Text("第二行")),
        )
        assertEquals("> 第一行  \n> 第二行", MarkdownRenderer.render(listOf(quote)))
    }

    @Test
    fun rendersCodeWithAdaptiveFence() {
        val plain = ContentNode.Code(content = "val a = 1", language = "kotlin")
        assertEquals("```kotlin\nval a = 1\n```", MarkdownRenderer.render(listOf(plain)))

        val nested = ContentNode.Code(content = "a\n```\nb", language = "text")
        assertEquals("````text\na\n```\nb\n````", MarkdownRenderer.render(listOf(nested)))
    }

    @Test
    fun rendersListingAndNestedListing() {
        val unordered = ContentNode.Listing(
            items = listOf(
                ContentNode.Paragraph(listOf(InlineNode.Text("一"))),
                ContentNode.Paragraph(listOf(InlineNode.Text("二"))),
            ),
            isSorted = false,
        )
        assertEquals("- 一\n- 二", MarkdownRenderer.render(listOf(unordered)))

        val ordered = ContentNode.Listing(
            items = listOf(
                ContentNode.Paragraph(listOf(InlineNode.Text("一"))),
                ContentNode.Paragraph(listOf(InlineNode.Text("二"))),
            ),
            isSorted = true,
        )
        assertEquals("1. 一\n2. 二", MarkdownRenderer.render(listOf(ordered)))

        val nested = ContentNode.Listing(
            items = listOf(
                ContentNode.Paragraph(listOf(InlineNode.Text("父"))),
                ContentNode.Listing(
                    items = listOf(ContentNode.Paragraph(listOf(InlineNode.Text("子")))),
                    isSorted = false,
                ),
                ContentNode.Paragraph(listOf(InlineNode.Text("续"))),
            ),
            isSorted = false,
        )
        assertEquals("- 父\n  - 子\n- 续", MarkdownRenderer.render(listOf(nested)))
    }

    @Test
    fun rendersTableAsGfm() {
        val header = ContentNode.TableRow(
            listOf(ContentNode.TableCell("列1", true), ContentNode.TableCell("列|2", true)),
        )
        val table = ContentNode.Table(
            rows = listOf(
                header,
                ContentNode.TableRow(
                    listOf(ContentNode.TableCell("a", false), ContentNode.TableCell("b\nc", false)),
                ),
            ),
            header = header,
        )
        assertEquals(
            "| 列1 | 列\\|2 |\n| --- | --- |\n| a | b<br>c |",
            MarkdownRenderer.render(listOf(table)),
        )

        val headerless = ContentNode.Table(
            rows = listOf(
                ContentNode.TableRow(
                    listOf(ContentNode.TableCell("a", false), ContentNode.TableCell("b", false)),
                ),
            ),
        )
        assertEquals("|  |  |\n| --- | --- |\n| a | b |", MarkdownRenderer.render(listOf(headerless)))
    }

    @Test
    fun rendersFullDocumentWithMetaAndSourceLink() {
        val answer = post(PostType.Answer, questionId = 42)
        assertEquals(
            """
            |# 标题
            |
            |> 张三 · 回答 · 12 赞 · 3 评论
            |
            |正文
            |
            |---
            |
            |原文：https://www.zhihu.com/question/42/answer/100
            """.trimMargin(),
            MarkdownRenderer.render(answer),
        )

        assertEquals(
            "# 标题\n\n> 张三 · 文章 · 12 赞 · 3 评论\n\n正文\n\n---\n\n原文：https://zhuanlan.zhihu.com/p/100",
            MarkdownRenderer.render(post(PostType.Article)),
        )
    }

    @Test
    fun buildsSanitizedFileName() {
        assertEquals("a_b_c_张三的回答_100.md", MarkdownRenderer.fileName(post(PostType.Answer, title = "a/b: c")))

        val blank = post(PostType.Pin, title = "", authorName = "")
        assertEquals("无标题_匿名作者的想法_100.md", MarkdownRenderer.fileName(blank))
    }

    private fun post(
        type: PostType,
        title: String = "标题",
        authorName: String = "张三",
        questionId: Long? = null,
    ): Post = Post(
        id = 100,
        type = type,
        title = title,
        author = Author(id = "u1", name = authorName, avatarUrl = "https://pic1.zhimg.com/avatar.jpg"),
        content = listOf(ContentNode.Paragraph(listOf(InlineNode.Text("正文")))),
        voteCount = 12,
        commentCount = 3,
        questionId = questionId,
    )
}

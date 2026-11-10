package com.zhihuminus.core.content

import com.zhihuminus.core.content.renderer.HtmlRenderer
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class AstParserTest {
    @Test
    fun testParseFootnoteFromSampleArticleHtmlFile() {
        val sampleFile = File("samples/article_content.html")
        if (!sampleFile.exists()) return

        val nodes = AstParser.parseContent(sampleFile.readText())
        val footnoteNode = nodes
            .filterIsInstance<ContentNode.Paragraph>()
            .flatMap { it.content }
            .filterIsInstance<InlineNode.Footnote>()
            .firstOrNull()

        requireNotNull(footnoteNode)
        assertEquals("[1]", footnoteNode.label)
        assertEquals("文章也可以添加注释", footnoteNode.text)
        assertEquals("https://x.com/Mluc32705556/status/2089013710132310242", footnoteNode.url)
    }

    @Test
    fun testParseFootnoteFromFragment() {
        val html = """<p><sup data-text="文章也可以添加注释" data-url="https://example.com" data-draft-node="inline" data-draft-type="reference" data-numero="1">[1]</sup></p>"""
        val nodes = AstParser.parseContent(html)
        assertEquals(1, nodes.size)

        val paragraph = assertIs<ContentNode.Paragraph>(nodes.first())
        assertEquals(1, paragraph.content.size)

        val footnote = assertIs<InlineNode.Footnote>(paragraph.content.first())
        assertEquals("[1]", footnote.label)
        assertEquals("文章也可以添加注释", footnote.text)
        assertEquals("https://example.com", footnote.url)
    }

    @Test
    fun testParseFootnoteFallbackNumero() {
        val html = """<p><sup data-text="仅有注释" data-numero="2"></sup></p>"""
        val nodes = AstParser.parseContent(html)
        val paragraph = assertIs<ContentNode.Paragraph>(nodes.first())
        val footnote = assertIs<InlineNode.Footnote>(paragraph.content.first())

        assertEquals("[2]", footnote.label)
        assertEquals("仅有注释", footnote.text)
        assertEquals("", footnote.url)
    }

    @Test
    fun testParseFootnoteWithoutNumeroOrLabel() {
        val html = """<p><sup data-draft-type="reference" data-text="无编号引用"></sup></p>"""
        val nodes = AstParser.parseContent(html)
        val paragraph = assertIs<ContentNode.Paragraph>(nodes.first())
        val footnote = assertIs<InlineNode.Footnote>(paragraph.content.first())

        assertEquals("[*]", footnote.label)
        assertEquals("无编号引用", footnote.text)
        assertEquals("", footnote.url)
    }

    @Test
    fun testParseRegularSuperscriptNotFootnote() {
        val html = """<p>E = mc<sup>2</sup></p>"""
        val nodes = AstParser.parseContent(html)
        val paragraph = assertIs<ContentNode.Paragraph>(nodes.first())
        val hasFootnote = paragraph.content.any { it is InlineNode.Footnote }

        assertEquals(false, hasFootnote)
        val text = paragraph.content.filterIsInstance<InlineNode.Text>().joinToString("") { it.text }
        assertEquals("E = mc2", text)
    }

    @Test
    fun testHtmlRendererFootnoteOutput() {
        val nodeWithUrl = InlineNode.Footnote(
            label = "[1]",
            text = "注释内容",
            url = "https://example.com",
        )
        val htmlWithUrl = HtmlRenderer.render(listOf(ContentNode.Paragraph(listOf(nodeWithUrl))))
        assertTrue(htmlWithUrl.contains("<sup"))
        assertTrue(htmlWithUrl.contains("data-text=\"注释内容\""))
        assertTrue(htmlWithUrl.contains("title=\"注释内容\""))
        assertTrue(htmlWithUrl.contains("data-url=\"https://example.com\""))
        assertTrue(htmlWithUrl.contains("<a href=\"https://example.com\">[1]</a>"))

        val nodeWithoutUrl = InlineNode.Footnote(
            label = "[2]",
            text = "纯注释",
            url = "",
        )
        val htmlWithoutUrl = HtmlRenderer.render(listOf(ContentNode.Paragraph(listOf(nodeWithoutUrl))))
        assertTrue(htmlWithoutUrl.contains("<sup"))
        assertTrue(htmlWithoutUrl.contains("data-text=\"纯注释\""))
        assertTrue(!htmlWithoutUrl.contains("<a href="))
        assertTrue(htmlWithoutUrl.contains("[2]</sup>"))
    }

    @Test
    fun testParseContentDivWithoutPFallback() {
        val html = "<div>直接纯文本想法<br/>第二行</div>"
        val content = AstParser.parseContent(html)
        assertEquals(1, content.size)
        val paragraph = assertIs<ContentNode.Paragraph>(content.first())
        assertTrue(paragraph.content.any { it is InlineNode.LineBreak })
    }

    @Test
    fun testParseContentKeepsTrailingAnchorInline() {
        val html =
            "作为冷酷、客观的评分机器，请根据事实和逻辑对本回答进行评估，按低认知到高认知十分制打分。" +
                "  <a data-hash=\"f3bce7aa022240e25f12aec9a9b70363\" " +
                "href=\"http://www.zhihu.com/people/f3bce7aa022240e25f12aec9a9b70363\" " +
                "class=\"member_mention\">@知乎直答 </a>"
        val nodes = AstParser.parseContent(html)

        assertEquals(1, nodes.size)
        val paragraph = assertIs<ContentNode.Paragraph>(nodes.first())
        val link = assertIs<InlineNode.Link>(paragraph.content.last())
        assertEquals("http://www.zhihu.com/people/f3bce7aa022240e25f12aec9a9b70363", link.url)
        assertEquals("@知乎直答", link.name.trim())
        assertTrue(paragraph.content.first() is InlineNode.Text)
    }

    @Test
    fun testParseContentSplitsInlineRunFromBlockElements() {
        val html = """前文<a href="https://example.com">链接</a><p>段落</p>"""
        val nodes = AstParser.parseContent(html)

        assertEquals(2, nodes.size)
        val inlineParagraph = assertIs<ContentNode.Paragraph>(nodes[0])
        assertTrue(inlineParagraph.content.last() is InlineNode.Link)
        val blockParagraph = assertIs<ContentNode.Paragraph>(nodes[1])
        assertEquals("段落", blockParagraph.content.filterIsInstance<InlineNode.Text>().joinToString("") { it.text })
    }

    @Test
    fun testParseContentKeepsBlockAnchorAsBlock() {
        val html = """<a class="comment_img" href="https://pic1.zhimg.com/v2-abc.jpg">[图片]</a>"""
        val nodes = AstParser.parseContent(html)

        assertEquals(1, nodes.size)
        val image = assertIs<ContentNode.Image>(nodes.first())
        assertEquals("https://pic1.zhimg.com/v2-abc.jpg", image.url)
    }

    @Test
    fun testParseCommentStickerAsImage() {
        val html =
            "<a href=\"https://pic1.zhimg.com/v2-2637ec87f0d63fe043a33096e0fb8f5e.gif?source=1d2f5c51\" " +
                "class=\"comment_sticker\" data-width=\"0\" data-height=\"0\" " +
                "data-sticker-id=\"2004244544011653972\">[灵感正在发酵]</a>"
        val nodes = AstParser.parseContent(html)

        assertEquals(1, nodes.size)
        val image = assertIs<ContentNode.Image>(nodes.first())
        assertEquals("https://pic1.zhimg.com/v2-2637ec87f0d63fe043a33096e0fb8f5e.gif?source=1d2f5c51", image.url)
    }

    @Test
    fun testParseTableWithTheadAndTbody() {
        val html =
            """
            <table>
                <thead>
                    <tr><th>标题1</th><th>标题2</th></tr>
                </thead>
                <tbody>
                    <tr><td>数据1</td><td>数据2</td></tr>
                    <tr><td>数据3</td><td>数据4</td></tr>
                </tbody>
            </table>
            """.trimIndent()
        val nodes = AstParser.parseContent(html)
        assertEquals(1, nodes.size)
        val table = assertIs<ContentNode.Table>(nodes.first())
        assertEquals(3, table.rows.size)
        val header = table.header
        requireNotNull(header)
        assertEquals(2, header.content.size)
        assertEquals("标题1", header.content[0].content)
        assertTrue(header.content[0].isHeader)
        assertEquals("标题2", header.content[1].content)
        assertTrue(header.content[1].isHeader)

        assertEquals("数据1", table.rows[1].content[0].content)
        assertEquals(false, table.rows[1].content[0].isHeader)
    }

    @Test
    fun testParseTableWithoutHeader() {
        val html =
            """
            <table>
                <tbody>
                    <tr><td>A</td><td>B</td></tr>
                    <tr><td>C</td><td>D</td></tr>
                </tbody>
            </table>
            """.trimIndent()
        val nodes = AstParser.parseContent(html)
        assertEquals(1, nodes.size)
        val table = assertIs<ContentNode.Table>(nodes.first())
        assertEquals(2, table.rows.size)
        assertEquals(null, table.header)
    }

    @Test
    fun testHtmlRendererTableOutput() {
        val withHeader = ContentNode.Table(
            rows = listOf(
                ContentNode.TableRow(
                    listOf(
                        ContentNode.TableCell("Col 1", isHeader = true),
                        ContentNode.TableCell("Col 2", isHeader = true),
                    ),
                ),
                ContentNode.TableRow(
                    listOf(
                        ContentNode.TableCell("Val 1", isHeader = false),
                        ContentNode.TableCell("Val 2", isHeader = false),
                    ),
                ),
            ),
            header = ContentNode.TableRow(
                listOf(
                    ContentNode.TableCell("Col 1", isHeader = true),
                    ContentNode.TableCell("Col 2", isHeader = true),
                ),
            ),
        )
        val htmlWithHeader = HtmlRenderer.render(listOf(withHeader))
        assertTrue(htmlWithHeader.contains("<thead><tr><th>Col 1</th><th>Col 2</th></tr></thead>"))
        assertTrue(htmlWithHeader.contains("<tbody><tr><td>Val 1</td><td>Val 2</td></tr></tbody>"))

        val withoutHeader = ContentNode.Table(
            rows = listOf(
                ContentNode.TableRow(
                    listOf(
                        ContentNode.TableCell("A", isHeader = false),
                        ContentNode.TableCell("B", isHeader = false),
                    ),
                ),
            ),
            header = null,
        )
        val htmlWithoutHeader = HtmlRenderer.render(listOf(withoutHeader))
        assertTrue(!htmlWithoutHeader.contains("<thead>"))
        assertTrue(htmlWithoutHeader.contains("<tbody><tr><td>A</td><td>B</td></tr></tbody>"))
    }
}

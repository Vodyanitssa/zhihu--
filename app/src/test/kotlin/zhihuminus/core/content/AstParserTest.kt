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

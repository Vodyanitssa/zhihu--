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
}

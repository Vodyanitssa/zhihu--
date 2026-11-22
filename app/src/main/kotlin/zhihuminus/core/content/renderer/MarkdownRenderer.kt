package com.zhihuminus.core.content.renderer

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.InlineNode
import com.zhihuminus.core.util.formatCount
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.core.util.sanitizeFileNamePart
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.feature.post.contentLink

/**
 * 将 [ContentNode] AST 或整个 [Post] 渲染为 Markdown 文本。
 *
 * 与 [HtmlRenderer] 属于同一套 AST 的不同输出后端：不依赖 Compose，
 * 只做纯字符串拼接，因此既可以在 ViewModel 后台线程调用，也方便单测。
 *
 * 输出约定：
 * - 图片与公式保留远程 URL（知乎图片本身是外部资源，不下载到本地）。
 * - 文本只做最小转义，避免中文正文出现大量反斜杠。
 * - 仅渲染正文 AST；投票、链接卡片、话题、评论不在导出范围内。
 */
object MarkdownRenderer {
    /**
     * 渲染整篇内容：标题 + 引用块元信息 + 正文 + 原文链接。
     */
    fun render(post: Post): String = buildList {
        add("# ${escapeInlineText(post.title.ifBlank { "无标题" })}")
        add(buildMetaBlock(post))
        render(post.content).trim().takeIf { it.isNotEmpty() }?.let(::add)
        add("---")
        add("原文：${post.contentLink()}")
    }.joinToString("\n\n")

    /**
     * 仅渲染正文 AST，块级节点之间以空行分隔。
     */
    fun render(nodes: List<ContentNode>): String = nodes
        .mapNotNull(::renderNode)
        .filter { it.isNotBlank() }
        .joinToString("\n\n")

    /**
     * 建议的导出文件名，形如 `标题_作者的回答_123.md`。
     */
    fun fileName(post: Post): String {
        val safeTitle = sanitizeFileNamePart(post.title).ifBlank { "无标题" }
        val safeAuthorName = sanitizeFileNamePart(post.author.name).ifBlank { "匿名作者" }
        return "${safeTitle}_${safeAuthorName}的${typeLabel(post.type)}_${post.id}.md"
    }

    // ── 文档头 ───────────────────────────────────────────────────────

    private fun buildMetaBlock(post: Post): String {
        val parts = buildList {
            add(post.author.name)
            add(typeLabel(post.type))
            add("${formatCount(post.voteCount)} 赞")
            add("${formatCount(post.commentCount)} 评论")
            formatDateTime(post.createdAt).takeIf { it.isNotEmpty() }?.let(::add)
            post.ipInfo
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { add("IP 属地：$it") }
        }
        return "> " + parts.joinToString(" · ")
    }

    private fun typeLabel(type: PostType): String = when (type) {
        PostType.Answer -> "回答"
        PostType.Article -> "文章"
        PostType.Pin -> "想法"
    }

    // ── 块级节点 ─────────────────────────────────────────────────────

    private fun renderNode(node: ContentNode): String? = when (node) {
        is ContentNode.Paragraph -> escapeLeadingMarkers(renderInline(node.content)).takeIf { it.isNotBlank() }
        is ContentNode.Heading -> "${"#".repeat(node.level.coerceIn(1, 6))} ${escapeInlineText(node.content)}"
        is ContentNode.Splitter -> "---"
        is ContentNode.Link -> renderLink(node)
        is ContentNode.Code -> renderCode(node)
        is ContentNode.Formula -> renderImage(url = node.url, caption = "公式")
        is ContentNode.Quote -> renderQuote(node)
        is ContentNode.Image -> renderImage(url = node.url, caption = node.caption)
        is ContentNode.Video -> renderVideo(node)
        is ContentNode.Listing -> renderListing(node, depth = 0)
        is ContentNode.Table -> renderTable(node)
    }

    private fun renderLink(node: ContentNode.Link): String? {
        val text = escapeLinkLabel(node.content)
        if (text.isBlank()) return null
        val link = "[$text](${formatUrl(node.url)})"
        return if (node.isCard) "> $link" else link
    }

    private fun renderCode(node: ContentNode.Code): String {
        val content = node.content.trimEnd()
        val fence = "`".repeat(fenceLength(content, minimum = 3))
        val language = node.language.trim().takeIf { it.isNotEmpty() && '`' !in it } ?: ""
        return buildString {
            append(fence).append(language).append('\n')
            append(content)
            if (content.isNotEmpty()) append('\n')
            append(fence)
        }
    }

    private fun renderQuote(node: ContentNode.Quote): String? {
        val content = renderInline(node.content).trimEnd()
        if (content.isBlank()) return null
        return content.lines().joinToString("\n") { line ->
            if (line.isEmpty()) ">" else "> $line"
        }
    }

    private fun renderImage(url: String, caption: String?): String? {
        val normalized = normalizeUrl(url)
        if (normalized.isBlank()) return null
        val alt = caption?.let(::escapeLinkLabel).orEmpty()
        return "![$alt](${formatUrl(normalized)})"
    }

    private fun renderVideo(node: ContentNode.Video): String {
        val label = node.caption?.takeIf { it.isNotBlank() }?.let(::escapeLinkLabel) ?: "视频"
        return "[$label](https://www.zhihu.com/video/${node.videoId})"
    }

    private fun renderListing(node: ContentNode.Listing, depth: Int): String {
        val indent = "  ".repeat(depth)
        val lines = mutableListOf<String>()
        var itemNumber = 0
        node.items.forEach { item ->
            if (item is ContentNode.Listing) {
                lines += renderListing(item, depth + 1)
                return@forEach
            }
            itemNumber++
            val marker = if (node.isSorted) "$itemNumber." else "-"
            val continuationIndent = indent + " ".repeat(marker.length + 1)
            val content = when (item) {
                is ContentNode.Paragraph -> renderInline(item.content)
                else -> renderNode(item).orEmpty()
            }
            lines += indent + marker + " " + content.replace("\n", "\n$continuationIndent")
        }
        return lines.filter { it.isNotEmpty() }.joinToString("\n")
    }

    private fun renderTable(node: ContentNode.Table): String? {
        val header = node.header
        val bodyRows = node.rows.filter { header == null || it != header }
        val columnCount = (listOfNotNull(header) + bodyRows).maxOfOrNull { it.content.size } ?: 0
        if (columnCount == 0) return null

        fun renderRow(cells: List<ContentNode.TableCell>): String =
            (0 until columnCount).joinToString(" | ", prefix = "| ", postfix = " |") { index ->
                escapeTableCell(cells.getOrNull(index)?.content.orEmpty())
            }

        return buildList {
            add(renderRow(header?.content.orEmpty()))
            add((0 until columnCount).joinToString(" | ", prefix = "| ", postfix = " |") { "---" })
            bodyRows.forEach { add(renderRow(it.content)) }
        }.joinToString("\n")
    }

    // ── 行内节点 ─────────────────────────────────────────────────────

    private fun renderInline(nodes: List<InlineNode>): String = buildString {
        nodes.forEach { append(renderInlineNode(it)) }
    }

    private fun renderInlineNode(node: InlineNode): String = when (node) {
        is InlineNode.Text -> escapeInlineText(node.text)
        is InlineNode.Bold -> renderWrapped(node.children, "**")
        is InlineNode.Italic -> renderWrapped(node.children, "*")
        is InlineNode.Emoji -> node.name
        is InlineNode.Link -> "[${escapeLinkLabel(node.name)}](${formatUrl(node.url)})"
        is InlineNode.Code -> renderInlineCode(node.text)
        is InlineNode.Formula -> renderImage(url = node.url, caption = "公式").orEmpty()
        is InlineNode.Footnote -> renderFootnote(node)
        InlineNode.LineBreak -> "  \n"
    }

    private fun renderWrapped(children: List<InlineNode>, marker: String): String {
        val content = renderInline(children)
        return if (content.isBlank()) "" else "$marker$content$marker"
    }

    private fun renderInlineCode(text: String): String {
        val fence = "`".repeat(fenceLength(text, minimum = 1))
        val padding = if (text.startsWith("`") || text.endsWith("`")) " " else ""
        return "$fence$padding$text$padding$fence"
    }

    private fun renderFootnote(node: InlineNode.Footnote): String {
        val label = node.label.ifBlank { "[*]" }
        // 知乎脚注 label 自带方括号（如 `[1]`），直接作为链接文本即可避免源码里出现 `\[1\]`
        val linkLabel = if (label.startsWith("[") && label.endsWith("]")) label else escapeLinkLabel(label)
        if (node.url.isNotBlank()) {
            return "[" + linkLabel + "](" + formatUrl(node.url) + ")"
        }
        if (node.text.isNotBlank()) return "$label（${escapeInlineText(node.text)}）"
        return label
    }

    // ── 转义与格式化 ─────────────────────────────────────────────────

    private fun escapeInlineText(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '`' -> append("\\`")
                else -> append(char)
            }
        }
    }

    private fun escapeLinkLabel(text: String): String = text
        .replace("\\", "\\\\")
        .replace("[", "\\[")
        .replace("]", "\\]")

    private val LEADING_MARKER = Regex("^(#{1,6}\\s|>|[-+*]\\s|\\d+\\.\\s)")

    /**
     * 段落行首若恰好是标题/列表/引用的语法前缀，加反斜杠避免被解析成结构。
     */
    private fun escapeLeadingMarkers(text: String): String =
        text.lines().joinToString("\n") { line ->
            if (LEADING_MARKER.containsMatchIn(line)) "\\$line" else line
        }

    private fun escapeTableCell(text: String): String = text
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\r\n", "\n")
        .replace("\n", "<br>")
        .trim()

    private fun formatUrl(url: String): String {
        val normalized = normalizeUrl(url)
        return if (normalized.any { it.isWhitespace() || it == '(' || it == ')' }) {
            "<" + normalized.replace("<", "%3C").replace(">", "%3E") + ">"
        } else {
            normalized
        }
    }

    private fun fenceLength(content: String, minimum: Int): Int {
        val longest = Regex("`+").findAll(content).maxOfOrNull { it.value.length } ?: 0
        return maxOf(minimum, longest + 1)
    }

    private fun normalizeUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

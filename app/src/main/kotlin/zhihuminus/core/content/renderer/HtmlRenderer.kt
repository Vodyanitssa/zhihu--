package com.zhihuminus.core.content.renderer

import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.content.EmojiManager
import com.zhihuminus.core.content.InlineNode

private const val EMOJI_ASSET_PREFIX = "file:///android_asset/emojis/images/"

/**
 * A standalone renderer that converts a list of [ContentNode] into an HTML string.
 *
 * This renderer is completely independent of the Compose rendering layer and the
 * article-export utilities. It only depends on the AST types defined in
 * [com.zhihuminus.core.content] and produces semantically-correct HTML elements
 * for every node type, preserving document order and structure.
 *
 * No surrounding `<html>`, `<head>`, or `<body>` tags are emitted — the returned
 * string is a fragment suitable for embedding inside an existing document.
 */
object HtmlRenderer {
    /**
     * Renders a list of content nodes into an HTML fragment string.
     *
     * Each node is converted to its corresponding HTML element and concatenated
     * in order. The result is a flat HTML fragment with no wrapping document tags.
     */
    fun render(nodes: List<ContentNode>): String = buildString {
        nodes.forEach { node ->
            append(renderNode(node))
        }
    }

    // ── Block-level nodes ──────────────────────────────────────────────

    private fun renderNode(node: ContentNode): String = when (node) {
        is ContentNode.Paragraph ->
            "<p>${renderInline(node.content)}</p>"

        is ContentNode.Heading ->
            "<h${node.level}>${escapeHtml(node.content)}</h${node.level}>"

        is ContentNode.Splitter ->
            "<hr />"

        is ContentNode.Link ->
            renderLink(node)

        is ContentNode.Code ->
            buildString {
                append("<pre>")
                append("<code")
                if (node.language.isNotBlank()) {
                    append(" class=\"language-${escapeHtml(node.language)}\"")
                }
                append(">")
                append(escapeHtml(node.content))
                append("</code>")
                append("</pre>")
            }

        is ContentNode.Formula ->
            "<img class=\"formula\" src=\"${escapeHtml(normalizeImageUrl(node.url))}\" alt=\"公式\" />"

        is ContentNode.Quote ->
            "<blockquote>${renderInline(node.content)}</blockquote>"

        is ContentNode.Image ->
            renderImage(node)

        is ContentNode.Video ->
            renderVideo(node)

        is ContentNode.Listing ->
            renderListing(node)

        is ContentNode.Table ->
            renderTable(node)
    }

    private fun renderLink(node: ContentNode.Link): String = buildString {
        val href = escapeHtml(node.url)
        val text = escapeHtml(node.content)
        if (node.isCard) {
            append("<div class=\"link-card\"><a href=\"$href\">$text</a></div>")
        } else {
            append("<a href=\"$href\">$text</a>")
        }
    }

    private fun renderImage(node: ContentNode.Image): String = buildString {
        val src = escapeHtml(normalizeImageUrl(node.url))
        if (node.caption != null) {
            append("<figure class=\"img\"><img src=\"$src\" alt=\"${escapeHtml(node.caption)}\" /><figcaption>${escapeHtml(node.caption)}</figcaption></figure>")
        } else {
            append("<img src=\"$src\" alt=\"\" />")
        }
    }

    private fun renderVideo(node: ContentNode.Video): String = buildString {
        append("<figure class=\"video\">")
        append("<a href=\"/video/${node.videoId}\">")
        node.coverUrl?.let { coverUrl ->
            append("<img src=\"${escapeHtml(coverUrl)}\"")
            append(" alt=\"")
            node.caption?.let { append(escapeHtml(it)) }
            append("\" />")
        } ?: run {
            append("<span class=\"video-placeholder\">播放")
            append("<span aria-hidden=\"true\">▶</span>")
            append("</span>")
        }
        append("</a>")
        node.caption?.let { caption ->
            append("<figcaption>${escapeHtml(caption)}</figcaption>")
        }
        append("</figure>")
    }

    private fun renderListing(node: ContentNode.Listing): String = buildString {
        val tag = if (node.isSorted) "ol" else "ul"
        append("<$tag>")
        node.items.forEach { item ->
            append("<li>${escapeHtml(item)}</li>")
        }
        append("</$tag>")
    }

    private fun renderTable(node: ContentNode.Table): String = buildString {
        append("<table>")

        // Render the header row in <thead>.
        append("<thead><tr>")
        node.header.content.forEach { cell ->
            append("<th>${escapeHtml(cell.content)}</th>")
        }
        append("</tr></thead>")

        // Render remaining rows in <tbody>, skipping the header if it is
        // also present in the rows list (the parser always includes it).
        append("<tbody>")
        node.rows.forEach { row ->
            if (row == node.header) return@forEach
            append("<tr>")
            row.content.forEach { cell ->
                if (cell.isHeader) {
                    append("<th>${escapeHtml(cell.content)}</th>")
                } else {
                    append("<td>${escapeHtml(cell.content)}</td>")
                }
            }
            append("</tr>")
        }
        append("</tbody>")
        append("</table>")
    }

    // ── Inline nodes ─────────────────────────────────────────────────

    private fun renderInline(nodes: List<InlineNode>): String = buildString {
        nodes.forEach { node ->
            append(renderInlineNode(node))
        }
    }

    private fun renderInlineNode(node: InlineNode): String = when (node) {
        is InlineNode.Text ->
            escapeHtml(node.text)

        is InlineNode.Bold ->
            "<strong>${renderInline(node.children)}</strong>"

        is InlineNode.Italic ->
            "<em>${renderInline(node.children)}</em>"

        is InlineNode.Emoji -> {
            val fileName = EmojiManager.mapping[node.name]
            if (fileName != null) {
                "<img class=\"emoji\" src=\"$EMOJI_ASSET_PREFIX${escapeHtml(fileName)}\" alt=\"${escapeHtml(node.name)}\" />"
            } else {
                "<img class=\"emoji\" data-name=\"${escapeHtml(node.name)}\">"
            }
        }

        is InlineNode.Code ->
            "<code style=\"font-family:monospace;background:rgba(0,0,0,0.08);padding:0 4px;border-radius:3px\">${escapeHtml(node.text)}</code>"

        is InlineNode.Formula ->
            "<img class=\"formula\" src=\"${escapeHtml(normalizeImageUrl(node.url))}\" alt=\"公式\" />"

        is InlineNode.Link ->
            "<a href=\"${escapeHtml(node.url)}\">${escapeHtml(node.name)}</a>"

        is InlineNode.LineBreak -> {
            "<br>"
        }
    }

    // ── HTML escaping ─────────────────────────────────────────────────

    private fun escapeHtml(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private fun normalizeImageUrl(url: String): String = when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

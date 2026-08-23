package com.zhihuminus.core.content

import com.zhihuminus.navigation.Video
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.util.extractImageUrl
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

object AstParser {
    fun parseContent(text: String): List<ContentNode> {
        val document = Jsoup.parseBodyFragment(text)
        return document.body().childNodes().flatMap { node ->
            when (node) {
                is TextNode -> {
                    if (node.isBlank) {
                        emptyList()
                    } else {
                        listOf(
                            ContentNode.Paragraph(
                                content = parseInline(node),
                            ),
                        )
                    }
                }

                is Element -> parseBlock(node)
                else -> emptyList()
            }
        }
    }

    fun parseInline(node: Node): List<InlineNode> = when (node) {
        is TextNode -> parseText(node.text())

        is Element -> when (node.tagName()) {
            "b" -> listOf(
                InlineNode.Bold(
                    children = node.childNodes().flatMap(::parseInline),
                ),
            )

            "i" -> listOf(
                InlineNode.Italic(
                    children = node.childNodes().flatMap(::parseInline),
                ),
            )

            "br" -> listOf(InlineNode.LineBreak)

            "code" -> listOf(
                InlineNode.Code(
                    text = node.text(),
                ),
            )

            "a" -> {
                val url = node.attr("href").ifBlank { null }
                if (url != null) {
                    listOf(
                        InlineNode.Link(
                            name = node.text(),
                            url = url,
                        ),
                    )
                } else {
                    parseText(node.text())
                }
            }

            else -> parseText(node.text())
        }

        else -> emptyList()
    }

    private fun parseText(text: String): List<InlineNode> {
        val result = mutableListOf<InlineNode>()
        val textBuffer = StringBuilder()
        var i = 0
        while (i < text.length) {
            if (text[i] == '[') {
                val closeIdx = text.indexOf(']', i + 1)
                if (closeIdx != -1) {
                    val key = text.substring(i, closeIdx + 1)
                    if (EmojiManager.mapping.containsKey(key)) {
                        if (textBuffer.isNotEmpty()) {
                            result.add(InlineNode.Text(textBuffer.toString()))
                            textBuffer.clear()
                        }
                        result.add(
                            InlineNode.Emoji(
                                name = key,
                            ),
                        )
                        i = closeIdx + 1
                        continue
                    }
                }
            }
            textBuffer.append(text[i])
            i++
        }
        if (textBuffer.isNotEmpty()) {
            result.add(InlineNode.Text(textBuffer.toString()))
        }
        return result
    }

    private fun parseBlock(element: Element): List<ContentNode> = when (element.tagName()) {
        "h1", "h2", "h3", "h4", "h5", "h6" -> {
            listOf(
                ContentNode.Heading(
                    level = element.tagName().substring(1).toInt(),
                    content = element.text(),
                ),
            )
        }

        "p" -> {
            val children = element.children()
            if (children.size == 1 && children.first()!!.tagName() == "img") {
                listOfNotNull(children.first()?.let(::parseImage))
            } else {
                listOf(
                    ContentNode.Paragraph(
                        content = element.childNodes().flatMap { node ->
                            if (element.className() == "ztext-empty-paragraph") {
                                emptyList()
                            } else {
                                parseInline(node)
                            }
                        },
                    ),
                )
            }
        }

        "blockquote" -> {
            listOf(ContentNode.Quote(content = element.childNodes().flatMap { node -> parseInline(node) }))
        }

        "pre" -> {
            listOfNotNull(parseCode(element))
        }

        "div" -> {
            buildList {
                element
                    .ownText()
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let {
                        add(
                            ContentNode.Heading(
                                level = 1,
                                content = it,
                            ),
                        )
                    }

                element.children().forEach {
                    if (it.tagName() == "p" || it.tagName() == "pre") {
                        addAll(parseBlock(it))
                    }
                }
            }
        }

        "ol", "ul" -> {
            listOf(
                ContentNode.Listing(
                    items = element
                        .children()
                        .filter { it.tagName() == "li" }
                        .map { it.text() },
                    isSorted = element.tagName() == "ol",
                ),
            )
        }

        "figure" -> listOfNotNull(parseFigure(element))

        "img" -> listOfNotNull(parseImage(element))

        "a" -> {
            listOfNotNull(
                if (element.hasClass("video-box")) {
                    parseVideo(element)
                } else if (element.hasClass("comment_img")) {
                    parseImage(element)
                } else {
                    parseLink(element)
                },
            )
        }

        "table" -> listOfNotNull(parseTable(element))

        "hr" -> listOf(ContentNode.Splitter)

        else -> emptyList()
    }

    private fun parseImage(element: Element): ContentNode.Image? {
        val url = element
            .attr("data-original")
            .ifBlank { element.attr("src") }
            .ifBlank { element.attr("href") }
            .takeIf { it.isNotBlank() }
            ?: return null
        // 知乎使用 a 渲染评论图片，因此最终可能需要用 href
        return ContentNode.Image(
            url = url,
            caption = null,
            // 由于知乎用 img 渲染 latex，直接渲染 img 则不显示 caption
        )
    }

    private fun parseFigure(element: Element): ContentNode.Image? {
        val image = element.selectFirst("img")
            ?: return null

        return ContentNode.Image(
            url = element
                .attr("data-original")
                .ifBlank { image.attr("src") }
                .takeIf { it.isNotBlank() }
                ?: return null,
            caption = element
                .children()
                .firstOrNull { it.tagName() == "figcaption" }
                ?.text()
                ?.ifBlank { null },
        )
    }

    private fun parseVideo(element: Element): ContentNode.Video? {
        val href = element.attr("href")
        val videoId = href.takeIf { it.isNotBlank() }?.let { destination ->
            val resolved = resolveContent(destination)
            if (resolved is Video) resolved.id else null
        } ?: element.attr("data-lens-id").toLongOrNull() ?: return null
        val coverUrl = element.selectFirst("img")?.let { image ->
            extractImageUrl(image::attr)
        } ?: element.attr("data-poster").ifBlank { null }
        return ContentNode.Video(
            videoId = videoId,
            caption = element.attr("data-name").ifBlank { null },
            coverUrl = coverUrl,
        )
    }

    private fun parseLink(element: Element): ContentNode.Link? {
        return ContentNode.Link(
            content = element.text().ifBlank {
                return null
            },
            url = element.attr("href").ifBlank {
                return null
            },
            isCard = element.attr("data-draft-type") == "link-card",
        )
    }

    private fun parseCode(element: Element): ContentNode.Code? {
        val code = element.selectFirst("code") ?: element
        val language = code
            .classNames()
            .firstOrNull { it.startsWith("language-") }
            ?.removePrefix("language-")
            ?.ifBlank { null }
            ?: "text"
        return ContentNode.Code(
            // wholeText() 比 text() 更适合代码，因为需要保留换行和缩进
            content = code.wholeText().trimEnd(),
            language = language,
        )
    }

    private fun parseTable(element: Element): ContentNode.Table? {
        val rowElements = element.select("tbody > tr")
        if (rowElements.isEmpty()) {
            return null
        }
        val rows = rowElements
            .map { row ->
                ContentNode.TableRow(
                    content = row
                        .children()
                        .filter {
                            it.tagName() == "th" || it.tagName() == "td"
                        }.map { cell ->
                            ContentNode.TableCell(
                                content = cell.text(),
                                isHeader = cell.tagName() == "th",
                            )
                        },
                )
            }.filter {
                it.content.isNotEmpty()
            }
        if (rows.isEmpty()) {
            return null
        }
        val header = rows.firstOrNull { row ->
            row.content.any(ContentNode.TableCell::isHeader)
        } ?: rows.first()
        return ContentNode.Table(
            rows = rows,
            header = header,
        )
    }
}

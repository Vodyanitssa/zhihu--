package com.zhihuminus.core.content

/*
 * 块内容，可为多种类型
 */
sealed interface ContentNode {
    data class Paragraph(
        val content: List<InlineNode>,
    ) : ContentNode

    data class Heading(
        val level: Int,
        val content: String,
    ) : ContentNode

    data object Splitter : ContentNode

    data class Link(
        val content: String,
        val url: String,
        val isCard: Boolean,
    ) : ContentNode

    data class Code(
        val content: String,
        val language: String,
    ) : ContentNode

    // 知乎以 eeimg="1" 的 img 渲染 LaTeX 公式，独立成段时为块级公式
    data class Formula(
        val url: String,
    ) : ContentNode

    data class Quote(
        val content: List<InlineNode>,
    ) : ContentNode

    data class Image(
        val url: String,
        val caption: String?,
    ) : ContentNode

    data class Video(
        val videoId: Long,
        val caption: String?,
        val coverUrl: String?,
    ) : ContentNode

    // avoid naming collision with List
    data class Listing(
        val items: List<ContentNode>,
        val isSorted: Boolean,
    ) : ContentNode

    data class Table(
        val rows: List<TableRow>,
        val header: TableRow? = null,
    ) : ContentNode

    data class TableRow(
        val content: List<TableCell>,
    )

    data class TableCell(
        val content: String,
        val isHeader: Boolean,
    )
}

sealed interface InlineNode {
    data class Text(
        val text: String,
    ) : InlineNode

    data class Bold(
        val children: List<InlineNode>,
    ) : InlineNode

    data class Italic(
        val children: List<InlineNode>,
    ) : InlineNode

    data class Emoji(
        val name: String,
    ) : InlineNode

    data class Link(
        val name: String,
        val url: String,
    ) : InlineNode

    data class Code(
        val text: String,
    ) : InlineNode

    // 行内公式（eeimg="1" 的 img 元素）
    data class Formula(
        val url: String,
    ) : InlineNode

    // 脚注/参考资料（例如文章中带 data-text 和 data-url 的 sup 元素）
    data class Footnote(
        val label: String,
        val text: String,
        val url: String,
    ) : InlineNode

    data object LineBreak : InlineNode
}

/**
 * 递归提取 AST 节点中的所有图片 URL（过滤空白和 data URI）。
 */
fun List<ContentNode>.extractAllImageUrls(): List<String> = buildList<String> {
    fun collectFrom(node: ContentNode) {
        when (node) {
            is ContentNode.Image -> {
                if (node.url.isNotBlank() && !node.url.startsWith("data:")) {
                    add(node.url)
                }
            }
            is ContentNode.Listing -> {
                node.items.forEach { collectFrom(it) }
            }
            else -> Unit
        }
    }
    this@extractAllImageUrls.forEach { collectFrom(it) }
}

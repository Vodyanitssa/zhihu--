package com.github.zly2006.zhihu.renderer

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

    data class Link(
        val content: String,
    ) : ContentNode

    data class Code(
        val content: String,
        val language: String,
    ) : ContentNode

    data class Quote(
        val content: List<InlineNode>,
    ) : ContentNode

    data class Image(
        val url: String,
        val caption: String?,
    ) : ContentNode

    data class Video(
        val url: String,
        val caption: String?,
    ) : ContentNode

    // avoid naming collision with List
    data class Listing(
        val items: List<String>,
        val isSorted: Boolean,
    ) : ContentNode

    data class Table(
        val rows: List<TableRow>,
        val header: TableRow,
    ) : ContentNode

    data class TableRow(
        val content: List<TableCell>,
    )

    data class TableCell(
        val content: String,
        val isHeader: Boolean,
    )
}

/*
 * 行内内容，文字表情混合文本
 */
sealed interface InlineNode {
    data class Text(
        val text: String,
    ) : InlineNode

    data class Emoji(
        val name: String,
    ) : InlineNode
}

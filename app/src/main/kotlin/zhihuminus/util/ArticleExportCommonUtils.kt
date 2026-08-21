/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.zhihuminus.util

import com.zhihuminus.core.content.AstParser
import com.zhihuminus.core.content.ContentNode
import com.zhihuminus.core.renderer.HtmlRenderer
import com.zhihuminus.data.DataHolder
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ArticleExportComment(
    val authorName: String,
    val nodes: List<ContentNode>,
    val createdTimeText: String,
)

fun prepareArticleExportComment(
    authorName: String,
    content: String,
    createdTimeText: String,
): ArticleExportComment = ArticleExportComment(
    authorName = authorName,
    nodes = AstParser.parseContent(content),
    createdTimeText = createdTimeText,
)

fun buildArticleExportCommentsHtml(
    comments: List<ArticleExportComment>,
    requestedCount: Int? = null,
): String {
    if (comments.isEmpty()) return ""
    val titleSuffix = requestedCount
        ?.takeIf { it > 0 }
        ?.let { " (前 ${minOf(it, comments.size)} 条)" }
        .orEmpty()

    return buildString {
        append("<div class='comments-title'>热门评论$titleSuffix</div>")
        comments.forEach { comment ->
            val contentHtml = HtmlRenderer.render(comment.nodes)
            append(
                """
                <div class="comment">
                    <div class="comment-author">${escapeArticleExportHtml(comment.authorName)}</div>
                    <div class="comment-content">$contentHtml</div>
                    <div class="comment-time">${escapeArticleExportHtml(comment.createdTimeText)}</div>
                </div>
                """.trimIndent(),
            )
        }
    }
}

fun buildArticleExportFileName(
    content: DataHolder.Content,
    extension: String,
): String {
    val timestamp = formatArticleExportFileTimestamp()
    val (title, authorName, typeLabel, typeKey, articleId) = when (content) {
        is DataHolder.Answer -> ExportFileMeta(
            title = content.question.title,
            authorName = content.author.name,
            typeLabel = "回答",
            typeKey = "answer",
            articleId = content.id,
        )

        is DataHolder.Article -> ExportFileMeta(
            title = content.title,
            authorName = content.author.name,
            typeLabel = "文章",
            typeKey = "article",
            articleId = content.id,
        )

        else -> throw IllegalArgumentException("Unsupported export content type: ${content::class.simpleName}")
    }
    val safeTitle = sanitizeArticleExportFileNamePart(title).ifBlank { "无标题" }
    val safeAuthorName = sanitizeArticleExportFileNamePart(authorName)
        .ifBlank { "匿名作者" }
    val normalizedExtension = extension.trimStart('.')

    return "zhihu++_${safeTitle}_${safeAuthorName}的${typeLabel}_${typeKey}_${articleId}_$timestamp.$normalizedExtension"
}

fun sanitizeArticleExportFileNamePart(text: String): String = text
    .trim()
    .replace(Regex("\\s+"), "_")
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .replace(Regex("_+"), "_")
    .trim('_')

fun escapeArticleExportHtml(text: String): String = buildString(text.length) {
    text.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#x27;")
            else -> append(char)
        }
    }
}

@OptIn(ExperimentalTime::class)
private fun formatArticleExportFileTimestamp(timestampMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    val dateTime = Instant
        .fromEpochMilliseconds(timestampMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append((dateTime.month.ordinal + 1).twoDigitString())
        append(dateTime.day.twoDigitString())
        append('_')
        append(dateTime.hour.twoDigitString())
        append(dateTime.minute.twoDigitString())
        append(dateTime.second.twoDigitString())
    }
}

private data class ExportFileMeta(
    val title: String,
    val authorName: String,
    val typeLabel: String,
    val typeKey: String,
    val articleId: Long,
)

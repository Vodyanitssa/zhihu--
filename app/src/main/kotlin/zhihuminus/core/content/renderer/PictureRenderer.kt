package com.zhihuminus.core.content.renderer

import android.content.Context
import android.graphics.Bitmap
import com.zhihuminus.core.platform.PictureExportWebView
import com.zhihuminus.core.util.formatDateTime
import com.zhihuminus.feature.post.Post
import com.zhihuminus.feature.post.PostType

/**
 * 将 [Post] 渲染为图片。
 *
 * 纯渲染工具对象，只负责 Post → Bitmap，不涉及文件保存。
 * 每次调用创建临时 WebView，渲染完成后销毁。
 */
object PictureRenderer {
    /**
     * 将 Post 渲染为 Bitmap。
     *
     * @return 渲染结果，包含 Bitmap 和建议的文件名。调用方负责回收 Bitmap。
     */
    suspend fun render(context: Context, post: Post): RenderResult {
        val htmlContent = buildHtmlContent(post)
        val webView = PictureExportWebView(context)
        var prepared: com.zhihuminus.core.platform.PreparedWebView? = null
        try {
            prepared = webView.prepare(htmlContent)
            val bitmap = webView.captureBitmap(prepared)
            return RenderResult(bitmap, buildFileName(post))
        } finally {
            prepared?.let { webView.destroy(it) }
        }
    }

    data class RenderResult(
        val bitmap: Bitmap,
        val displayName: String,
    )

    private fun buildHtmlContent(post: Post): String {
        val bodyHtml = HtmlRenderer.render(post.content)
        val typeLabel = when (post.type) {
            PostType.Answer -> "回答"
            PostType.Article -> "文章"
            PostType.Pin -> "想法"
        }
        val voteText = "${post.voteCount} 赞"
        val commentText = "${post.commentCount} 评论"
        val timeText = formatDateTime(post.createdAt)

        return """
            |<!DOCTYPE html>
            |<html>
            |<head>
            |<meta charset="UTF-8"/>
            |<meta name="viewport" content="width=device-width,initial-scale=1"/>
            |<style>
            |body{max-width:680px;margin:0 auto;padding:24px 16px;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"Helvetica Neue",Arial,sans-serif;color:#1a1a1a;line-height:1.8;font-size:16px}
            |.post-header{margin-bottom:24px;border-bottom:1px solid #eee;padding-bottom:16px}
            |.post-title{font-size:24px;font-weight:700;margin:0 0 12px;line-height:1.4}
            |.post-meta{font-size:14px;color:#8590a6}
            |.post-meta strong{color:#1a1a1a}
            |.post-type{display:inline-block;background:#f0f7ff;color:#175199;padding:2px 8px;border-radius:4px;font-size:12px;margin-left:8px}
            |img{max-width:100%;height:auto}
            |.emoji{display:inline-block;width:1.3em;height:1.3em;vertical-align:text-bottom}
            |pre{background:#f6f8fa;padding:16px;border-radius:6px;overflow-x:auto}
            |code{font-family:"SFMono-Regular",Consolas,"Liberation Mono",Menlo,monospace;font-size:14px}
            |blockquote{border-left:3px solid #ddd;margin:0;padding:8px 16px;color:#666}
            |table{border-collapse:collapse;width:100%}
            |th,td{border:1px solid #ddd;padding:8px;text-align:left}
            |th{background:#f6f8fa}
            |figure{margin:16px 0;text-align:center}
            |figcaption{font-size:13px;color:#999;margin-top:4px}
            |a{color:#175199;text-decoration:none}
            |a:hover{text-decoration:underline}
            |</style>
            |</head>
            |<body>
            |<div class="post-header">
            |<h1 class="post-title">${escapeHtml(post.title)}</h1>
            |<div class="post-meta">
            |<strong>${escapeHtml(post.author.name)}</strong>
            |<span class="post-type">$typeLabel</span>
            | · $voteText · $commentText
            |${if (timeText.isNotEmpty()) " · $timeText" else ""}
            |</div>
            |</div>
            |$bodyHtml
            |</body>
            |</html>
            """.trimMargin()
    }

    private fun buildFileName(post: Post): String {
        val typeLabel = when (post.type) {
            PostType.Answer -> "回答"
            PostType.Article -> "文章"
            PostType.Pin -> "想法"
        }
        val safeTitle = sanitizeFileNamePart(post.title).ifBlank { "无标题" }
        val safeAuthorName = sanitizeFileNamePart(post.author.name).ifBlank { "匿名作者" }
        return "${safeTitle}_${safeAuthorName}的${typeLabel}_${post.id}.png"
    }

    private fun sanitizeFileNamePart(text: String): String = text
        .trim()
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')

    private fun escapeHtml(text: String): String = buildString(text.length) {
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
}

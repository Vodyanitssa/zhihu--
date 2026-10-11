package com.zhihuminus.navigation.router

/**
 * URL 归一化工具：补全知乎 Web 内容中常见的协议相对 URL（`//...`）与站点根路径（`/...`）。
 */
object UrlNormalizer {
    fun normalize(url: String): String {
        val trimmed = url.trim()
        return when {
            trimmed.startsWith("//") -> "https:$trimmed"
            trimmed.startsWith("/") -> "https://www.zhihu.com$trimmed"
            else -> trimmed
        }
    }
}

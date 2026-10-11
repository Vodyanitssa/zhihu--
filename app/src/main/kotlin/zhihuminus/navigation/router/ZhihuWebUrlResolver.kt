package com.zhihuminus.navigation.router

import com.zhihuminus.core.util.Log
import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Column
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.Topic
import com.zhihuminus.navigation.Video
import io.ktor.http.Url

/**
 * 知乎 HTTP / HTTPS Web 链接解析器。
 * 负责把知乎网站 URL（`www.zhihu.com`、`zhuanlan.zhihu.com` 等）解析为对应屏幕的 [NavDestination]。
 */
object ZhihuWebUrlResolver {
    private const val TAG = "ZhihuWebUrlResolver"

    fun resolve(url: String): NavDestination? {
        val normalized = UrlNormalizer.normalize(url)
        return runCatching { resolve(Url(normalized)) }.getOrNull()
    }

    fun resolve(url: Url): NavDestination? {
        if (url.protocol.name != "http" && url.protocol.name != "https") return null
        val segments = url.segments

        return when (url.host) {
            "zhihu.com", "www.zhihu.com" -> resolveMainHost(segments, url)
            "zhuanlan.zhihu.com" -> resolveZhuanlanHost(segments, url)
            "api.zhihu.com" -> resolveApiHost(segments, url)
            "link.zhihu.com" -> resolveLinkRedirect(url)
            else -> {
                Log.w(TAG, "Cannot resolve content from url host: ${url.host}")
                null
            }
        }
    }

    private fun resolveMainHost(segments: List<String>, url: Url): NavDestination? {
        if (segments.size == 1 && segments[0] == "compose_answer_tab") {
            return Notification.Invitations
        }
        if (segments.size == 2 && segments[0] == "inbox") {
            return Notification.Message(
                peerId = segments[1],
                name = url.parameters["title"].orEmpty(),
            )
        }
        if (segments.size == 5 &&
            segments[0] == "notifications" &&
            segments[1] == "v3" &&
            segments[2] == "timeline" &&
            segments[3] == "entry"
        ) {
            return Notification.Entry(
                entryName = segments[4],
                title = url.parameters["title"].orEmpty().ifBlank { "消息" },
            )
        }
        if (segments.size == 4 && segments[0] == "question" && segments[2] == "answer") {
            val answerId = segments[3].toLongOrNull() ?: return null
            return PostDestination(type = PostType.Answer, id = answerId)
        }
        if (segments.size == 2 && segments[0] == "answer") {
            val answerId = segments[1].toLongOrNull() ?: return null
            return PostDestination(type = PostType.Answer, id = answerId)
        }
        if (segments.size == 2 && segments[0] == "question") {
            val questionId = segments[1].toLongOrNull() ?: return null
            return Question(questionId)
        }
        if (segments.size == 3 && segments[0] == "oia" && segments[1] == "articles") {
            val articleId = segments[2].toLongOrNull() ?: return null
            return PostDestination(type = PostType.Article, id = articleId)
        }
        if (segments.size == 2 && segments[0] == "people") {
            val urlToken = segments[1]
            return if (urlToken.length == 32 && urlToken.all { it in '0'..'9' || it in 'a'..'f' }) {
                Person(id = urlToken, urlToken = urlToken)
            } else {
                Person(id = Person.EMPTY_ID, urlToken = urlToken)
            }
        }
        if (segments.size == 2 && segments[0] == "video") {
            val videoId = segments[1].toLongOrNull() ?: return null
            return Video(id = videoId)
        }
        if (segments.size == 2 && segments[0] == "pin") {
            val pinId = segments[1].toLongOrNull() ?: return null
            return PostDestination(type = PostType.Pin, id = pinId)
        }
        if (segments.size >= 2 && segments[0] == "topic") {
            return Topic(id = segments[1], section = segments.getOrNull(2).orEmpty())
        }
        if (segments.size == 3 && segments[0] == "appview") {
            val contentId = segments[2].toLongOrNull() ?: return null
            return when (segments[1]) {
                "pin" -> PostDestination(type = PostType.Pin, id = contentId)
                "answer" -> PostDestination(type = PostType.Answer, id = contentId)
                "p" -> PostDestination(type = PostType.Article, id = contentId)
                else -> null
            }
        }
        if (segments.size == 1 && segments[0] == "search") {
            val query = url.parameters["q"].orEmpty()
            return Search(query)
        }
        if (segments.size == 2 && segments[0] == "column") {
            return Column(columnId = segments[1])
        }
        if (segments.size == 2 && (segments[0] == "collection" || segments[0] == "collections")) {
            return CollectionContent(collectionId = segments[1])
        }

        Log.w(TAG, "Cannot resolve content from url: $url")
        return null
    }

    private fun resolveZhuanlanHost(segments: List<String>, url: Url): NavDestination? {
        if (segments.size == 2 && segments[0] == "p") {
            val articleId = segments[1].toLongOrNull() ?: return null
            return PostDestination(type = PostType.Article, id = articleId)
        }
        Log.w(TAG, "Cannot resolve content from zhuanlan url: $url")
        return null
    }

    private fun resolveApiHost(segments: List<String>, url: Url): NavDestination? {
        if (segments.size == 2 && segments[0] == "collections") {
            return CollectionContent(collectionId = segments[1])
        }
        Log.w(TAG, "Cannot resolve content from api url: $url")
        return null
    }

    private fun resolveLinkRedirect(url: Url): NavDestination? {
        val target = url.parameters["target"] ?: return null
        return resolve(target)
    }
}

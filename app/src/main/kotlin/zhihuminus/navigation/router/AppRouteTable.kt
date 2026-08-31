package com.zhihuminus.navigation.router

import com.zhihuminus.feature.post.PostType
import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.CollectionContent
import com.zhihuminus.navigation.Collections
import com.zhihuminus.navigation.Column
import com.zhihuminus.navigation.CommentHolder
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.HotList
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.MyCollections
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.Notification
import com.zhihuminus.navigation.Person
import com.zhihuminus.navigation.PostDestination
import com.zhihuminus.navigation.Question
import com.zhihuminus.navigation.Search
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.navigation.Topic
import com.zhihuminus.navigation.Video
import io.ktor.http.DEFAULT_PORT
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol

/** 应用自有 URL 协议的 scheme。 */
const val APP_URL_SCHEME = "zhminus"

/**
 * 一次路由匹配时传给规则的参数。
 *
 * [segments] 是 host 之后的路径段，[queryParameters] 取每个查询参数的第一个值。
 */
class RouteArguments(
    val segments: List<String>,
    val queryParameters: Map<String, String>,
) {
    fun segment(index: Int): String? = segments.getOrNull(index)

    fun longSegment(index: Int): Long? = segment(index)?.toLongOrNull()

    operator fun get(name: String): String? = queryParameters[name]
}

/**
 * 一条 `zhminus://` 路由规则。
 *
 * @param host URL 的 host，即资源类型（如 `question`、`settings`）
 * @param resolve 正向解析：URL → 目的地；返回 null 表示该 URL 无法映射
 * @param encode 反向编码：目的地 → 去掉 scheme 的 URL 部分（如 `question/123`）；
 * null 表示该规则不负责此目的地或目的地无法编码。为 null 时跳过反向编码。
 */
internal class RouteRule(
    val host: String,
    private val segmentCounts: IntRange,
    internal val resolve: (RouteArguments) -> RouteResolution?,
    internal val encode: ((NavDestination) -> String?)? = null,
) {
    fun matches(host: String, segmentCount: Int): Boolean =
        host.equals(this.host, ignoreCase = true) && segmentCount in segmentCounts
}

/**
 * 应用内部导航 URL 协议的声明式路由表。
 *
 * URL 只编码身份字段（id、type、urlToken 等）；标题、作者、摘要等展示字段由页面进入后自行加载。
 * 知乎 Web URL（https/zhihu 官方 deeplink）的解析仍由 [com.zhihuminus.navigation.resolveContent] 负责。
 */
internal val APP_ROUTE_RULES: List<RouteRule> = buildList {
    // ── 内容 ────────────────────────────────────────────────────────────────

    add(
        screenRule(
            "question",
            1..1,
            parse = { args ->
                Question(questionId = args.longSegment(0) ?: return@screenRule null)
            },
            encode = { destination ->
                (destination as? Question)?.let { buildAppUrl("question", it.questionId.toString()) }
            },
        ),
    )

    add(postRule(PostType.Answer))
    add(postRule(PostType.Article))
    add(postRule(PostType.Pin))

    add(
        screenRule(
            "video",
            1..1,
            parse = { args -> args.longSegment(0)?.let(::Video) },
            encode = { destination ->
                (destination as? Video)?.let { buildAppUrl("video", it.id.toString()) }
            },
        ),
    )

    add(
        screenRule(
            "topic",
            1..2,
            parse = { args ->
                args.segment(0)?.takeIf(String::isNotEmpty)?.let { id ->
                    Topic(id = id, section = args.segment(1).orEmpty())
                }
            },
            encode = { destination ->
                (destination as? Topic)?.let {
                    if (it.section.isEmpty()) buildAppUrl("topic", it.id) else buildAppUrl("topic", it.id, it.section)
                }
            },
        ),
    )

    add(
        screenRule(
            "column",
            1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::Column) },
            encode = { destination ->
                (destination as? Column)?.let { buildAppUrl("column", it.columnId) }
            },
        ),
    )

    // ── 用户与收藏 ──────────────────────────────────────────────────────────

    add(
        screenRule(
            "people",
            1..1,
            parse = { args ->
                val token = args.segment(0) ?: return@screenRule null
                if (token.length == 32 && token.all { it in '0'..'9' || it in 'a'..'f' }) {
                    // 32 位十六进制字符，通常是用户 ID。
                    Person(id = token, urlToken = token)
                } else {
                    // 可读 token。
                    Person(id = Person.EMPTY_ID, urlToken = token)
                }
            },
            encode = { destination ->
                (destination as? Person)?.let { buildAppUrl("people", it.userTokenOrId) }
            },
        ),
    )

    add(
        screenRule(
            "collections",
            1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::Collections) },
            encode = { destination ->
                (destination as? Collections)?.let { buildAppUrl("collections", it.userToken) }
            },
        ),
    )

    add(
        screenRule(
            "collection",
            1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::CollectionContent) },
            encode = { destination ->
                (destination as? CollectionContent)?.let { buildAppUrl("collection", it.collectionId) }
            },
        ),
    )

    // ── 搜索 ────────────────────────────────────────────────────────────────

    add(
        screenRule(
            "search",
            0..0,
            parse = { args ->
                Search(
                    query = args["q"].orEmpty(),
                    restrictedMemberHashId = args["member"].orEmpty(),
                )
            },
            encode = { destination ->
                (destination as? Search)?.let {
                    buildAppUrl(
                        "search",
                        queryParameters = listOf(
                            "q" to it.query,
                            "member" to it.restrictedMemberHashId,
                        ),
                    )
                }
            },
        ),
    )

    // ── 评论锚点 ────────────────────────────────────────────────────────────

    add(
        screenRule(
            "comment",
            2..2,
            parse = { args ->
                val contentId = args.longSegment(1) ?: return@screenRule null
                val commentId = args["anchor"]
                    ?.takeIf { id -> id.isNotEmpty() && id.all(Char::isDigit) }
                    ?: return@screenRule null
                val article: NavDestination = when (args.segment(0)) {
                    PostType.Answer.urlSegment -> PostDestination(type = PostType.Answer, id = contentId)
                    PostType.Article.urlSegment -> PostDestination(type = PostType.Article, id = contentId)
                    PostType.Pin.urlSegment -> PostDestination(type = PostType.Pin, id = contentId)
                    "question" -> Question(questionId = contentId)
                    else -> return@screenRule null
                }
                CommentHolder(commentId = commentId, article = article)
            },
            encode = { destination ->
                (destination as? CommentHolder)?.let { holder ->
                    when (val article = holder.article) {
                        is PostDestination ->
                            commentUrl(article.type.urlSegment, article.id.toString(), holder.commentId)

                        is Question ->
                            commentUrl("question", article.questionId.toString(), holder.commentId)

                        else -> null
                    }
                }
            },
        ),
    )

    // ── 主 pager tab：tab 不是 NavHost route，单独返回 Tab 分辨率 ─────────────

    add(
        RouteRule(
            host = "tab",
            segmentCounts = 1..1,
            resolve = { args ->
                args
                    .segment(0)
                    ?.lowercase()
                    ?.let(TAB_DESTINATIONS::get)
                    ?.let(RouteResolution::Tab)
            },
            encode = { destination -> if (destination == MainTabs) buildAppUrl("tab", "home") else null },
        ),
    )

    // ── 消息中心 ────────────────────────────────────────────────────────────

    add(
        screenRule(
            "notification",
            0..0,
            parse = { Notification },
            encode = { destination -> if (destination == Notification) buildAppUrl("notification") else null },
        ),
    )

    add(
        screenRule(
            "notification",
            2..2,
            parse = { args ->
                when (args.segment(0)) {
                    "entry" ->
                        args
                            .segment(1)
                            ?.takeIf(String::isNotEmpty)
                            ?.let { Notification.Entry(entryName = it, title = "") }

                    "message" ->
                        args
                            .segment(1)
                            ?.takeIf(String::isNotEmpty)
                            ?.let { Notification.Message(peerId = it) }

                    else -> null
                }
            },
            encode = { destination ->
                when (destination) {
                    is Notification.Entry -> buildAppUrl("notification", "entry", destination.entryName)
                    is Notification.Message -> buildAppUrl("notification", "message", destination.peerId)
                    else -> null
                }
            },
        ),
    )

    add(
        screenRule(
            "notification",
            1..1,
            parse = { args ->
                when (args.segment(0)) {
                    "invitations" -> Notification.Invitations
                    "settings" -> Notification.NotificationSettings(setting = args["setting"].orEmpty())
                    else -> null
                }
            },
            encode = { destination ->
                when (destination) {
                    is Notification.Invitations -> buildAppUrl("notification", "invitations")
                    is Notification.NotificationSettings -> buildAppUrl(
                        "notification",
                        "settings",
                        queryParameters = listOf("setting" to destination.setting),
                    )

                    else -> null
                }
            },
        ),
    )

    // ── 设置页 ──────────────────────────────────────────────────────────────

    add(
        screenRule(
            "settings",
            1..1,
            parse = { args ->
                val setting = args["setting"].orEmpty()
                when (args.segment(0)) {
                    "appearance" -> Account.AppearanceSettings(setting)
                    "reading" -> Account.ReadingSettings
                    "identity" -> Account.IdentityManagement
                    "system" -> Account.SystemAndUpdateSettings(setting)
                    "search" -> Account.SettingsSearch
                    "licenses" -> Account.OpenSourceLicenses
                    else -> null
                }
            },
            encode = { destination ->
                when (destination) {
                    is Account.AppearanceSettings -> settingsUrl("appearance", destination.setting)
                    is Account.ReadingSettings -> settingsUrl("reading", "")
                    is Account.IdentityManagement -> settingsUrl("identity", "")
                    is Account.SystemAndUpdateSettings -> settingsUrl("system", destination.setting)
                    is Account.SettingsSearch -> settingsUrl("search", "")
                    is Account.OpenSourceLicenses -> settingsUrl("licenses", "")
                    else -> null
                }
            },
        ),
    )
}

private val TAB_DESTINATIONS: Map<String, TopLevelDestination> = mapOf(
    "home" to Home,
    "follow" to Follow,
    "hot" to HotList,
    "daily" to Daily,
    "history" to History,
    "collections" to MyCollections,
    "account" to Account,
)

private val PostType.urlSegment: String
    get() = when (this) {
        PostType.Answer -> "answer"
        PostType.Article -> "article"
        PostType.Pin -> "pin"
    }

private fun postRule(type: PostType): RouteRule = screenRule(
    type.urlSegment,
    1..1,
    parse = { args -> args.longSegment(0)?.let { id -> PostDestination(type = type, id = id) } },
    encode = { destination ->
        (destination as? PostDestination)
            ?.takeIf { it.type == type }
            ?.let { buildAppUrl(type.urlSegment, it.id.toString()) }
    },
)

private fun commentUrl(contentType: String, contentId: String, anchor: String): String =
    buildAppUrl(
        "comment",
        contentType,
        contentId,
        queryParameters = listOf("anchor" to anchor),
    )

private fun settingsUrl(page: String, setting: String): String =
    buildAppUrl(
        "settings",
        page,
        queryParameters = listOf("setting" to setting),
    )

private fun screenRule(
    host: String,
    segmentCounts: IntRange,
    parse: (RouteArguments) -> NavDestination?,
    encode: ((NavDestination) -> String?)? = null,
): RouteRule = RouteRule(
    host = host,
    segmentCounts = segmentCounts,
    resolve = { args -> parse(args)?.let(RouteResolution::Screen) },
    encode = encode,
)

/**
 * 构建去掉 scheme 前缀的应用 URL（`host/path?query`）。
 * 值为 null 或空的查询参数会被省略。
 */
internal fun buildAppUrl(
    host: String,
    vararg segments: String,
    queryParameters: List<Pair<String, String?>> = emptyList(),
): String = URLBuilder(
    protocol = URLProtocol(APP_URL_SCHEME, DEFAULT_PORT),
    host = host,
).apply {
    pathSegments = segments.toList()
    queryParameters.forEach { (name, value) ->
        if (!value.isNullOrEmpty()) parameters.append(name, value)
    }
}.buildString()

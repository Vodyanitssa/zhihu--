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
import com.zhihuminus.navigation.MainTabs
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

/** 应用支持的 Deeplink 协议 scheme，与知乎官方保持一致。 */
const val APP_URL_SCHEME = "zhihu"

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
 * 一条 `zhihu://` 路由规则。
 *
 * @param hosts 支持匹配的 host 集合（支持官方复数与单数兼容）
 * @param segmentCounts 允许的路径段范围
 * @param resolve 正向解析：URL → 目的地；返回 null 表示该 URL 无法映射
 * @param encode 反向编码：目的地 → 去掉 scheme 的 URL 部分；为 null 时跳过反向编码
 */
internal class RouteRule(
    val host: String,
    private val hosts: Set<String> = setOf(host),
    private val segmentCounts: IntRange,
    internal val resolve: (RouteArguments) -> RouteResolution?,
    internal val encode: ((NavDestination) -> String?)? = null,
) {
    constructor(
        host: String,
        segmentCounts: IntRange,
        resolve: (RouteArguments) -> RouteResolution?,
        encode: ((NavDestination) -> String?)? = null,
    ) : this(
        host = host,
        hosts = setOf(host),
        segmentCounts = segmentCounts,
        resolve = resolve,
        encode = encode,
    )

    fun matches(host: String, segmentCount: Int): Boolean =
        hosts.any { it.equals(host, ignoreCase = true) } && segmentCount in segmentCounts
}

/**
 * 应用导航 URL 协议的声明式路由表，采用知乎官方 `zhihu://` 协议规范。
 *
 * URL 只编码身份字段（id、type、urlToken 等）；标题、作者、摘要等展示字段由页面进入后自行加载。
 */
internal val APP_ROUTE_RULES: List<RouteRule> = buildList {
    // ── 内容 ────────────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("questions", "question"),
            segmentCounts = 1..1,
            parse = { args ->
                args.longSegment(0)?.let(::Question)
            },
            encode = { destination ->
                (destination as? Question)?.let { buildAppUrl("questions", it.questionId.toString()) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("answers", "answer"),
            segmentCounts = 1..1,
            parse = { args ->
                args.longSegment(0)?.let { id -> PostDestination(type = PostType.Answer, id = id) }
            },
            encode = { destination ->
                (destination as? PostDestination)
                    ?.takeIf { it.type == PostType.Answer }
                    ?.let { buildAppUrl("answers", it.id.toString()) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("articles", "article"),
            segmentCounts = 1..1,
            parse = { args ->
                args.longSegment(0)?.let { id -> PostDestination(type = PostType.Article, id = id) }
            },
            encode = { destination ->
                (destination as? PostDestination)
                    ?.takeIf { it.type == PostType.Article }
                    ?.let { buildAppUrl("articles", it.id.toString()) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("pin"),
            segmentCounts = 1..1,
            parse = { args ->
                args.longSegment(0)?.let { id -> PostDestination(type = PostType.Pin, id = id) }
            },
            encode = { destination ->
                (destination as? PostDestination)
                    ?.takeIf { it.type == PostType.Pin }
                    ?.let { buildAppUrl("pin", it.id.toString()) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("video"),
            segmentCounts = 1..1,
            parse = { args -> args.longSegment(0)?.let(::Video) },
            encode = { destination ->
                (destination as? Video)?.let { buildAppUrl("video", it.id.toString()) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("topic", "topics"),
            segmentCounts = 1..2,
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
            hosts = setOf("pin20"),
            segmentCounts = 0..1,
            parse = { args ->
                val topicId = args["topic_id"]
                    ?: args["topicId"]
                    ?: args.segment(0)?.takeIf { it.all(Char::isDigit) }
                topicId?.let(::Topic)
            },
            encode = null,
        ),
    )

    add(
        screenRule(
            hosts = setOf("column"),
            segmentCounts = 1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::Column) },
            encode = { destination ->
                (destination as? Column)?.let { buildAppUrl("column", it.columnId) }
            },
        ),
    )

    // ── 用户与收藏 ──────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("people"),
            segmentCounts = 1..1,
            parse = { args ->
                val token = args.segment(0) ?: return@screenRule null
                if (token.length == 32 && token.all { it in '0'..'9' || it in 'a'..'f' }) {
                    Person(id = token, urlToken = token)
                } else {
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
            hosts = setOf("collections"),
            segmentCounts = 1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::Collections) },
            encode = { destination ->
                (destination as? Collections)?.let { buildAppUrl("collections", it.userToken) }
            },
        ),
    )

    add(
        screenRule(
            hosts = setOf("collection"),
            segmentCounts = 1..1,
            parse = { args -> args.segment(0)?.takeIf(String::isNotEmpty)?.let(::CollectionContent) },
            encode = { destination ->
                (destination as? CollectionContent)?.let { buildAppUrl("collection", it.collectionId) }
            },
        ),
    )

    // ── 搜索 ────────────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("search"),
            segmentCounts = 0..0,
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
            hosts = setOf("comment"),
            segmentCounts = 2..3,
            parse = { args ->
                val isList = args.segments.size == 3 && args.segment(0) == "list"
                val typeStr = if (isList) args.segment(1) else args.segment(0)
                val contentId = (if (isList) args.longSegment(2) else args.longSegment(1))
                    ?: return@screenRule null
                val commentId = (args["anchor_comment_id"] ?: args["anchor"])
                    ?.takeIf { id -> id.isNotEmpty() && id.all(Char::isDigit) }
                    ?: return@screenRule null

                val article: NavDestination = when (typeStr) {
                    "answer", "answers" -> PostDestination(type = PostType.Answer, id = contentId)
                    "article", "articles" -> PostDestination(type = PostType.Article, id = contentId)
                    "pin" -> PostDestination(type = PostType.Pin, id = contentId)
                    "question", "questions" -> Question(questionId = contentId)
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

    // ── 官方 Feed / 主页 ────────────────────────────────────────────────────

    add(
        RouteRule(
            host = "feed",
            segmentCounts = 0..0,
            resolve = { RouteResolution.Tab(Home) },
            encode = { destination ->
                if (destination == MainTabs) buildAppUrl("feed") else null
            },
        ),
    )

    // ── 主 pager tab：tab 不是 NavHost route，单独返回 Tab 分辨率 ─────────────

    add(
        RouteRule(
            host = "tab",
            segmentCounts = 1..1,
            resolve = { args ->
                val tab = args.segment(0)?.lowercase()
                when (tab) {
                    "history" -> RouteResolution.Screen(History)
                    "account" -> RouteResolution.Screen(Account)
                    else -> tab?.let(TAB_DESTINATIONS::get)?.let(RouteResolution::Tab)
                }
            },
            encode = { destination ->
                when (destination) {
                    MainTabs -> buildAppUrl("feed")
                    Account -> buildAppUrl("tab", "account")
                    else -> null
                }
            },
        ),
    )

    // ── 历史记录 ────────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("history"),
            segmentCounts = 0..0,
            parse = { History },
            encode = { destination -> if (destination == History) buildAppUrl("history") else null },
        ),
    )

    // ── 消息中心 ────────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("notification"),
            segmentCounts = 0..0,
            parse = { Notification },
            encode = { destination -> if (destination == Notification) buildAppUrl("notification") else null },
        ),
    )

    add(
        screenRule(
            hosts = setOf("notification"),
            segmentCounts = 2..2,
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
            hosts = setOf("notification"),
            segmentCounts = 1..1,
            parse = { args ->
                when (args.segment(0)) {
                    "invitations" -> Notification.Invitations
                    else -> null
                }
            },
            encode = { destination ->
                when (destination) {
                    is Notification.Invitations -> buildAppUrl("notification", "invitations")
                    else -> null
                }
            },
        ),
    )

    // ── 设置页 ──────────────────────────────────────────────────────────────

    add(
        screenRule(
            hosts = setOf("settings"),
            segmentCounts = 1..1,
            parse = { args ->
                val setting = args["setting"].orEmpty()
                when (args.segment(0)) {
                    "appearance", "reading", "system" -> Account.AppearanceSettings(setting)
                    "notification" -> Account.NotificationSettings(setting)
                    "search" -> Account.SettingsSearch
                    "licenses" -> Account.OpenSourceLicenses
                    else -> null
                }
            },
            encode = { destination ->
                when (destination) {
                    is Account.AppearanceSettings -> settingsUrl("appearance", destination.setting)
                    is Account.NotificationSettings -> settingsUrl("notification", destination.setting)
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
    "daily" to Daily,
)

private val PostType.urlSegment: String
    get() = when (this) {
        PostType.Answer -> "answer"
        PostType.Article -> "article"
        PostType.Pin -> "pin"
    }

private fun commentUrl(contentType: String, contentId: String, anchor: String): String =
    buildAppUrl(
        "comment",
        "list",
        contentType,
        contentId,
        queryParameters = listOf("anchor_comment_id" to anchor),
    )

private fun settingsUrl(page: String, setting: String): String =
    buildAppUrl(
        "settings",
        page,
        queryParameters = listOf("setting" to setting),
    )

private fun screenRule(
    hosts: Set<String>,
    segmentCounts: IntRange,
    parse: (RouteArguments) -> NavDestination?,
    encode: ((NavDestination) -> String?)? = null,
): RouteRule = RouteRule(
    host = hosts.first(),
    hosts = hosts,
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

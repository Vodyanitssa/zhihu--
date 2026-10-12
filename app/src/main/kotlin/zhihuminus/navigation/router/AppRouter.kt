package com.zhihuminus.navigation.router

import com.zhihuminus.core.util.Log
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.MainTabs
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.navigation.WatchLater
import io.ktor.http.Url

/**
 * 一次 URL 解析的结果。
 */
sealed interface RouteResolution {
    /**
     * 压入主 NavHost 返回栈的普通屏幕。
     */
    data class Screen(
        val destination: NavDestination,
    ) : RouteResolution

    /**
     * 主 pager 的 tab 切换。tab 不是返回栈条目，调用方应通过
     * `mainTabNavigationTarget` 桥让主壳切换 pager 页。
     */
    data class Tab(
        val destination: TopLevelDestination,
    ) : RouteResolution
}

/**
 * 应用路由门面。
 * 支持官方 `zhihu://` 协议及知乎 HTTP/HTTPS Web 链接的统一解析。
 */
object AppRouter {
    private const val TAG = "AppRouter"

    /**
     * 统一解析任意已知 URL（`zhihu://` 官方 Deeplink、知乎网页链接等）。
     * 自动处理 URL 归一化与非法输入，格式错误或未知链接返回 null。
     */
    fun resolveAny(url: String): RouteResolution? = runCatching {
        val normalized = UrlNormalizer.normalize(url)
        val parsed = Url(normalized)
        when (parsed.protocol.name.lowercase()) {
            APP_URL_SCHEME -> resolve(parsed)
            "http", "https" -> ZhihuWebUrlResolver.resolve(parsed)?.let(RouteResolution::Screen)
            else -> null
        }
    }.getOrNull()

    /**
     * 统一解析任意已知 URL 为屏幕目的地；若为 Tab 切换则返回 [MainTabs]。
     */
    fun resolveDestination(url: String): NavDestination? = when (val res = resolveAny(url)) {
        is RouteResolution.Screen -> res.destination
        is RouteResolution.Tab -> MainTabs
        null -> null
    }

    /** 解析 `zhihu://` Deeplink；格式错误或非本协议的 URL 返回 null。 */
    fun resolve(url: String): RouteResolution? = runCatching { resolve(Url(url)) }.getOrNull()

    /** 解析已解析过的 [Url]；host 或路径无法映射到任何路由规则时返回 null。 */
    fun resolve(url: Url): RouteResolution? {
        if (!url.protocol.name.equals(APP_URL_SCHEME, ignoreCase = true)) return null
        val arguments = RouteArguments(
            segments = url.segments,
            queryParameters = buildMap {
                url.parameters.forEach { name, values ->
                    put(name, values.firstOrNull().orEmpty())
                }
            },
        )
        val rule = APP_ROUTE_RULES.firstOrNull { it.matches(url.host, arguments.segments.size) }
            ?: return unmatched(url)
        return rule.resolve(arguments) ?: unmatched(url)
    }

    private fun unmatched(url: Url): RouteResolution? {
        Log.w(TAG, "Cannot resolve app url: $url")
        return null
    }
}

/**
 * 把目的地反向编码为 `zhihu://` 应用 URL；无法编码的目的地返回 null。
 */
fun NavDestination.toAppUrl(): String? =
    APP_ROUTE_RULES.firstNotNullOfOrNull { rule -> rule.encode?.invoke(this) }

/**
 * 把顶层 tab 反向编码为 tab 切换 URL。
 */
fun TopLevelDestination.toAppUrl(): String? = when (this) {
    Home -> buildAppUrl("feed")
    Follow -> buildAppUrl("tab", "follow")
    Daily -> buildAppUrl("tab", "daily")
    WatchLater -> buildAppUrl("tab", "watch_later")
    else -> null
}

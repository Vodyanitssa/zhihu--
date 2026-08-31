package com.zhihuminus.navigation.router

import com.zhihuminus.navigation.Account
import com.zhihuminus.navigation.Daily
import com.zhihuminus.navigation.Follow
import com.zhihuminus.navigation.History
import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.HotList
import com.zhihuminus.navigation.MyCollections
import com.zhihuminus.navigation.NavDestination
import com.zhihuminus.navigation.TopLevelDestination
import com.zhihuminus.util.Log
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
 * 应用自有 `zhminus://` URL 协议的路由门面。
 *
 * Router 只负责「URL ↔ 目的地」的双向转换；导航副作用（历史记录、评论锚点交接、
 * 视频外跳等）仍由 [com.zhihuminus.MainActivity.navigate] 统一处理。
 */
object AppRouter {
    private const val TAG = "AppRouter"

    /** 解析应用 URL；格式错误或非本协议的 URL 返回 null。 */
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
 * 把目的地反向编码为应用 URL；无法编码的目的地（如携带富字段的
 * [com.zhihuminus.navigation.SegmentCommentHolder]）返回 null。
 */
fun NavDestination.toAppUrl(): String? =
    APP_ROUTE_RULES.firstNotNullOfOrNull { rule -> rule.encode?.invoke(this) }

/**
 * 把顶层 tab 反向编码为 tab 切换 URL。
 */
fun TopLevelDestination.toAppUrl(): String? = when (this) {
    Home -> buildAppUrl("tab", "home")
    Follow -> buildAppUrl("tab", "follow")
    HotList -> buildAppUrl("tab", "hot")
    Daily -> buildAppUrl("tab", "daily")
    History -> buildAppUrl("tab", "history")
    MyCollections -> buildAppUrl("tab", "collections")
    Account -> buildAppUrl("tab", "account")
    else -> null
}

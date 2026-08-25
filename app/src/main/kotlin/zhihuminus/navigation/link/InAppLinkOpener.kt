package com.zhihuminus.navigation.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.resolveContent
import com.zhihuminus.navigation.router.AppRouter
import com.zhihuminus.navigation.router.RouteResolution
import com.zhihuminus.platform.rememberExternalUrlOpener

/**
 * 内容链接点击的统一入口：优先解析为应用内目的地导航，
 * 无法解析时回落到外部浏览器，保持原有行为。
 */
@Composable
fun rememberInAppLinkOpener(): (String) -> Unit {
    val navigator = LocalNavigator.current
    val openExternalUrl = rememberExternalUrlOpener()
    return remember(navigator, openExternalUrl) {
        { url ->
            val normalized = normalizeContentUrl(url)
            when (val resolution = AppRouter.resolve(normalized)) {
                is RouteResolution.Screen -> navigator.onNavigate(resolution.destination)
                is RouteResolution.Tab -> navigator.onNavigateTopLevel(resolution.destination)
                null -> resolveContent(normalized)?.let(navigator.onNavigate) ?: openExternalUrl(url)
            }
        }
    }
}

/** 知乎 HTML 里的链接常见 `//host/path` 与 `/path` 相对形式，补全后才能被路由解析。 */
private fun normalizeContentUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    url.startsWith("/") -> "https://www.zhihu.com$url"
    else -> url
}

package com.zhihuminus.navigation.link

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.zhihuminus.core.platform.rememberExternalUrlOpener
import com.zhihuminus.navigation.LocalNavigator
import com.zhihuminus.navigation.router.AppRouter
import com.zhihuminus.navigation.router.RouteResolution

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
            when (val resolution = AppRouter.resolveAny(url)) {
                is RouteResolution.Screen -> navigator.onNavigate(resolution.destination)
                is RouteResolution.Tab -> navigator.onNavigateTopLevel(resolution.destination)
                null -> openExternalUrl(url)
            }
        }
    }
}

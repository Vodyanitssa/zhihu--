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

package com.zhihuminus.ui

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import com.zhihuminus.MainActivity
import com.zhihuminus.data.zhihu.ZhihuApiImpl
import com.zhihuminus.data.zhihu.ZhihuCommentRepository
import com.zhihuminus.data.zhihu.ZhihuPostRepository
import com.zhihuminus.feature.post.PostRoute
import com.zhihuminus.viewmodel.rememberPaginationEnvironment

/**
 * Android 平台的 Zhihu++ 主界面入口。
 *
 * 这里把 [MainActivity] 持有的导航、偏好设置、文章页 ViewModel 和回答切换转场适配到共享 [ZhihuMain]。
 * UI 结构仍由 common 主壳负责，Android 只提供生命周期、Activity、ViewModel 和平台专属页面实现。
 */
@Composable
fun AndroidZhihuMain(navController: NavHostController) {
    val activity = LocalActivity.current as MainActivity
    ZhihuMain(
        navController = navController,
        mainTabNavigationTarget = activity.mainTabNavigationTarget,
        navigate = activity::navigate,
        setCurrentMainTabOpenFrom = activity::setCurrentMainTabOpenFrom,
        consumeMainTabNavigationTarget = activity::consumeMainTabNavigationTarget,
        preferenceState = rememberAndroidZhihuMainPreferenceState(),
        isDarkTheme = com.zhihuminus.theme.ThemeManager.isDarkTheme,
        postContent = { destination, _ ->
            // 深链锚点：MainActivity 暂存的评论 ID（若有），透传到评论组件做定位
            val pendingCommentId = remember(destination) {
                activity.consumePendingCommentId(destination)
            }
            val environment = rememberPaginationEnvironment(allowGuestAccess = false)
            val repository = remember {
                val api = ZhihuApiImpl(environment)
                ZhihuPostRepository(api)
            }
            val commentRepository = remember {
                val api = ZhihuApiImpl(environment)
                ZhihuCommentRepository(api)
            }
            PostRoute(
                destination = destination,
                repository = repository,
                commentRepository = commentRepository,
                initialCommentId = pendingCommentId,
                onBack = { navController.popBackStack() },
            )
        },
    )
}

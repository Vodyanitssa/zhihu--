package com.zhihuminus.core.environment

import android.content.Context

interface ArticleExportContentEnvironment :
    ArticleExportEnvironment,
    ZhihuApiEnvironment

interface ContentLoadEnvironment :
    ZhihuApiEnvironment,
    HistoryEnvironment

interface ProfileLoadEnvironment : ContentLoadEnvironment

interface ArticleLoadEnvironment :
    ZhihuApiEnvironment,
    ContentLoadEnvironment

interface PaginationEnvironment :
    ZhihuApiEnvironment,
    AccountEnvironment,
    MobileHomeFeedEnvironment,
    ClipboardEnvironment,
    ProfileLoadEnvironment,
    ArticleLoadEnvironment,
    ArticleExportContentEnvironment

interface AndroidContextPaginationEnvironment : PaginationEnvironment {
    val context: Context
}

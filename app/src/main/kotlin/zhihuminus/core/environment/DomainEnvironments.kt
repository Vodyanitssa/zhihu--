package com.zhihuminus.core.environment

import com.zhihuminus.navigation.NavDestination
import io.ktor.client.HttpClient

interface AccountEnvironment {
    suspend fun refreshAccountProfile() = Unit

    fun requestLogin(): Boolean = false

    fun clearAccountSession() = Unit

    fun currentAccountId(): String = ""

    fun restartApplication() = Unit

    suspend fun verifyLogin(cookies: Map<String, String>): Boolean = false

    fun saveCookies(cookies: Map<String, String>) = Unit

    fun logout() = clearAccountSession()
}

interface MobileHomeFeedEnvironment : ZhihuApiEnvironment {
    fun mobileHomeFeedHttpClient(): HttpClient = httpClient()

    suspend fun handleMobileHomeFeedFailure(error: Exception) {
        handleFetchFailure("AndroidHomeFeedViewModel", error)
    }
}

interface HistoryEnvironment {
    suspend fun postHistoryDestination(destination: NavDestination) = Unit
}

interface ClipboardEnvironment {
    fun setPlainTextClipboard(
        label: String,
        text: String,
    ) = Unit
}

interface ArticleExportEnvironment {
    fun hasImageExportPermission(): Boolean = false

    fun requestImageExportPermission() = Unit

    fun loadExportAssetText(fileName: String): String = ""
}

package com.zhihuminus.data

import com.zhihuminus.core.environment.ZhihuApiEnvironment
import io.ktor.client.HttpClient

object FakeZhihuApiEnvironment : ZhihuApiEnvironment {
    override fun httpClient(): HttpClient = error("Not implemented for test")

    override fun authenticatedCookies(): Map<String, String> = emptyMap()

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
}

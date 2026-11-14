package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FakeZhihuApiEnvironment
import com.zhihuminus.data.zhihu.api.ZhihuCollectionApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuCollectionRepositoryTest {
    private class RecordingHistoryApi : ZhihuHistoryApi(FakeZhihuApiEnvironment) {
        var token: String? = null
        var type: String? = null
        var referer: String? = null

        override suspend fun addHistory(contentToken: String, contentType: String, referer: String?) {
            token = contentToken
            type = contentType
            this.referer = referer
        }
    }

    @Test
    fun testRecordReadSendsCollectionTokenAndReferer() = runBlocking {
        val historyApi = RecordingHistoryApi()
        val repository = ZhihuCollectionRepository(ZhihuCollectionApi(FakeZhihuApiEnvironment), historyApi)

        repository.recordRead("727548193")

        assertEquals("727548193", historyApi.token)
        assertEquals("collection", historyApi.type)
        assertEquals("https://www.zhihu.com/collection/727548193", historyApi.referer)
    }
}

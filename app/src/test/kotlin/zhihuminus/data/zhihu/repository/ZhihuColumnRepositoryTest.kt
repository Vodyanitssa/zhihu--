package com.zhihuminus.data.zhihu.repository

import com.zhihuminus.data.FakeZhihuApiEnvironment
import com.zhihuminus.data.common.ZhihuJson
import com.zhihuminus.data.zhihu.api.ZhihuColumnApi
import com.zhihuminus.data.zhihu.api.ZhihuHistoryApi
import com.zhihuminus.data.zhihu.api.ZhihuPeopleApi
import com.zhihuminus.data.zhihu.dto.ColumnArticlePage
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZhihuColumnRepositoryTest {
    private fun repositoryReturning(json: String): ZhihuColumnRepository {
        val api = object : ZhihuColumnApi(FakeZhihuApiEnvironment) {
            override suspend fun getColumnArticles(columnId: String, nextUrl: String?): ColumnArticlePage =
                ZhihuJson.decodeFromString(json)
        }
        return ZhihuColumnRepository(
            api,
            ZhihuPeopleApi(FakeZhihuApiEnvironment),
            ZhihuHistoryApi(FakeZhihuApiEnvironment),
        )
    }

    @Test
    fun testBrokenArticlesDoNotBreakWholePage() = runBlocking {
        val repository = repositoryReturning(
            """
            {"paging":{"totals":2},"data":[
              {"id":"123","type":"article"},
              {"id":"456","title":"正常文章","url":"https://zhuanlan.zhihu.com/p/456","excerpt":"摘要"}
            ]}
            """.trimIndent(),
        )

        val result = repository.getColumnArticles("c_1", null)

        assertEquals(2, result.articles.size)
        assertEquals("", result.articles.first().title)
        assertEquals("正常文章", result.articles.last().title)
        assertEquals(2, result.totals)
    }

    @Test
    fun testPageWithoutDataDecodesToEmptyList() = runBlocking {
        val result = repositoryReturning("""{"paging":{"totals":0}}""").getColumnArticles("c_1", null)

        assertTrue(result.articles.isEmpty())
        assertTrue(result.isEnd)
    }
}

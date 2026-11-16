package com.zhihuminus.data.zhihu.repository

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZhihuSearchRepositoryTest {
    @Test
    fun testGeneralSearchContentEntryTypes() {
        assertTrue(isGeneralSearchContentEntry("search_result"))
        assertTrue(isGeneralSearchContentEntry("zvideo"))
        assertFalse(isGeneralSearchContentEntry("relevant_query"))
        assertFalse(isGeneralSearchContentEntry("people"))
        assertFalse(isGeneralSearchContentEntry("topic"))
    }
}

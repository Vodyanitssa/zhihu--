package com.zhihuminus.feature.collection

import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionTest {
    @Test
    fun testCollectionSubtitleText() {
        val publicCollection = Collection(id = "1", title = "Public Col", itemCount = 5, isPublic = true)
        assertEquals("5 内容·公开", publicCollection.subtitleText)

        val privateCollection = Collection(id = "2", title = "Private Col", itemCount = 5, isPublic = false)
        assertEquals("5 内容·仅自己可见", privateCollection.subtitleText)

        val zeroItemCollection = Collection(id = "3", title = "Zero Col", itemCount = 0, isPublic = false)
        assertEquals("0 内容·仅自己可见", zeroItemCollection.subtitleText)
    }
}

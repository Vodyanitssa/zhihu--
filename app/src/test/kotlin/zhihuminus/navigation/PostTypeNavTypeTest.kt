package com.zhihuminus.navigation

import com.zhihuminus.feature.post.PostType
import kotlin.test.Test
import kotlin.test.assertEquals

class PostTypeNavTypeTest {
    @Test
    fun parseValueParsesValidEntries() {
        assertEquals(PostType.Answer, PostTypeNavType.parseValue("Answer"))
        assertEquals(PostType.Article, PostTypeNavType.parseValue("Article"))
        assertEquals(PostType.Pin, PostTypeNavType.parseValue("Pin"))
    }

    @Test
    fun parseValueDefaultsToArticleOnInvalid() {
        assertEquals(PostType.Article, PostTypeNavType.parseValue("Invalid"))
        assertEquals(PostType.Article, PostTypeNavType.parseValue(""))
        assertEquals(PostType.Article, PostTypeNavType.parseValue("UnknownType"))
    }

    @Test
    fun serializeAsValueOutputsName() {
        assertEquals("Answer", PostTypeNavType.serializeAsValue(PostType.Answer))
        assertEquals("Article", PostTypeNavType.serializeAsValue(PostType.Article))
        assertEquals("Pin", PostTypeNavType.serializeAsValue(PostType.Pin))
    }
}

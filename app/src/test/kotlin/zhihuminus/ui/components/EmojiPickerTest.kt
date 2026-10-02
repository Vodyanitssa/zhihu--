package com.zhihuminus.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals

class EmojiPickerTest {
    @Test
    fun insertIntoEmptyTextField() {
        val initial = TextFieldValue("")
        val result = initial.replaceSelection("[微笑]")
        assertEquals("[微笑]", result.text)
        assertEquals(TextRange("[微笑]".length), result.selection)
    }

    @Test
    fun appendAtEndOfText() {
        val initial = TextFieldValue("你好", selection = TextRange(2))
        val result = initial.replaceSelection("[握手]")
        assertEquals("你好[握手]", result.text)
        assertEquals(TextRange("你好[握手]".length), result.selection)
    }

    @Test
    fun insertInMiddleOfText() {
        val initial = TextFieldValue("你好世界", selection = TextRange(2))
        val result = initial.replaceSelection("[微笑]")
        assertEquals("你好[微笑]世界", result.text)
        assertEquals(TextRange("你好[微笑]".length), result.selection)
    }

    @Test
    fun replaceSelectedText() {
        val initial = TextFieldValue("你好世界", selection = TextRange(2, 4))
        val result = initial.replaceSelection("[大笑]")
        assertEquals("你好[大笑]", result.text)
        assertEquals(TextRange("你好[大笑]".length), result.selection)
    }

    @Test
    fun consecutiveInsertions() {
        var current = TextFieldValue("")
        current = current.replaceSelection("[微笑]")
        current = current.replaceSelection("[握手]")
        assertEquals("[微笑][握手]", current.text)
        assertEquals(TextRange("[微笑][握手]".length), current.selection)
    }
}

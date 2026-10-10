package com.zhihuminus.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlatformCapabilitiesTest {
    @Test
    fun testUserMessageSinkDispatchesCorrectDuration() {
        val shortMessages = mutableListOf<String>()
        val longMessages = mutableListOf<String>()

        val sink = UserMessageSink(
            showShortMessage = { shortMessages += it },
            showLongMessage = { longMessages += it },
        )

        sink.showMessage("hello", UserMessageDuration.Short)
        sink.showMessage("world", UserMessageDuration.Long)
        sink.showMessage("default")

        assertEquals(listOf("hello", "default"), shortMessages)
        assertEquals(listOf("world"), longMessages)
    }

    @Test
    fun testUserMessageSinkDefaultLongMessageDelegation() {
        val messages = mutableListOf<String>()
        val sink = UserMessageSink(
            showShortMessage = { messages += it },
        )

        sink.showMessage("msg", UserMessageDuration.Long)

        assertEquals(listOf("msg"), messages)
    }

    @Test
    fun testIsAndroidLiteVariantPackageName() {
        assertTrue(isAndroidLiteVariantPackageName("com.zhihuminus.lite"))
        assertTrue(isAndroidLiteVariantPackageName("org.test.app.lite"))
        assertFalse(isAndroidLiteVariantPackageName("com.zhihuminus"))
        assertFalse(isAndroidLiteVariantPackageName("com.zhihuminus.lite.debug"))
        assertFalse(isAndroidLiteVariantPackageName(""))
    }
}

package com.zhihuminus.ui.components

import com.zhihuminus.navigation.Home
import com.zhihuminus.navigation.TopLevelDestination
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelReselectTest {
    @Test
    fun resolveReselectBehavior_whenAtTop_returnsRefresh() {
        val behavior = resolveReselectBehavior(
            isAtTop = true,
            currentTime = 1000L,
            lastTapTime = 0L,
        )
        assertEquals(ReselectBehavior.Refresh, behavior)
    }

    @Test
    fun resolveReselectBehavior_whenNotAtTop_singleTap_returnsScrollToTop() {
        val behavior = resolveReselectBehavior(
            isAtTop = false,
            currentTime = 2000L,
            lastTapTime = 1000L, // 1000ms > 400ms threshold
        )
        assertEquals(ReselectBehavior.ScrollToTop, behavior)
    }

    @Test
    fun resolveReselectBehavior_whenNotAtTop_doubleTapWithinThreshold_returnsRefresh() {
        val behavior = resolveReselectBehavior(
            isAtTop = false,
            currentTime = 1250L,
            lastTapTime = 1000L, // 250ms <= 400ms threshold
        )
        assertEquals(ReselectBehavior.Refresh, behavior)
    }

    @Test
    fun resolveReselectBehavior_whenNotAtTop_tapExceedsThreshold_returnsScrollToTop() {
        val behavior = resolveReselectBehavior(
            isAtTop = false,
            currentTime = 1401L,
            lastTapTime = 1000L, // 401ms > 400ms threshold
        )
        assertEquals(ReselectBehavior.ScrollToTop, behavior)
    }

    @Test
    fun reselectFlow_emission_deliversExpectedDestination() = runBlocking {
        withTimeout(2000L) {
            val flow = MutableSharedFlow<TopLevelDestination>(extraBufferCapacity = 64)
            val received = CompletableDeferred<TopLevelDestination>()

            val job = launch {
                flow.collect { received.complete(it) }
            }

            // Ensure collector is actively subscribed before emitting
            flow.subscriptionCount.first { it > 0 }

            flow.tryEmit(Home)
            assertEquals(Home, received.await())
            job.cancel()
        }
    }
}

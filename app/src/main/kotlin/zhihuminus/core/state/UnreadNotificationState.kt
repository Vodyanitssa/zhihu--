package com.zhihuminus.core.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnreadNotificationState {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun update(newCount: Int) {
        _count.value = newCount.coerceAtLeast(0)
    }

    val currentCount: Int get() = _count.value
}

@Composable
fun rememberUnreadNotificationCount(): State<Int> =
    UnreadNotificationState.count.collectAsState()

fun formatUnreadCount(count: Int): String = if (count > 99) "99+" else count.toString()

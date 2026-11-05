package com.zhihuminus.core.platform

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

enum class UserMessageDuration {
    Short,
    Long,
}

data class UserMessageSink(
    val showShortMessage: (String) -> Unit,
    val showLongMessage: (String) -> Unit = showShortMessage,
    val showError: (String) -> Unit = showShortMessage,
) {
    fun showMessage(
        message: String,
        duration: UserMessageDuration = UserMessageDuration.Short,
    ) {
        when (duration) {
            UserMessageDuration.Short -> showShortMessage(message)
            UserMessageDuration.Long -> showLongMessage(message)
        }
    }
}

/** 主壳提供的 Snackbar 宿主；未提供时错误提示回退为 Toast。 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState?> { null }

fun androidUserMessageSink(context: Context): UserMessageSink {
    val appContext = context.applicationContext
    val mainHandler = Handler(Looper.getMainLooper())

    fun showToast(
        message: String,
        duration: Int,
    ) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            Toast.makeText(appContext, message, duration).show()
        } else {
            mainHandler.post {
                Toast.makeText(appContext, message, duration).show()
            }
        }
    }

    return UserMessageSink(
        showShortMessage = { message ->
            showToast(message, Toast.LENGTH_SHORT)
        },
        showLongMessage = { message ->
            showToast(message, Toast.LENGTH_LONG)
        },
    )
}

@Composable
fun rememberUserMessageSink(): UserMessageSink {
    val context = LocalContext.current.applicationContext
    val snackbarHostState = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    return remember(context, snackbarHostState, scope) {
        val toastSink = androidUserMessageSink(context)
        toastSink.copy(
            showError = { message ->
                if (snackbarHostState != null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(message, withDismissAction = true)
                    }
                } else {
                    toastSink.showShortMessage(message)
                }
            },
        )
    }
}

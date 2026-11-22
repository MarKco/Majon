package com.ilsecondodasinistra.majon.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * While the keyboard is up, Back closes it (and drops the focus) instead of leaving the screen.
 * Put it in every screen with text fields, next to `Modifier.imePadding()` on the scrollable content so the
 * field being typed in (and anything under it, like suggestions) rises above the keyboard.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyboardBackHandler() {
    val visible = WindowInsets.isImeVisible
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler(enabled = visible) {
        keyboard?.hide()
        focus.clearFocus()
    }
}

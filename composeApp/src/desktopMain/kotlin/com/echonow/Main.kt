package com.echonow

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.echonow.presentation.navigation.EchoNowApp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "ECHO NOW",
    ) {
        EchoNowApp()
    }
}

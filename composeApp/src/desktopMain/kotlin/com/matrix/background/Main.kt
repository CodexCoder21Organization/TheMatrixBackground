package com.matrix.background

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        size = DpSize(1024.dp, 768.dp)
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "The Matrix Background",
        state = windowState
    ) {
        MatrixBackground(
            modifier = Modifier.fillMaxSize(),
            speed = 1.0f,
            density = 20f,
            doFog = true,
            doWaves = true,
            doRotate = true,
            mode = MatrixMode.MATRIX
        )
    }
}

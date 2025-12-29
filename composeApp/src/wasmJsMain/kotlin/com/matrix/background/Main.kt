package com.matrix.background

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow(canvasElementId = "ComposeTarget") {
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

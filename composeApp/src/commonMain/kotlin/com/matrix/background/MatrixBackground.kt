package com.matrix.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Matrix Background Composable
 *
 * Renders the Matrix digital rain effect using Compose Canvas.
 * This is a faithful conversion of glmatrix.c by Jamie Zawinski.
 *
 * @param modifier Modifier for the composable
 * @param speed Animation speed multiplier (default 1.0)
 * @param density Number of strips (default 20)
 * @param doFog Enable depth-based fog effect (default true)
 * @param doWaves Enable brightness wave effect (default true)
 * @param doRotate Enable auto-rotation of view (default true)
 * @param mode Character mode (MATRIX, DNA, BINARY, etc.)
 * @param frameDelayMs Delay between frames in milliseconds (default 30, matching glmatrix.c)
 */
@Composable
fun MatrixBackground(
    modifier: Modifier = Modifier,
    speed: Float = DEF_SPEED,
    density: Float = DEF_DENSITY,
    doFog: Boolean = DEF_FOG,
    doWaves: Boolean = DEF_WAVES,
    doRotate: Boolean = DEF_ROTATE,
    mode: MatrixMode = MatrixMode.MATRIX,
    frameDelayMs: Long = 30L
) {
    val matrixState = remember {
        MatrixState(
            speed = speed,
            density = density,
            doFog = doFog,
            doWaves = doWaves,
            doRotate = doRotate,
            mode = mode
        )
    }

    val textMeasurer = rememberTextMeasurer()

    // Animation loop - update state every frame
    LaunchedEffect(Unit) {
        while (true) {
            matrixState.tick()
            delay(frameDelayMs)
        }
    }

    // Force recomposition on each tick
    var frameCount by remember { mutableStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            frameCount++
            delay(frameDelayMs)
        }
    }

    Canvas(
        modifier = modifier.background(Color.Black)
    ) {
        // Use frameCount to force redraw
        @Suppress("UNUSED_EXPRESSION")
        frameCount

        drawMatrix(matrixState, textMeasurer)
    }
}

/**
 * Draw the entire matrix effect
 * Based on draw_matrix() from glmatrix.c
 */
private fun DrawScope.drawMatrix(
    state: MatrixState,
    textMeasurer: TextMeasurer
) {
    val centerX = size.width / 2
    val centerY = size.height / 2

    // Calculate scale based on canvas size
    // The original uses a perspective projection with FOV 80 degrees
    val baseScale = minOf(size.width, size.height) / GRID_SIZE.toFloat()

    // Apply view rotation (simplified 2D rotation for the view angles)
    rotate(
        degrees = state.viewX * 0.1f,  // Scale down rotation for 2D
        pivot = Offset(centerX, centerY)
    ) {
        // Sort strips by z-depth (back to front) for proper alpha blending
        // From draw_matrix(): draw the ones farthest from the camera first
        val sortedStrips = state.strips.sortedBy { it.z }

        for (strip in sortedStrips) {
            drawStrip(state, strip, textMeasurer, centerX, centerY, baseScale)
        }
    }
}

/**
 * Draw all the visible glyphs in the strip.
 * Based on draw_strip() from glmatrix.c
 */
private fun DrawScope.drawStrip(
    state: MatrixState,
    s: Strip,
    textMeasurer: TextMeasurer,
    centerX: Float,
    centerY: Float,
    baseScale: Float
) {
    for (i in 0 until GRID_SIZE) {
        val g = s.glyphs[i]
        val belowP = s.spinnerY >= i

        // In erase mode, invert the condition
        val shouldDraw = if (s.erasingP) !belowP else belowP

        if (g != 0 && shouldDraw) {
            // Don't draw cells below the spinner (or above in erase mode)
            val brightness = state.calculateBrightness(s, i, g, s.highlight[i], s.z)
            drawGlyph(
                state = state,
                glyph = g,
                highlight = s.highlight[i],
                x = s.x,
                y = s.y - i,
                z = s.z,
                brightness = brightness,
                textMeasurer = textMeasurer,
                centerX = centerX,
                centerY = centerY,
                baseScale = baseScale
            )
        }
    }

    // Draw the spinner (the bright leading character)
    if (!s.erasingP) {
        val spinnerBrightness = state.calculateBrightness(
            s,
            s.spinnerY.toInt().coerceIn(0, GRID_SIZE - 1),
            s.spinnerGlyph,
            false,
            s.z
        )
        drawGlyph(
            state = state,
            glyph = s.spinnerGlyph,
            highlight = false,
            x = s.x,
            y = s.y - s.spinnerY,
            z = s.z,
            brightness = spinnerBrightness,
            textMeasurer = textMeasurer,
            centerX = centerX,
            centerY = centerY,
            baseScale = baseScale,
            isSpinner = true
        )
    }
}

/**
 * Draw a single character at the given position and brightness.
 * Based on draw_glyph() from glmatrix.c
 */
private fun DrawScope.drawGlyph(
    state: MatrixState,
    glyph: Int,
    highlight: Boolean,
    x: Float,
    y: Float,
    z: Float,
    brightness: Float,
    textMeasurer: TextMeasurer,
    centerX: Float,
    centerY: Float,
    baseScale: Float,
    isSpinner: Boolean = false
) {
    if (glyph == 0) return

    val actualGlyph = abs(glyph)
    val spinnerP = glyph < 0 || isSpinner

    // Get the character to draw
    val char = MatrixGlyphs.getChar(actualGlyph)

    // Calculate perspective scale based on z-depth
    // Objects closer (higher z) appear larger
    // z ranges from approximately -GRID_DEPTH*0.5 to GRID_DEPTH*SPLASH_RATIO
    val depthRatio = (z + GRID_DEPTH * 0.5f) / (GRID_DEPTH * (SPLASH_RATIO + 0.5f))
    val perspectiveScale = 0.3f + (depthRatio * 0.7f)  // Scale from 0.3 to 1.0

    // Calculate screen position
    // Apply simple perspective: closer objects (higher z) are offset more from center
    val perspectiveX = centerX + (x * baseScale * perspectiveScale)
    val perspectiveY = centerY - (y * baseScale * perspectiveScale)  // Invert Y for screen coords

    // Calculate font size based on perspective
    val fontSize = (baseScale * perspectiveScale * 0.9f).coerceIn(8f, 48f)

    // Calculate color
    // In the original: spinners are white, regular glyphs are green
    // Brightness affects alpha
    val adjustedBrightness = brightness * (if (spinnerP) 1.5f else 1.0f) * (if (highlight) 2.0f else 1.0f)
    val finalBrightness = adjustedBrightness.coerceIn(0f, 1f)

    val color = if (spinnerP) {
        // Spinner (leading character) - bright white/green
        Color(
            red = finalBrightness * 0.8f,
            green = finalBrightness,
            blue = finalBrightness * 0.8f,
            alpha = finalBrightness
        )
    } else {
        // Regular glyph - green
        Color(
            red = finalBrightness * 0.2f,
            green = finalBrightness,
            blue = finalBrightness * 0.2f,
            alpha = finalBrightness
        )
    }

    // Draw the character
    val style = TextStyle(
        color = color,
        fontSize = fontSize.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = if (spinnerP) FontWeight.Bold else FontWeight.Normal
    )

    val textLayoutResult = textMeasurer.measure(
        text = char.toString(),
        style = style
    )

    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = perspectiveX - textLayoutResult.size.width / 2f,
            y = perspectiveY - textLayoutResult.size.height / 2f
        )
    )
}

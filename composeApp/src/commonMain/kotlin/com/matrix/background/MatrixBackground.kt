package com.matrix.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Matrix Background Composable
 *
 * Renders the Matrix digital rain effect using Compose Canvas.
 * This is a faithful port of glmatrix.c by Jamie Zawinski.
 *
 * The rendering uses the exact same projection math as the original OpenGL code:
 * - gluPerspective(80.0, 1/h, 1.0, 100)
 * - gluLookAt(0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
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
    frameDelayMs: Long = 30L  // *delay: 30000 from DEFAULTS in glmatrix.c (30000 microseconds = 30ms)
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

// OpenGL projection constants from glmatrix.c reshape_matrix()
private const val FOV_DEGREES = 80.0
private const val NEAR_PLANE = 1.0
private const val FAR_PLANE = 100.0
private const val CAMERA_Z = 25.0  // from gluLookAt(0, 0, 25, ...)

// Precomputed: focal length = 1 / tan(fov/2)
// tan(40 degrees) = tan(40 * PI / 180) ≈ 0.8391
private val FOCAL_LENGTH = (1.0 / tan((FOV_DEGREES / 2) * PI / 180.0)).toFloat()

// Helper function to convert degrees to radians (Kotlin multiplatform compatible)
private fun toRadians(degrees: Double): Double = degrees * PI / 180.0

/**
 * Project a 3D point to 2D screen coordinates using the same math as OpenGL.
 *
 * From glmatrix.c:
 *   gluPerspective(80.0, 1/h, 1.0, 100)
 *   gluLookAt(0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
 *
 * @param worldX X position in world coordinates
 * @param worldY Y position in world coordinates
 * @param worldZ Z position in world coordinates
 * @param viewRotX View rotation around X axis (from auto_track)
 * @param viewRotY View rotation around Y axis (from auto_track)
 * @param screenWidth Width of the screen/canvas
 * @param screenHeight Height of the screen/canvas
 * @return Pair of (screenX, screenY) or null if behind camera
 */
private fun project3Dto2D(
    worldX: Float,
    worldY: Float,
    worldZ: Float,
    viewRotX: Float,
    viewRotY: Float,
    screenWidth: Float,
    screenHeight: Float
): ProjectedPoint? {
    // Apply view rotation (from glRotatef in draw_matrix)
    // Rotation is applied in the order: rotateX then rotateY
    val radX = toRadians(viewRotX.toDouble())
    val radY = toRadians(viewRotY.toDouble())

    val cosX = cos(radX).toFloat()
    val sinX = sin(radX).toFloat()
    val cosY = cos(radY).toFloat()
    val sinY = sin(radY).toFloat()

    // Rotate around X axis
    val y1 = worldY * cosX - worldZ * sinX
    val z1 = worldY * sinX + worldZ * cosX

    // Rotate around Y axis
    val x2 = worldX * cosY + z1 * sinY
    val z2 = -worldX * sinY + z1 * cosY

    // Transform to camera space (camera at z=25 looking at origin)
    // In camera space, z increases away from camera
    val cameraZ = CAMERA_Z.toFloat() - z2

    // Don't render if behind camera or too close
    if (cameraZ <= NEAR_PLANE.toFloat()) return null

    // Perspective projection
    // OpenGL perspective: x_clip = x * focal / z, y_clip = y * focal / z
    val aspect = screenWidth / screenHeight
    val projX = (x2 * FOCAL_LENGTH) / cameraZ
    val projY = (y1 * FOCAL_LENGTH) / cameraZ

    // Convert from normalized device coordinates [-1, 1] to screen coordinates
    // Account for aspect ratio
    val screenX = (projX / aspect + 1f) * screenWidth / 2f
    val screenY = (1f - projY) * screenHeight / 2f  // Flip Y for screen coords

    // Calculate the projected size of a 1-unit quad at this depth
    val projectedSize = (FOCAL_LENGTH / cameraZ) * minOf(screenWidth, screenHeight) / 2f

    return ProjectedPoint(screenX, screenY, projectedSize, cameraZ)
}

private data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val size: Float,
    val cameraZ: Float
)

/**
 * Draw the entire matrix effect
 * Based on draw_matrix() from glmatrix.c
 */
private fun DrawScope.drawMatrix(
    state: MatrixState,
    textMeasurer: TextMeasurer
) {
    // Sort strips by z-depth (back to front) for proper alpha blending
    // From draw_matrix(): "draw the ones farthest from the camera first"
    val sortedStrips = state.strips.sortedBy { it.z }

    for (strip in sortedStrips) {
        drawStrip(state, strip, textMeasurer)
    }
}

/**
 * Draw all the visible glyphs in the strip.
 * Based on draw_strip() from glmatrix.c
 */
private fun DrawScope.drawStrip(
    state: MatrixState,
    s: Strip,
    textMeasurer: TextMeasurer
) {
    // From draw_strip() in glmatrix.c:
    // for (i = 0; i < GRID_SIZE; i++)
    for (i in 0 until GRID_SIZE) {
        val g = s.glyphs[i]
        val belowP = s.spinnerY >= i

        // if (s->erasing_p) below_p = !below_p;
        val shouldDraw = if (s.erasingP) !belowP else belowP

        // if (g && below_p) - don't draw cells below the spinner
        if (g != 0 && shouldDraw) {
            val brightness = calculateGlyphBrightness(state, s, i, g)
            drawGlyph(
                state = state,
                glyph = g,
                highlight = s.highlight[i],
                x = s.x,
                y = s.y - i,
                z = s.z,
                brightness = brightness,
                textMeasurer = textMeasurer
            )
        }
    }

    // if (!s->erasing_p) draw_glyph(...spinner...)
    if (!s.erasingP) {
        val spinnerBrightness = calculateGlyphBrightness(state, s, s.spinnerY.toInt(), s.spinnerGlyph)
        drawGlyph(
            state = state,
            glyph = s.spinnerGlyph,
            highlight = false,
            x = s.x,
            y = s.y - s.spinnerY,
            z = s.z,
            brightness = 1.0f,  // Spinner always at full brightness
            textMeasurer = textMeasurer
        )
    }
}

/**
 * Calculate brightness for a glyph position
 * Based on draw_strip() brightness calculation in glmatrix.c
 */
private fun calculateGlyphBrightness(
    state: MatrixState,
    s: Strip,
    glyphIndex: Int,
    glyph: Int
): Float {
    // From draw_strip():
    // if (!do_waves) brightness = 1.0;
    // else { int j = WAVE_SIZE - ((i + (GRID_SIZE - s->wave_position)) % WAVE_SIZE);
    //        brightness = mp->brightness_ramp[j]; }
    return if (!state.doWaves) {
        1.0f
    } else {
        val idx = glyphIndex.coerceIn(0, GRID_SIZE - 1)
        val rawIndex = WAVE_SIZE - ((idx + (GRID_SIZE - s.wavePosition)) % WAVE_SIZE)
        val j = if (rawIndex >= WAVE_SIZE) 0 else rawIndex
        state.brightnessRamp[j]
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
    textMeasurer: TextMeasurer
) {
    // if (glyph == 0) abort(); - but we check before calling
    if (glyph == 0) return

    val spinnerP = glyph < 0
    val actualGlyph = abs(glyph)

    // Project 3D position to screen
    val projected = project3Dto2D(
        worldX = x,
        worldY = y,
        worldZ = z,
        viewRotX = state.viewX,
        viewRotY = state.viewY,
        screenWidth = size.width,
        screenHeight = size.height
    ) ?: return  // Behind camera

    // From draw_glyph():
    // GLfloat S = 1;
    // if (!do_texture) { S = 0.8; x += 0.1; y += 0.1; }
    val S = 1.0f

    // Calculate final brightness with all modifiers from draw_glyph()
    var finalBrightness = brightness

    // if (spinner_p) brightness *= 1.5;
    if (spinnerP) {
        finalBrightness *= 1.5f
    }

    // if (highlight) brightness *= 2;
    if (highlight) {
        finalBrightness *= 2f
    }

    // Fog effect from draw_glyph():
    // if (do_fog) {
    //   GLfloat depth = (z / GRID_DEPTH) + 0.5;
    //   depth = 0.2 + (depth * 0.8);
    //   brightness *= depth;
    // }
    if (state.doFog) {
        var depth = (z / GRID_DEPTH) + 0.5f
        depth = 0.2f + (depth * 0.8f)
        finalBrightness *= depth
    }

    // Fade out when close to screen from draw_glyph():
    // if (z > GRID_DEPTH/2) {
    //   GLfloat ratio = ((z - GRID_DEPTH/2) / ((GRID_DEPTH * SPLASH_RATIO) - GRID_DEPTH/2));
    //   int i = ratio * WAVE_SIZE;
    //   if (i < 0) i = 0; else if (i >= WAVE_SIZE) i = WAVE_SIZE-1;
    //   a *= mp->brightness_ramp[i];
    // }
    if (z > GRID_DEPTH / 2f) {
        val ratio = ((z - GRID_DEPTH / 2f) / ((GRID_DEPTH * SPLASH_RATIO) - GRID_DEPTH / 2f))
        val i = (ratio * WAVE_SIZE).toInt().coerceIn(0, WAVE_SIZE - 1)
        finalBrightness *= state.brightnessRamp[i]
    }

    finalBrightness = finalBrightness.coerceIn(0f, 1f)

    // Color calculation from draw_glyph():
    // if (!do_texture && !spinner_p) r = b = 0, g = 1;
    // else r = g = b = 1;
    // a = brightness;
    // glColor4f(r, g, b, a);
    //
    // The original uses GL_SRC_ALPHA, GL_ONE blending which is additive
    // For spinner: white (r=g=b=1), for regular: also white but will appear green due to texture
    // Since we don't have the texture, we use green for regular glyphs
    val color = if (spinnerP) {
        // Spinner - bright white/green tinted
        Color(
            red = finalBrightness,
            green = finalBrightness,
            blue = finalBrightness,
            alpha = finalBrightness
        )
    } else {
        // Regular glyph - green (matching the Matrix aesthetic)
        // The original texture has green characters, RGB gets multiplied by the texture
        Color(
            red = 0f,
            green = finalBrightness,
            blue = 0f,
            alpha = finalBrightness
        )
    }

    // Get the character to draw
    val char = MatrixGlyphs.getChar(actualGlyph)

    // Calculate font size based on projected size
    // A 1-unit quad in the original becomes projected.size pixels
    val fontSize = (projected.size * S * 0.8f).coerceIn(6f, 72f)

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

    // Draw centered at the projected position
    // In the original, the quad goes from (x,y) to (x+S, y+S)
    // We center the text on the projected point
    drawText(
        textLayoutResult = textLayoutResult,
        topLeft = Offset(
            x = projected.screenX - textLayoutResult.size.width / 2f,
            y = projected.screenY - textLayoutResult.size.height / 2f
        )
    )
}

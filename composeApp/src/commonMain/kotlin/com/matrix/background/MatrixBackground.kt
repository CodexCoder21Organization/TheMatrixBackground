package com.matrix.background

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.imageResource
import thematrixbackground.composeapp.generated.resources.Res
import thematrixbackground.composeapp.generated.resources.matrix3
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
 * Uses the original matrix3.png texture atlas for pixel-perfect rendering.
 *
 * The rendering uses the exact same projection math as the original OpenGL code:
 * - gluPerspective(80.0, 1/h, 1.0, 100)
 * - gluLookAt(0.0, 0.0, 25.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0)
 */
@OptIn(ExperimentalResourceApi::class)
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

    // Load the original matrix3.png texture
    val texture = imageResource(Res.drawable.matrix3)

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

        drawMatrix(matrixState, texture)
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

// Texture dimensions from glmatrix.c
// The original texture is 512x598, padded to 512x512 after spank_image()
// Character grid is 16 columns x 13 rows (but reduced to 11 rows after spank_image)
private const val TEXTURE_WIDTH = 512
private const val TEXTURE_HEIGHT = 512  // After padding/spanking
private const val ORIGINAL_HEIGHT = 598
private const val REAL_CHAR_ROWS = 11  // After spank_image removes 2 rows

/**
 * Project a 3D point to 2D screen coordinates using the same math as OpenGL.
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

    // Transform to camera space
    val cameraZ = CAMERA_Z.toFloat() - z2

    if (cameraZ <= NEAR_PLANE.toFloat()) return null

    // Perspective projection
    val aspect = screenWidth / screenHeight
    val projX = (x2 * FOCAL_LENGTH) / cameraZ
    val projY = (y1 * FOCAL_LENGTH) / cameraZ

    // Convert to screen coordinates
    val screenX = (projX / aspect + 1f) * screenWidth / 2f
    val screenY = (1f - projY) * screenHeight / 2f

    // Calculate the projected size of a 1-unit quad
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
 */
private fun DrawScope.drawMatrix(
    state: MatrixState,
    texture: ImageBitmap
) {
    // Sort strips by z-depth (back to front)
    val sortedStrips = state.strips.sortedBy { it.z }

    for (strip in sortedStrips) {
        drawStrip(state, strip, texture)
    }
}

/**
 * Draw all the visible glyphs in the strip.
 */
private fun DrawScope.drawStrip(
    state: MatrixState,
    s: Strip,
    texture: ImageBitmap
) {
    for (i in 0 until GRID_SIZE) {
        val g = s.glyphs[i]
        val belowP = s.spinnerY >= i
        val shouldDraw = if (s.erasingP) !belowP else belowP

        if (g != 0 && shouldDraw) {
            val brightness = calculateGlyphBrightness(state, s, i)
            drawGlyph(
                state = state,
                glyph = g,
                highlight = s.highlight[i],
                x = s.x,
                y = s.y - i,
                z = s.z,
                brightness = brightness,
                texture = texture
            )
        }
    }

    // Draw the spinner
    if (!s.erasingP) {
        drawGlyph(
            state = state,
            glyph = s.spinnerGlyph,
            highlight = false,
            x = s.x,
            y = s.y - s.spinnerY,
            z = s.z,
            brightness = 1.0f,
            texture = texture
        )
    }
}

/**
 * Calculate brightness for a glyph position
 */
private fun calculateGlyphBrightness(
    state: MatrixState,
    s: Strip,
    glyphIndex: Int
): Float {
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
 * Draw a single glyph using texture coordinates from matrix3.png
 *
 * From glmatrix.c draw_glyph():
 * - cx, cy are texture coordinates for the character
 * - The quad is drawn from (x,y,z) to (x+S, y+S, z)
 * - Color is modulated by brightness and alpha
 */
private fun DrawScope.drawGlyph(
    state: MatrixState,
    glyph: Int,
    highlight: Boolean,
    x: Float,
    y: Float,
    z: Float,
    brightness: Float,
    texture: ImageBitmap
) {
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
    ) ?: return

    // From glmatrix.c: GLfloat S = 1;
    val S = 1.0f

    // Calculate final brightness with all modifiers
    var finalBrightness = brightness

    // if (spinner_p) brightness *= 1.5;
    if (spinnerP) {
        finalBrightness *= 1.5f
    }

    // if (highlight) brightness *= 2;
    if (highlight) {
        finalBrightness *= 2f
    }

    // Fog effect
    if (state.doFog) {
        var depth = (z / GRID_DEPTH) + 0.5f
        depth = 0.2f + (depth * 0.8f)
        finalBrightness *= depth
    }

    // Fade out when close to screen
    if (z > GRID_DEPTH / 2f) {
        val ratio = ((z - GRID_DEPTH / 2f) / ((GRID_DEPTH * SPLASH_RATIO) - GRID_DEPTH / 2f))
        val i = (ratio * WAVE_SIZE).toInt().coerceIn(0, WAVE_SIZE - 1)
        finalBrightness *= state.brightnessRamp[i]
    }

    finalBrightness = finalBrightness.coerceIn(0f, 1f)

    // Calculate texture coordinates
    // From glmatrix.c:
    // int ccx = ((glyph - 1) % CHAR_COLS);
    // int ccy = ((glyph - 1) / CHAR_COLS);
    // cx = ccx * w;
    // cy = (mp->real_char_rows - ccy - 1) * h;
    val glyphIndex = actualGlyph - 1
    val charCol = glyphIndex % CHAR_COLS
    val charRow = glyphIndex / CHAR_COLS

    // Character dimensions in the texture
    val charWidth = texture.width / CHAR_COLS
    val charHeight = texture.height / REAL_CHAR_ROWS

    // Texture coordinates (note: y is flipped in the original)
    val srcX = charCol * charWidth
    val srcY = (REAL_CHAR_ROWS - charRow - 1) * charHeight

    // Screen size for this glyph
    val screenSize = (projected.size * S).coerceIn(4f, 100f)

    // Draw the texture region with color modulation
    // The original uses GL_SRC_ALPHA, GL_ONE blending (additive)
    // and sets glColor4f(r, g, b, a) where a = brightness
    //
    // In the original: if (!do_texture && !spinner_p) r=b=0, g=1; else r=g=b=1;
    // Since we're using texture, r=g=b=1, and the texture provides the green color

    val colorFilter = ColorFilter.tint(
        Color(
            red = finalBrightness,
            green = finalBrightness,
            blue = finalBrightness,
            alpha = finalBrightness
        ),
        BlendMode.Modulate
    )

    drawImage(
        image = texture,
        srcOffset = IntOffset(srcX.coerceIn(0, texture.width - charWidth), srcY.coerceIn(0, texture.height - charHeight)),
        srcSize = IntSize(charWidth, charHeight),
        dstOffset = IntOffset(
            (projected.screenX - screenSize / 2).toInt(),
            (projected.screenY - screenSize / 2).toInt()
        ),
        dstSize = IntSize(screenSize.toInt(), screenSize.toInt()),
        colorFilter = colorFilter
    )
}

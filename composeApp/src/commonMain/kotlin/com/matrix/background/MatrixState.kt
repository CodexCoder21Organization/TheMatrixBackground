package com.matrix.background

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Matrix configuration - holds the state of the entire animation
 * Direct conversion from glmatrix.c matrix_configuration struct
 */
class MatrixState(
    val speed: Float = DEF_SPEED,
    density: Float = DEF_DENSITY,
    val doClock: Boolean = DEF_CLOCK,
    val timeFmt: String = DEF_TIMEFMT,
    val doFog: Boolean = DEF_FOG,
    val doWaves: Boolean = DEF_WAVES,
    val doRotate: Boolean = DEF_ROTATE,
    mode: MatrixMode = MatrixMode.MATRIX
) {
    // Button/touch down state - pauses animation
    var buttonDownP: Boolean = false

    // Number of strips
    val nStrips: Int

    // Array of strips
    val strips: Array<Strip>

    // Glyph map based on mode
    val glyphMap: IntArray

    // Number of glyphs in the map
    val nGlyphs: Int

    // Auto-tracking direction of view
    var lastView: Int = 0
    var targetView: Int = 0
    var viewX: Float = 0f
    var viewY: Float = 0f
    var viewSteps: Int = 100
    var viewTick: Int = 0
    var autoTrackingP: Boolean = false
    var trackTick: Int = 0

    // Brightness ramp for waves
    val brightnessRamp: FloatArray = computeBrightnessRamp()

    // Random number generator
    private val random = Random

    init {
        // Set up glyph map based on mode
        when (mode) {
            MatrixMode.MATRIX -> {
                glyphMap = matrixEncoding
                nGlyphs = matrixEncoding.size
            }
            MatrixMode.DNA -> {
                glyphMap = dnaEncoding
                nGlyphs = dnaEncoding.size
            }
            MatrixMode.BINARY -> {
                glyphMap = binaryEncoding
                nGlyphs = binaryEncoding.size
            }
            MatrixMode.HEXADECIMAL -> {
                glyphMap = hexEncoding
                nGlyphs = hexEncoding.size
            }
            MatrixMode.DECIMAL -> {
                glyphMap = decimalEncoding
                nGlyphs = decimalEncoding.size
            }
        }

        // To scale coverage-percent to strips, this number looks about right...
        // From init_matrix() in glmatrix.c
        nStrips = (density * 2.2f).toInt().coerceIn(1, 2000)

        // Initialize strips
        strips = Array(nStrips) { Strip() }
        for (i in 0 until nStrips) {
            val s = strips[i]
            resetStrip(s)

            // If we start all strips from zero at once, then the first few seconds
            // of the animation are much denser than normal. So instead, set all
            // the initial strips to erase-mode with random starting positions.
            // As these die off at random speeds and are re-created, we'll get a
            // more consistent density.
            s.erasingP = true
            s.spinnerY = frand(GRID_SIZE.toFloat())
            s.glyphs.fill(0) // no visible glyphs
        }

        // Initialize auto-tracking
        autoTrackInit()
    }

    /**
     * BELLRAND macro from glmatrix.c
     * Returns a bell-curve distributed random number between 0 and n
     */
    private fun bellrand(n: Float): Float {
        return (frand(n) + frand(n) + frand(n)) / 3f
    }

    /**
     * frand - returns a random float between 0 and n
     */
    private fun frand(n: Float): Float {
        return random.nextFloat() * n
    }

    /**
     * Re-randomize the state of one strip.
     * Direct conversion of reset_strip() from glmatrix.c
     */
    fun resetStrip(s: Strip) {
        var timeDisplayedP = false // never display time twice in one strip

        // Clear the strip
        s.x = frand(GRID_SIZE.toFloat()) - (GRID_SIZE / 2f)
        s.y = GRID_SIZE / 2f + bellrand(0.5f)  // shift top slightly
        s.z = (GRID_DEPTH * 0.2f) - frand(GRID_DEPTH * 0.7f)
        s.spinnerY = 0f

        s.dx = 0f
        // s.dx = (bellrand(0.01f) - 0.005f) * speed  // commented out in original
        s.dy = 0f
        s.dz = bellrand(0.02f) * speed

        s.spinnerSpeed = bellrand(0.3f) * speed

        s.spinSpeed = (bellrand(2.0f / speed) + 1).toInt()
        s.spinTick = 0

        s.wavePosition = 0
        s.waveSpeed = (bellrand(3.0f / speed) + 1).toInt()
        s.waveTick = 0

        s.erasingP = false

        for (i in 0 until GRID_SIZE) {
            if (doClock &&
                !timeDisplayedP &&
                (i < GRID_SIZE - 5) &&  // display approx. once per 5 strips
                (random.nextInt((GRID_SIZE - 5) * 5) == 0)
            ) {
                // Time display code - simplified for now
                // In the original, this renders the current time
                // For now, we skip this feature
                timeDisplayedP = true
            } else {
                val drawP = random.nextInt(7) != 0
                val spinP = drawP && (random.nextInt(20) == 0)
                var g = if (drawP) {
                    glyphMap[random.nextInt(nGlyphs)] + 1
                } else {
                    0
                }
                if (spinP) g = -g
                s.glyphs[i] = g
                s.highlight[i] = false
            }
        }

        s.spinnerGlyph = -(glyphMap[random.nextInt(nGlyphs)] + 1)
    }

    /**
     * Animate the strip one step. Reset if it has reached the bottom.
     * Direct conversion of tick_strip() from glmatrix.c
     */
    fun tickStrip(s: Strip) {
        if (buttonDownP) return

        s.x += s.dx
        s.y += s.dy
        s.z += s.dz

        if (s.z > GRID_DEPTH * SPLASH_RATIO) {
            // splashed into screen
            resetStrip(s)
            return
        }

        s.spinnerY += s.spinnerSpeed
        if (s.spinnerY >= GRID_SIZE) {
            if (s.erasingP) {
                resetStrip(s)
                return
            } else {
                s.erasingP = true
                s.spinnerY = 0f
                s.spinnerSpeed /= 2  // erase it slower than we drew it
            }
        }

        // Spin the spinners
        s.spinTick++
        if (s.spinTick > s.spinSpeed) {
            s.spinTick = 0
            s.spinnerGlyph = -(glyphMap[random.nextInt(nGlyphs)] + 1)
            for (i in 0 until GRID_SIZE) {
                if (s.glyphs[i] < 0) {
                    s.glyphs[i] = -(glyphMap[random.nextInt(nGlyphs)] + 1)
                    if (random.nextInt(800) == 0) {
                        // sometimes they stop spinning
                        s.glyphs[i] = -s.glyphs[i]
                    }
                }
            }
        }

        // Move the color (brightness) wave
        s.waveTick++
        if (s.waveTick > s.waveSpeed) {
            s.waveTick = 0
            s.wavePosition++
            if (s.wavePosition >= WAVE_SIZE) {
                s.wavePosition = 0
            }
        }
    }

    /**
     * Initialize auto-tracking
     * From auto_track_init() in glmatrix.c
     */
    private fun autoTrackInit() {
        lastView = 0
        targetView = 0
        viewX = niceViews[lastView].x
        viewY = niceViews[lastView].y
        viewSteps = 100
        viewTick = 0
        autoTrackingP = false
    }

    /**
     * Auto-tracking camera movement
     * From auto_track() in glmatrix.c
     */
    fun autoTrack() {
        if (!doRotate) return
        if (buttonDownP) return

        // if we're not moving, maybe start moving. Otherwise, do nothing.
        if (!autoTrackingP) {
            trackTick++
            if (trackTick < (20 / speed).toInt()) return
            trackTick = 0
            if (random.nextInt(20) != 0) return
            autoTrackingP = true
        }

        val ox = niceViews[lastView].x
        val oy = niceViews[lastView].y
        val tx = niceViews[targetView].x
        val ty = niceViews[targetView].y

        // move from A to B with sinusoidal deltas, so that it doesn't jerk to a stop.
        val th = sin((PI / 2) * viewTick.toDouble() / viewSteps).toFloat()

        viewX = ox + ((tx - ox) * th)
        viewY = oy + ((ty - oy) * th)
        viewTick++

        if (viewTick >= viewSteps) {
            viewTick = 0
            viewSteps = (350.0f / speed).toInt()
            lastView = targetView
            targetView = random.nextInt(niceViews.size - 1) + 1
            autoTrackingP = false
        }
    }

    /**
     * Calculate brightness for a glyph
     * From draw_glyph() in glmatrix.c
     */
    fun calculateBrightness(
        s: Strip,
        glyphIndex: Int,
        glyph: Int,
        highlight: Boolean,
        z: Float
    ): Float {
        val spinnerP = glyph < 0

        var brightness = if (!doWaves) {
            1.0f
        } else {
            // Calculate wave index, ensuring it wraps properly to stay in bounds [0, WAVE_SIZE-1]
            val rawIndex = WAVE_SIZE - ((glyphIndex + (GRID_SIZE - s.wavePosition)) % WAVE_SIZE)
            val j = if (rawIndex >= WAVE_SIZE) 0 else rawIndex
            brightnessRamp[j]
        }

        if (spinnerP) {
            brightness *= 1.5f
        }

        if (highlight) {
            brightness *= 2f
        }

        if (doFog) {
            var depth = (z / GRID_DEPTH) + 0.5f  // z ratio from back/front
            depth = 0.2f + (depth * 0.8f)        // scale to range [0.2 - 1.0]
            brightness *= depth                   // so no row goes all black.
        }

        // If the glyph is very close to the screen (meaning it is very large,
        // and is about to splash into the screen and vanish) then start fading
        // it out, proportional to how close to the glass it is.
        if (z > GRID_DEPTH / 2f) {
            val ratio = ((z - GRID_DEPTH / 2f) /
                    ((GRID_DEPTH * SPLASH_RATIO) - GRID_DEPTH / 2f))
            val i = (ratio * WAVE_SIZE).toInt().coerceIn(0, WAVE_SIZE - 1)
            brightness *= brightnessRamp[i]
        }

        return brightness.coerceIn(0f, 1f)
    }

    /**
     * Get absolute glyph value (handles spinner glyphs which are negative)
     */
    fun getAbsoluteGlyph(glyph: Int): Int {
        return abs(glyph)
    }

    /**
     * Tick all strips
     */
    fun tick() {
        for (strip in strips) {
            tickStrip(strip)
        }
        autoTrack()
    }
}

/**
 * Matrix display mode
 */
enum class MatrixMode {
    MATRIX,
    DNA,
    BINARY,
    HEXADECIMAL,
    DECIMAL
}

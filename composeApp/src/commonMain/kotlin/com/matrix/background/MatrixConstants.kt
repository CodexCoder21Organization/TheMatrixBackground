package com.matrix.background

import kotlin.math.PI
import kotlin.math.sin

/**
 * Constants and encoding arrays from glmatrix.c
 * Copyright (c) 2003-2018 Jamie Zawinski <jwz@jwz.org>
 * Converted to Kotlin for Compose Multiplatform
 */

// Grid and rendering constants from glmatrix.c
const val GRID_SIZE = 70      // width and height of the arena
const val GRID_DEPTH = 35     // depth of the arena
const val WAVE_SIZE = 22      // periodicity of color (brightness) waves
const val SPLASH_RATIO = 0.7f // ratio of GRID_DEPTH where chars hit the screen

// Character texture constants
const val CHAR_COLS = 16
const val CHAR_ROWS = 13

// Default parameter values from glmatrix.c
const val DEF_SPEED = 1.0f
const val DEF_DENSITY = 20f
const val DEF_CLOCK = false
const val DEF_FOG = true
const val DEF_WAVES = true
const val DEF_ROTATE = true
const val DEF_TEXTURE = true
const val DEF_TIMEFMT = " %l%M%p "

// Cursor glyph index
const val CURSOR_GLYPH = 97

/**
 * Matrix encoding - the glyphs used in "Matrix" mode
 * These are indices into the character texture atlas
 */
val matrixEncoding = intArrayOf(
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25,
    // Using the second set (160-175) as in the original
    160, 161, 162, 163, 164, 165, 166, 167,
    168, 169, 170, 171, 172, 173, 174, 175
)

/**
 * Decimal encoding - digits 0-9
 */
val decimalEncoding = intArrayOf(
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25
)

/**
 * Hexadecimal encoding - digits 0-9 and A-F
 */
val hexEncoding = intArrayOf(
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 33, 34, 35, 36, 37, 38
)

/**
 * Binary encoding - just 0 and 1
 */
val binaryEncoding = intArrayOf(16, 17)

/**
 * DNA encoding - A, C, G, T
 */
val dnaEncoding = intArrayOf(33, 35, 39, 52)

/**
 * Character map - maps ASCII characters to glyph indices
 * From glmatrix.c char_map[256]
 */
val charMap = intArrayOf(
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96,  //   0
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96,  //  16
     0,  1,  2, 96,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14, 15,  //  32
    16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,  //  48
    32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,  //  64
    48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63,  //  80
    64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79,  //  96
    80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95,  // 112
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96,  // 128
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96,  // 144
    96, 97, 98, 99,100,101,102,103,104,105,106,107,108,109,110,111,  // 160
   112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,  // 176
   128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,  // 192
   144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,  // 208
    // see spank_image() - these are cleared
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96,  // 224
    96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96, 96   // 240
)

/**
 * Nice views - rotation angles that look good
 * From glmatrix.c nice_views[]
 */
data class ViewAngle(val x: Float, val y: Float)

val niceViews = arrayOf(
    ViewAngle(0f, 0f),
    ViewAngle(0f, -20f),     // this is a list of viewer rotations that look nice.
    ViewAngle(0f, 20f),      // every now and then we switch to a new one.
    ViewAngle(25f, 0f),      // (but we only use the first one at start-up.)
    ViewAngle(-25f, 0f),
    ViewAngle(25f, 20f),
    ViewAngle(-25f, 20f),
    ViewAngle(25f, -20f),
    ViewAngle(-25f, -20f),
    ViewAngle(10f, 0f),
    ViewAngle(-10f, 0f),
    ViewAngle(0f, 0f),       // prefer these
    ViewAngle(0f, 0f),
    ViewAngle(0f, 0f),
    ViewAngle(0f, 0f),
    ViewAngle(0f, 0f)
)

/**
 * Compute the brightness ramp.
 * From init_matrix() in glmatrix.c
 */
fun computeBrightnessRamp(): FloatArray {
    val ramp = FloatArray(WAVE_SIZE)
    for (i in 0 until WAVE_SIZE) {
        var j = ((WAVE_SIZE - i) / (WAVE_SIZE - 1).toFloat())
        j *= (PI / 2).toFloat()    // j ranges from 0.0 - PI/2
        j = sin(j.toDouble()).toFloat()  // j ranges from 0.0 - 1.0
        j = 0.2f + (j * 0.8f)      // j ranges from 0.2 - 1.0
        ramp[i] = j
    }
    return ramp
}

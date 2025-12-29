package com.matrix.background

/**
 * Strip data structure - represents a single falling column of glyphs
 * Direct conversion from glmatrix.c strip struct
 */
class Strip {
    // Position of strip
    var x: Float = 0f
    var y: Float = 0f
    var z: Float = 0f

    // Velocity of strip
    var dx: Float = 0f
    var dy: Float = 0f
    var dz: Float = 0f

    // Whether this strip is on its way out
    var erasingP: Boolean = false

    // The bottommost glyph -- the feeder
    var spinnerGlyph: Int = 0

    // Where on the strip the bottom glyph is
    var spinnerY: Float = 0f

    // How fast the bottom glyph drops
    var spinnerSpeed: Float = 0f

    // The other glyphs on the strip, which will be revealed by the dropping spinner.
    // 0 means no glyph; negative means "spinner".
    // If non-zero, real value is abs(G)-1.
    val glyphs: IntArray = IntArray(GRID_SIZE)

    // Some glyphs may be highlighted
    val highlight: BooleanArray = BooleanArray(GRID_SIZE)

    // Rotate all spinners every this-many frames
    var spinSpeed: Int = 0

    // Frame counter for spin
    var spinTick: Int = 0

    // Waves of brightness wash down the strip
    var wavePosition: Int = 0

    // Every this-many frames
    var waveSpeed: Int = 0

    // Frame counter for wave
    var waveTick: Int = 0
}

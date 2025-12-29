package com.matrix.background

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

/**
 * Process the texture exactly as glmatrix.c load_textures() does:
 * - Green channel becomes the alpha channel
 * - Green channel is then set to 0xFF (full intensity)
 *
 * From glmatrix.c:
 * ```c
 * for (y = 0; y < image->height; y++)
 *   for (x = 0; x < image->width; x++) {
 *     unsigned long pixel = XGetPixel(image, x, y);
 *     unsigned char r = (pixel & image->red_mask) >> 16;
 *     unsigned char g = (pixel & image->green_mask) >> 8;
 *     unsigned char b = (pixel & image->blue_mask);
 *     unsigned char a = ~0;
 *     a = g;      // green channel becomes alpha
 *     g = 0xFF;   // green set to full intensity
 *     ...
 *   }
 * ```
 *
 * This makes black pixels (g=0) fully transparent and green pixels
 * fully opaque with full green intensity, enabling proper additive blending.
 */
fun processMatrixTexture(source: ImageBitmap): ImageBitmap {
    val width = source.width
    val height = source.height

    // Read pixels from source ImageBitmap
    val sourcePixels = IntArray(width * height)
    source.readPixels(sourcePixels)

    // Process each pixel: alpha = green, green = 0xFF
    val processedPixels = IntArray(width * height)
    for (i in sourcePixels.indices) {
        val pixel = sourcePixels[i]

        // Compose ImageBitmap uses ARGB format (like Android)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF

        // From glmatrix.c: alpha = green channel value, green = 0xFF
        val newAlpha = g
        val newGreen = 0xFF

        // Reconstruct pixel with new values (ARGB format)
        processedPixels[i] = (newAlpha shl 24) or (r shl 16) or (newGreen shl 8) or b
    }

    // Create a Skia Bitmap and set the pixels
    val skiaBitmap = Bitmap()
    val imageInfo = ImageInfo(
        width = width,
        height = height,
        colorType = ColorType.BGRA_8888,  // Skia uses BGRA
        alphaType = ColorAlphaType.UNPREMUL
    )
    skiaBitmap.allocPixels(imageInfo)

    // Convert ARGB to BGRA byte array for Skia
    // Each pixel is 4 bytes: B, G, R, A (little-endian BGRA_8888)
    val bgraBytes = ByteArray(width * height * 4)
    for (i in processedPixels.indices) {
        val argb = processedPixels[i]
        val a = ((argb shr 24) and 0xFF).toByte()
        val r = ((argb shr 16) and 0xFF).toByte()
        val g = ((argb shr 8) and 0xFF).toByte()
        val b = (argb and 0xFF).toByte()
        // BGRA_8888 format: bytes are B, G, R, A
        val offset = i * 4
        bgraBytes[offset] = b
        bgraBytes[offset + 1] = g
        bgraBytes[offset + 2] = r
        bgraBytes[offset + 3] = a
    }

    skiaBitmap.installPixels(bgraBytes)

    // Convert to Compose ImageBitmap
    return skiaBitmap.asComposeImageBitmap()
}

/**
 * Extension to convert Skia Bitmap to Compose ImageBitmap
 */
private fun Bitmap.asComposeImageBitmap(): ImageBitmap {
    return org.jetbrains.skia.Image.makeFromBitmap(this).toComposeImageBitmap()
}

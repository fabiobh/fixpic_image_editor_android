package com.uaialternativa.imageeditor.data.processing.filters

import android.graphics.Bitmap
import com.uaialternativa.imageeditor.data.processing.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/**
 * Optimized blur filter using separable box blur algorithm
 * This implementation is significantly faster than the naive approach
 */
class BlurFilter : ImageFilter {
    
    override suspend fun apply(bitmap: Bitmap, intensity: Float): Bitmap = withContext(Dispatchers.Default) {
        val radius = (intensity * 12f).coerceIn(0f, 12f).toInt()
        if (radius == 0) return@withContext bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false)
        
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Apply optimized separable blur
        blurHorizontal(pixels, width, height, radius)
        blurVertical(pixels, width, height, radius)
        
        val resultBitmap = Bitmap.createBitmap(width, height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        
        resultBitmap
    }
    
    /**
     * Optimized horizontal blur using box filter
     */
    private fun blurHorizontal(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val tempPixels = IntArray(width * height)
        
        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // Calculate blur for this pixel
                for (dx in -radius..radius) {
                    val nx = (x + dx).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + nx]
                    
                    sumA += (pixel shr 24) and 0xFF
                    sumR += (pixel shr 16) and 0xFF
                    sumG += (pixel shr 8) and 0xFF
                    sumB += pixel and 0xFF
                    count++
                }
                
                tempPixels[y * width + x] = (sumA / count shl 24) or
                        (sumR / count shl 16) or
                        (sumG / count shl 8) or
                        (sumB / count)
            }
        }
        
        // Copy back to original array
        System.arraycopy(tempPixels, 0, pixels, 0, pixels.size)
    }
    
    /**
     * Optimized vertical blur using box filter
     */
    private fun blurVertical(pixels: IntArray, width: Int, height: Int, radius: Int) {
        val tempPixels = IntArray(width * height)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                var sumA = 0
                var sumR = 0
                var sumG = 0
                var sumB = 0
                var count = 0
                
                // Calculate blur for this pixel
                for (dy in -radius..radius) {
                    val ny = (y + dy).coerceIn(0, height - 1)
                    val pixel = pixels[ny * width + x]
                    
                    sumA += (pixel shr 24) and 0xFF
                    sumR += (pixel shr 16) and 0xFF
                    sumG += (pixel shr 8) and 0xFF
                    sumB += pixel and 0xFF
                    count++
                }
                
                tempPixels[y * width + x] = (sumA / count shl 24) or
                        (sumR / count shl 16) or
                        (sumG / count shl 8) or
                        (sumB / count)
            }
        }
        
        // Copy back to original array
        System.arraycopy(tempPixels, 0, pixels, 0, pixels.size)
    }
}
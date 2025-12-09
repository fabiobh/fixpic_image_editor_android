package com.uaialternativa.imageeditor.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import javax.inject.Inject

/**
 * Use case for loading images from URIs or file paths
 */
class LoadImageUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Execute the use case to load an image from a URI
     * @param uri The URI of the image to load
     * @param maxWidth Maximum width for the loaded image (optional)
     * @param maxHeight Maximum height for the loaded image (optional)
     * @return Result containing the loaded bitmap or error
     */
    suspend operator fun invoke(
        uri: Uri,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(FileNotFoundException("Cannot open input stream for URI: $uri"))
            
            inputStream.use { stream ->
                val options = BitmapFactory.Options()
                
                // If max dimensions are specified, calculate sample size
                if (maxWidth != null && maxHeight != null) {
                    // First decode with inJustDecodeBounds=true to check dimensions
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeStream(stream, null, options)
                    
                    // Calculate sample size
                    options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                    options.inJustDecodeBounds = false
                    
                    // Reopen stream for actual decoding
                    val newInputStream = context.contentResolver.openInputStream(uri)
                        ?: return@withContext Result.failure(FileNotFoundException("Cannot reopen input stream for URI: $uri"))
                    
                    newInputStream.use { newStream ->
                        val bitmap = BitmapFactory.decodeStream(newStream, null, options)
                        if (bitmap != null) {
                            // Apply EXIF orientation correction
                            val correctedBitmap = applyExifOrientation(bitmap, uri)
                            Result.success(correctedBitmap)
                        } else {
                            Result.failure(IOException("Failed to decode bitmap from URI: $uri"))
                        }
                    }
                } else {
                    val bitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (bitmap != null) {
                        // Apply EXIF orientation correction
                        val correctedBitmap = applyExifOrientation(bitmap, uri)
                        Result.success(correctedBitmap)
                    } else {
                        Result.failure(IOException("Failed to decode bitmap from URI: $uri"))
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            Result.failure(e)
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(IOException("Unexpected error loading image: ${e.message}", e))
        }
    }
    
    /**
     * Load an image from a file path
     * @param filePath The file path of the image to load
     * @param maxWidth Maximum width for the loaded image (optional)
     * @param maxHeight Maximum height for the loaded image (optional)
     * @return Result containing the loaded bitmap or error
     */
    suspend fun loadFromPath(
        filePath: String,
        maxWidth: Int? = null,
        maxHeight: Int? = null
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        try {
            val options = BitmapFactory.Options()
            
            // If max dimensions are specified, calculate sample size
            if (maxWidth != null && maxHeight != null) {
                // First decode with inJustDecodeBounds=true to check dimensions
                options.inJustDecodeBounds = true
                BitmapFactory.decodeFile(filePath, options)
                
                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
                options.inJustDecodeBounds = false
            }
            
            val bitmap = BitmapFactory.decodeFile(filePath, options)
            if (bitmap != null) {
                Result.success(bitmap)
            } else {
                Result.failure(IOException("Failed to decode bitmap from file: $filePath"))
            }
        } catch (e: Exception) {
            Result.failure(IOException("Error loading image from path: ${e.message}", e))
        }
    }
    
    /**
     * Calculate the sample size for efficient image loading
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Apply EXIF orientation correction to a bitmap
     */
    private fun applyExifOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            // Use reflection to avoid class loading issues in tests
            val exifClass = Class.forName("androidx.exifinterface.media.ExifInterface")
            val inputStream = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val exif = exifClass.getConstructor(java.io.InputStream::class.java).newInstance(stream)
                val tagOrientation = exifClass.getField("TAG_ORIENTATION").get(null) as String
                val orientationUndefined = exifClass.getField("ORIENTATION_UNDEFINED").getInt(null)
                val orientationNormal = exifClass.getField("ORIENTATION_NORMAL").getInt(null)
                val orientationFlipHorizontal = exifClass.getField("ORIENTATION_FLIP_HORIZONTAL").getInt(null)
                val orientationRotate180 = exifClass.getField("ORIENTATION_ROTATE_180").getInt(null)
                val orientationFlipVertical = exifClass.getField("ORIENTATION_FLIP_VERTICAL").getInt(null)
                val orientationTranspose = exifClass.getField("ORIENTATION_TRANSPOSE").getInt(null)
                val orientationRotate90 = exifClass.getField("ORIENTATION_ROTATE_90").getInt(null)
                val orientationTransverse = exifClass.getField("ORIENTATION_TRANSVERSE").getInt(null)
                val orientationRotate270 = exifClass.getField("ORIENTATION_ROTATE_270").getInt(null)
                
                val getAttributeIntMethod = exifClass.getMethod("getAttributeInt", String::class.java, Int::class.javaPrimitiveType)
                val orientation = getAttributeIntMethod.invoke(exif, tagOrientation, orientationUndefined) as Int
                
                when (orientation) {
                    orientationNormal -> bitmap
                    orientationFlipHorizontal -> {
                        val matrix = Matrix().apply { setScale(-1f, 1f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationRotate180 -> {
                        val matrix = Matrix().apply { setRotate(180f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationFlipVertical -> {
                        val matrix = Matrix().apply { setScale(1f, -1f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationTranspose -> {
                        val matrix = Matrix().apply { 
                            setRotate(90f)
                            postScale(-1f, 1f)
                        }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationRotate90 -> {
                        val matrix = Matrix().apply { setRotate(90f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationTransverse -> {
                        val matrix = Matrix().apply { 
                            setRotate(-90f)
                            postScale(-1f, 1f)
                        }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    orientationRotate270 -> {
                        val matrix = Matrix().apply { setRotate(-90f) }
                        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    }
                    else -> bitmap
                }
            } ?: bitmap
        } catch (e: ClassNotFoundException) {
            // ExifInterface not available (e.g., in tests), return original bitmap
            bitmap
        } catch (e: Exception) {
            // If EXIF reading fails, return original bitmap
            bitmap
        }
    }
}
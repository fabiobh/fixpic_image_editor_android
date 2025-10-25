package com.uaialternativa.imageeditor.ui.picker

import android.content.Context
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manager class for handling camera operations
 */
class CameraManager(
    private val context: Context
) {
    companion object {
        private const val CAMERA_IMAGES_DIR = "camera_images"
        private const val IMAGE_FILE_PREFIX = "IMG_"
        private const val IMAGE_FILE_EXTENSION = ".jpg"
    }

    /**
     * Create a temporary file for camera capture and return its URI
     */
    fun createImageFileUri(): Uri {
        val imageFile = createImageFile()
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    }

    /**
     * Launch the camera to take a photo
     */
    fun launchCamera(
        launcher: ManagedActivityResultLauncher<Uri, Boolean>,
        imageUri: Uri
    ) {
        launcher.launch(imageUri)
    }

    /**
     * Create a temporary image file for camera capture
     */
    private fun createImageFile(): File {
        // Create an image file name with timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "$IMAGE_FILE_PREFIX$timeStamp$IMAGE_FILE_EXTENSION"
        
        // Create the directory if it doesn't exist
        val storageDir = File(context.cacheDir, CAMERA_IMAGES_DIR)
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
        
        return File(storageDir, imageFileName)
    }

    /**
     * Clean up old camera images from cache
     */
    fun cleanupOldImages() {
        try {
            val storageDir = File(context.cacheDir, CAMERA_IMAGES_DIR)
            if (storageDir.exists()) {
                val files = storageDir.listFiles()
                files?.forEach { file ->
                    // Delete files older than 1 hour
                    val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000)
                    if (file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore cleanup errors
        }
    }
}
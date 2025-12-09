package com.uaialternativa.imageeditor.ui.picker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * Handler for managing image picker and camera permissions
 */
class PermissionHandler(private val context: Context) {
    
    companion object {
        /**
         * Get the required permission for image picker based on Android version
         */
        fun getRequiredImagePickerPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
        
        /**
         * Get the required permission for camera
         */
        const val CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
    
    /**
     * Check if the required image picker permission is granted
     */
    fun hasImagePickerPermission(): Boolean {
        val permission = getRequiredImagePickerPermission()
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Request the required image picker permission
     */
    fun requestImagePickerPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        val permission = getRequiredImagePickerPermission()
        launcher.launch(permission)
    }
    
    /**
     * Request camera permission
     */
    fun requestCameraPermission(launcher: ManagedActivityResultLauncher<String, Boolean>) {
        launcher.launch(CAMERA_PERMISSION)
    }
    
    /**
     * Check if we should show rationale for image picker permission
     */
    fun shouldShowImagePickerPermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        val permission = getRequiredImagePickerPermission()
        return activity.shouldShowRequestPermissionRationale(permission)
    }
    
    /**
     * Check if we should show rationale for camera permission
     */
    fun shouldShowCameraPermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        return activity.shouldShowRequestPermissionRationale(CAMERA_PERMISSION)
    }
    
    /**
     * Check if we should show rationale for the permission (legacy method for backward compatibility)
     */
    fun shouldShowPermissionRationale(activity: androidx.activity.ComponentActivity): Boolean {
        return shouldShowImagePickerPermissionRationale(activity)
    }
}
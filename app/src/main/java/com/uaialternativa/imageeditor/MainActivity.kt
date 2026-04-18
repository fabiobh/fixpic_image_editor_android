package com.uaialternativa.imageeditor

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.uaialternativa.imageeditor.R
import com.uaialternativa.imageeditor.domain.model.SavedImage
import com.uaialternativa.imageeditor.ui.editor.ImageEditorScreen
import com.uaialternativa.imageeditor.ui.editor.ImageEditorViewModel
import com.uaialternativa.imageeditor.ui.editor.ImageEditorAction
import com.uaialternativa.imageeditor.ui.gallery.GalleryScreen
import com.uaialternativa.imageeditor.ui.picker.ImagePickerManager
import com.uaialternativa.imageeditor.ui.picker.ImagePickerResult
import com.uaialternativa.imageeditor.ui.picker.PermissionHandler
import com.uaialternativa.imageeditor.ui.picker.CameraManager
import com.uaialternativa.imageeditor.ui.gallery.ImageSourceDialog
import com.uaialternativa.imageeditor.ui.settings.SettingsScreen
import com.uaialternativa.imageeditor.ui.theme.ImageEditorTheme
import android.content.Context
import android.content.res.Configuration
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.uaialternativa.imageeditor.ui.common.AnalyticsManager
import com.uaialternativa.imageeditor.ui.common.LocalAnalytics
import com.uaialternativa.imageeditor.data.preferences.ReviewPreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var analyticsManager: AnalyticsManager
    
    @Inject
    lateinit var reviewPreferenceManager: ReviewPreferenceManager
    
    companion object {
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val THEME_SYSTEM = "system"
        private const val THEME_LIGHT = "light"
        private const val THEME_DARK = "dark"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Increment app open count for Smart Review
        reviewPreferenceManager.incrementAppOpenCount()
        
        setContent {
            val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // Check if this is the first launch
            val isFirstLaunch = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
            if (isFirstLaunch) {
                // On first launch, set system theme as default and mark as not first launch
                prefs.edit()
                    .putString(KEY_THEME_MODE, THEME_SYSTEM)
                    .putBoolean(KEY_FIRST_LAUNCH, false)
                    .apply()
            }
            
            var themeMode by remember { 
                mutableStateOf(prefs.getString(KEY_THEME_MODE, THEME_SYSTEM) ?: THEME_SYSTEM) 
            }
            
            val darkTheme = when (themeMode) {
                THEME_LIGHT -> false
                THEME_DARK -> true
                else -> isSystemInDarkMode()
            }
            
            ImageEditorTheme(darkTheme = darkTheme) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAnalytics provides analyticsManager
                ) {
                    ImageEditorApp(
                        onLanguageChanged = ::changeLanguage,
                        onThemeChanged = { newThemeMode ->
                            themeMode = newThemeMode
                            prefs.edit().putString(KEY_THEME_MODE, newThemeMode).apply()
                        },
                        currentThemeMode = themeMode,
                        currentIsDarkTheme = darkTheme
                    )
                }
            }
        }
    }
    
    private fun isSystemInDarkMode(): Boolean {
        return try {
            val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            uiMode == Configuration.UI_MODE_NIGHT_YES
        } catch (e: Exception) {
            // Fallback to Compose's detection if there's any issue
            false
        }
    }
    
    private fun changeLanguage(languageCode: String) {
        // Create locale from language code
        val locale = when (languageCode) {
            "pt-BR" -> Locale("pt", "BR")
            "en" -> Locale("en")
            else -> Locale("en")
        }
        
        // Apply the new locale
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        
        // Recreate the activity to apply the new locale
        recreate()
    }
}

@Composable
fun ImageEditorApp(
    onLanguageChanged: (String) -> Unit = {},
    onThemeChanged: (String) -> Unit = {},
    currentThemeMode: String = "system",
    currentIsDarkTheme: Boolean = false
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Gallery) }
    var isImagePickerLoading by remember { mutableStateOf(false) }
    var imagePickerError by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }
    var showCameraPermissionDeniedDialog by remember { mutableStateOf(false) }
    var currentCameraImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val imagePickerManager = remember { ImagePickerManager(context) }
    val cameraManager = remember { CameraManager(context) }
    val permissionHandler = remember { PermissionHandler(context) }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isImagePickerLoading = true
            imagePickerError = null
            
            coroutineScope.launch {
                when (val result = imagePickerManager.validateAndProcessImage(uri)) {
                    is ImagePickerResult.Success -> {
                        isImagePickerLoading = false
                        currentScreen = Screen.Editor(uri, result.fileName)
                    }
                    is ImagePickerResult.Error -> {
                        isImagePickerLoading = false
                        imagePickerError = result.message
                    }
                    is ImagePickerResult.Cancelled -> {
                        isImagePickerLoading = false
                        // User cancelled, no action needed
                    }
                }
            }
        } else {
            // User cancelled selection
            isImagePickerLoading = false
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && currentCameraImageUri != null) {
            isImagePickerLoading = true
            imagePickerError = null
            
            coroutineScope.launch {
                when (val result = imagePickerManager.validateAndProcessImage(currentCameraImageUri!!)) {
                    is ImagePickerResult.Success -> {
                        isImagePickerLoading = false
                        currentScreen = Screen.Editor(currentCameraImageUri!!, result.fileName)
                    }
                    is ImagePickerResult.Error -> {
                        isImagePickerLoading = false
                        imagePickerError = result.message
                    }
                    is ImagePickerResult.Cancelled -> {
                        isImagePickerLoading = false
                    }
                }
                currentCameraImageUri = null
            }
        } else {
            // Camera cancelled or failed
            isImagePickerLoading = false
            currentCameraImageUri = null
        }
    }
    
    // Permission launcher for image picker
    val imagePickerPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch image picker
            imagePickerManager.launchImagePicker(imagePickerLauncher)
        } else {
            // Permission denied
            showPermissionDeniedDialog = true
        }
    }
    
    // Function to handle camera launch with permission check
    val launchCamera = {
        try {
            currentCameraImageUri = cameraManager.createImageFileUri()
            cameraManager.launchCamera(cameraLauncher, currentCameraImageUri!!)
            // Clean up old images
            cameraManager.cleanupOldImages()
        } catch (e: Exception) {
            imagePickerError = "Failed to launch camera: ${e.message}"
            currentCameraImageUri = null
        }
    }
    
    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, launch camera
            launchCamera()
        } else {
            // Permission denied
            showCameraPermissionDeniedDialog = true
        }
    }
    
    // Function to handle camera with permission check
    val handleCameraAction = {
        if (permissionHandler.hasCameraPermission()) {
            launchCamera()
        } else {
            if (context is ComponentActivity && permissionHandler.shouldShowCameraPermissionRationale(context)) {
                showCameraPermissionDialog = true
            } else {
                permissionHandler.requestCameraPermission(cameraPermissionLauncher)
            }
        }
    }
    
    // Function to handle image picker launch with permission check
    val launchImagePicker = {
        if (permissionHandler.hasImagePickerPermission()) {
            imagePickerManager.launchImagePicker(imagePickerLauncher)
        } else {
            if (context is ComponentActivity && permissionHandler.shouldShowImagePickerPermissionRationale(context)) {
                showPermissionDialog = true
            } else {
                permissionHandler.requestImagePickerPermission(imagePickerPermissionLauncher)
            }
        }
    }
    
    // Function to show image source dialog
    val showImageSourceSelection = {
        showImageSourceDialog = true
    }

    when (val screen = currentScreen) {
        is Screen.Gallery -> {
            GalleryScreen(
                onImageSelected = { savedImage ->
                    currentScreen = Screen.EditorFromSaved(savedImage)
                },
                onAddImageClicked = showImageSourceSelection,
                onSettingsClicked = { currentScreen = Screen.Settings },
                isImagePickerLoading = isImagePickerLoading,
                imagePickerError = imagePickerError,
                onImagePickerErrorDismissed = { imagePickerError = null },
                modifier = Modifier.fillMaxSize()
            )
        }
        is Screen.Editor -> {
            val editorViewModel: ImageEditorViewModel = hiltViewModel(key = "editor_${screen.imageUri}_${screen.fileName}")
            
            // Reset editor state and load the image when entering editor screen
            androidx.compose.runtime.LaunchedEffect(screen.imageUri) {
                editorViewModel.handleAction(ImageEditorAction.ResetEditor)
                editorViewModel.loadImage(screen.imageUri, screen.fileName)
            }
            
            ImageEditorScreen(
                onNavigateBack = { 
                    // Clear the editor state when navigating back
                    editorViewModel.handleAction(ImageEditorAction.ResetEditor)
                    currentScreen = Screen.Gallery 
                },
                modifier = Modifier.fillMaxSize(),
                viewModel = editorViewModel
            )
        }
        is Screen.EditorFromSaved -> {
            val editorViewModel: ImageEditorViewModel = hiltViewModel(key = "editor_saved_${screen.savedImage.id}")
            
            // Reset editor state and load the saved image when entering editor screen
            androidx.compose.runtime.LaunchedEffect(screen.savedImage.id) {
                editorViewModel.handleAction(ImageEditorAction.ResetEditor)
                val imageUri = Uri.fromFile(java.io.File(screen.savedImage.filePath))
                editorViewModel.loadImage(imageUri, screen.savedImage.originalFileName ?: screen.savedImage.fileName)
            }
            
            ImageEditorScreen(
                onNavigateBack = { 
                    // Clear the editor state when navigating back
                    editorViewModel.handleAction(ImageEditorAction.ResetEditor)
                    currentScreen = Screen.Gallery 
                },
                modifier = Modifier.fillMaxSize(),
                viewModel = editorViewModel
            )
        }
        is Screen.Settings -> {
            SettingsScreen(
                onNavigateBack = { currentScreen = Screen.Gallery },
                onLanguageSelected = onLanguageChanged,
                onThemeSelected = onThemeChanged,
                currentThemeMode = currentThemeMode,
                currentIsDarkTheme = currentIsDarkTheme,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
    
    // Permission rationale dialog
    if (showPermissionDialog) {
        PermissionRationaleDialog(
            onGrantPermission = {
                showPermissionDialog = false
                permissionHandler.requestImagePickerPermission(imagePickerPermissionLauncher)
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    }
    
    // Permission denied dialog
    if (showPermissionDeniedDialog) {
        PermissionDeniedDialog(
            onDismiss = {
                showPermissionDeniedDialog = false
            }
        )
    }
    
    // Image source selection dialog
    if (showImageSourceDialog) {
        ImageSourceDialog(
            onDismiss = {
                showImageSourceDialog = false
            },
            onTakePhoto = handleCameraAction,
            onChooseFromGallery = launchImagePicker
        )
    }
    
    // Camera permission rationale dialog
    if (showCameraPermissionDialog) {
        CameraPermissionRationaleDialog(
            onGrantPermission = {
                showCameraPermissionDialog = false
                permissionHandler.requestCameraPermission(cameraPermissionLauncher)
            },
            onDismiss = {
                showCameraPermissionDialog = false
            }
        )
    }
    
    // Camera permission denied dialog
    if (showCameraPermissionDeniedDialog) {
        CameraPermissionDeniedDialog(
            onDismiss = {
                showCameraPermissionDeniedDialog = false
            }
        )
    }
}

/**
 * Dialog shown to explain why permission is needed
 */
@Composable
private fun PermissionRationaleDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_required_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.permission_required_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog shown when permission is denied
 */
@Composable
private fun PermissionDeniedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.permission_denied_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.permission_denied_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * Dialog shown to explain why camera permission is needed
 */
@Composable
private fun CameraPermissionRationaleDialog(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.camera_permission_required_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.camera_permission_required_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onGrantPermission) {
                Text(stringResource(R.string.grant_permission))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Dialog shown when camera permission is denied
 */
@Composable
private fun CameraPermissionDeniedDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.camera_permission_denied_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.camera_permission_denied_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.ok))
            }
        }
    )
}

/**
 * Sealed class representing different screens in the app
 */
sealed class Screen {
    object Gallery : Screen()
    object Settings : Screen()
    data class Editor(val imageUri: Uri, val fileName: String?) : Screen()
    data class EditorFromSaved(val savedImage: SavedImage) : Screen()
}

@Preview(showBackground = true)
@Composable
fun ImageEditorAppPreview() {
    ImageEditorTheme {
        ImageEditorApp()
    }
}
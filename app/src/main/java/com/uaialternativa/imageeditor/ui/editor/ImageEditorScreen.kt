package com.uaialternativa.imageeditor.ui.editor

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.uaialternativa.imageeditor.R
import com.uaialternativa.imageeditor.domain.model.EditingTool
import com.uaialternativa.imageeditor.ui.common.LocalAnalytics
import com.uaialternativa.imageeditor.ui.common.SmartReviewDialog
import com.uaialternativa.imageeditor.ui.editor.crop.CropOverlay
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding

/**
 * Main image editor screen with toolbar and tool-specific controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showUnsavedChangesDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val analytics = LocalAnalytics.current

    // Handle back navigation with unsaved changes check
    BackHandler {
        analytics.logButtonClick("back_button_hardware", "Editor")
        if (viewModel.hasUnsavedChanges()) {
            showUnsavedChangesDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Show error messages in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.handleAction(ImageEditorAction.ClearError)
        }
    }

    // Show save success message and show review dialog
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.image_saved_successfully))
            viewModel.handleAction(ImageEditorAction.ClearSaveSuccess)
            if (uiState.shouldShowReview) {
                showReviewDialog = true
            }
        }
    }

    if (showReviewDialog) {
        SmartReviewDialog(
            onDismiss = {
                showReviewDialog = false
                onNavigateBack()
            }
        )
    }

    Scaffold(
        topBar = {
            ImageEditorTopBar(
                onNavigateBack = {
                    if (viewModel.hasUnsavedChanges()) {
                        showUnsavedChangesDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                canUndo = uiState.canUndo,
                canRedo = uiState.canRedo,
                onUndo = { viewModel.handleAction(ImageEditorAction.Undo) },
                onRedo = { viewModel.handleAction(ImageEditorAction.Redo) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            // Main image display area
            ImageDisplayArea(
                bitmap = uiState.previewImage ?: uiState.editedImage,
                isLoading = uiState.isLoading,
                isProcessing = uiState.isProcessing,
                selectedTool = uiState.selectedTool,
                cropBounds = uiState.cropBounds,
                onCropBoundsChanged = { bounds ->
                    viewModel.handleAction(ImageEditorAction.SetCropBounds(bounds))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            )

            // Tool-specific control panel (when a tool is selected)
            if (uiState.selectedTool != EditingTool.None) {
                ToolControlPanel(
                    selectedTool = uiState.selectedTool,
                    uiState = uiState,
                    onAction = viewModel::handleAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }

            // Toolbar with editing tools
            EditorToolbar(
                selectedTool = uiState.selectedTool,
                onToolSelected = { tool ->
                    viewModel.handleAction(ImageEditorAction.SelectTool(tool))
                },
                onSave = { viewModel.handleAction(ImageEditorAction.SaveImage) },
                isSaving = uiState.isSaving,
                hasImage = uiState.editedImage != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }

    // Unsaved changes dialog
    if (showUnsavedChangesDialog) {
        UnsavedChangesDialog(
            onSaveAndExit = {
                showUnsavedChangesDialog = false
                viewModel.handleAction(ImageEditorAction.SaveImage)
            },
            onDiscardAndExit = {
                showUnsavedChangesDialog = false
                onNavigateBack()
            },
            onCancel = {
                showUnsavedChangesDialog = false
            }
        )
    }
}

/**
 * Top app bar with navigation and undo/redo controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageEditorTopBar(
    onNavigateBack: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.image_editor_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    analytics.logButtonClick("editor_back", "Editor")
                    onNavigateBack()
                },
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.navigate_back)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back)
                )
            }
        },
        actions = {
            IconButton(
                onClick = {
                    analytics.logButtonClick("editor_undo", "Editor")
                    onUndo()
                },
                enabled = canUndo,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.undo_action)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.undo_action),
                    tint = if (canUndo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
            
            IconButton(
                onClick = {
                    analytics.logButtonClick("editor_redo", "Editor")
                    onRedo()
                },
                enabled = canRedo,
                modifier = Modifier.semantics {
                    contentDescription = context.getString(R.string.redo_action)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.redo_action),
                    tint = if (canRedo) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

/**
 * Main image display area with loading and processing states
 */
@Composable
private fun ImageDisplayArea(
    bitmap: Bitmap?,
    isLoading: Boolean,
    isProcessing: Boolean,
    selectedTool: EditingTool,
    cropBounds: android.graphics.Rect?,
    onCropBoundsChanged: (android.graphics.Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    LoadingImageState()
                }
                bitmap != null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(bitmap)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Image being edited",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                        
                        // Crop overlay (only show when crop tool is selected)
                        if (selectedTool == EditingTool.Crop) {
                            CropOverlay(
                                imageSize = IntSize(bitmap.width, bitmap.height),
                                cropBounds = cropBounds,
                                onCropBoundsChanged = onCropBoundsChanged,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    
                    // Processing overlay
                    if (isProcessing) {
                        ProcessingOverlay()
                    }
                }
                else -> {
                    EmptyImageState()
                }
            }
        }
    }
}

/**
 * Loading state for image display area
 */
@Composable
private fun LoadingImageState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "Loading image"
                }
        )
        Text(
            text = stringResource(R.string.loading_image),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Processing overlay shown during image operations
 */
@Composable
private fun ProcessingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color.Black.copy(alpha = 0.6f),
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Processing image"
                    }
            )
            Text(
                text = stringResource(R.string.processing_image),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Empty state when no image is loaded
 */
@Composable
private fun EmptyImageState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Text(
            text = stringResource(R.string.no_image_loaded),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.select_image_to_edit),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Toolbar with editing tools and save button
 */
@Composable
private fun EditorToolbar(
    selectedTool: EditingTool,
    onToolSelected: (EditingTool) -> Unit,
    onSave: () -> Unit,
    isSaving: Boolean,
    hasImage: Boolean,
    modifier: Modifier = Modifier
) {
    val analytics = LocalAnalytics.current
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Crop tool
            ToolButton(
                icon = Icons.Default.Edit,
                label = stringResource(R.string.tool_crop),
                isSelected = selectedTool == EditingTool.Crop,
                enabled = hasImage,
                onClick = { 
                    analytics.logButtonClick("tool_crop", "Editor")
                    onToolSelected(
                        if (selectedTool == EditingTool.Crop) EditingTool.None 
                        else EditingTool.Crop
                    )
                }
            )
            
            // Resize tool
            ToolButton(
                icon = Icons.Default.Build,
                label = stringResource(R.string.tool_resize),
                isSelected = selectedTool == EditingTool.Resize,
                enabled = hasImage,
                onClick = { 
                    analytics.logButtonClick("tool_resize", "Editor")
                    onToolSelected(
                        if (selectedTool == EditingTool.Resize) EditingTool.None 
                        else EditingTool.Resize
                    )
                }
            )
            
            // Filter tool
            ToolButton(
                icon = Icons.Default.Settings,
                label = stringResource(R.string.tool_filter),
                isSelected = selectedTool == EditingTool.Filter,
                enabled = hasImage,
                onClick = { 
                    analytics.logButtonClick("tool_filter", "Editor")
                    onToolSelected(
                        if (selectedTool == EditingTool.Filter) EditingTool.None 
                        else EditingTool.Filter
                    )
                }
            )
            
            // Save button
            SaveButton(
                onClick = {
                    analytics.logButtonClick("editor_save", "Editor")
                    onSave()
                },
                isSaving = isSaving,
                enabled = hasImage
            )
        }
    }
}

/**
 * Individual tool button in the toolbar
 */
@Composable
private fun ToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        FilledTonalButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .size(64.dp)
                .semantics {
                    contentDescription = "$label tool"
                }
                .background(
                    color = if (isSelected && enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected && enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    },
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = if (isSelected && enabled) {
                    MaterialTheme.colorScheme.primary
                } else if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Save button with loading state
 */
@Composable
private fun SaveButton(
    onClick: () -> Unit,
    isSaving: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isSaving,
            modifier = Modifier
                .size(64.dp)
                .semantics {
                    contentDescription = "Save image"
                },
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = MaterialTheme.colorScheme.onSecondary,
                    strokeWidth = 3.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Text(
            text = if (isSaving) stringResource(R.string.saving_image) else stringResource(R.string.save_image),
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Bottom sheet panel with tool-specific controls
 */
@Composable
private fun ToolControlPanel(
    selectedTool: EditingTool,
    uiState: ImageEditorUiState,
    onAction: (ImageEditorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        when (selectedTool) {
            EditingTool.Crop -> {
                CropControlPanel(
                    currentWidth = uiState.editedImage?.width,
                    currentHeight = uiState.editedImage?.height,
                    cropBounds = uiState.cropBounds,
                    onCropBoundsChanged = { bounds ->
                        onAction(ImageEditorAction.SetCropBounds(bounds))
                    },
                    onApplyCrop = { onAction(ImageEditorAction.ApplyCrop) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) }
                )
            }
            EditingTool.Resize -> {
                ResizeControlPanel(
                    currentWidth = uiState.editedImage?.width,
                    currentHeight = uiState.editedImage?.height,
                    resizeWidth = uiState.resizeWidth,
                    resizeHeight = uiState.resizeHeight,
                    onDimensionsChanged = { width, height ->
                        onAction(ImageEditorAction.SetResizeDimensions(width, height))
                    },
                    onApplyResize = { onAction(ImageEditorAction.ApplyResize) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) }
                )
            }
            EditingTool.Filter -> {
                FilterControlPanel(
                    selectedFilter = uiState.selectedFilter,
                    filterIntensity = uiState.filterIntensity,
                    appliedFilters = uiState.appliedFilters,
                    onFilterSelected = { filter ->
                        onAction(ImageEditorAction.SelectFilter(filter))
                    },
                    onIntensityChanged = { intensity ->
                        onAction(ImageEditorAction.SetFilterIntensity(intensity))
                    },
                    onApplyFilter = { onAction(ImageEditorAction.ApplyFilter) },
                    onRemoveFilter = { filterId -> onAction(ImageEditorAction.RemoveFilter(filterId)) },
                    onClearAllFilters = { onAction(ImageEditorAction.ClearAllFilters) },
                    onCancel = { onAction(ImageEditorAction.SelectTool(EditingTool.None)) },
                    isFilterApplied = uiState.isFilterApplied
                )
            }
            EditingTool.None -> {
                // Empty panel when no tool is selected
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
    }
}

/**
 * Control panel for crop tool
 */
@Composable
private fun CropControlPanel(
    currentWidth: Int?,
    currentHeight: Int?,
    cropBounds: android.graphics.Rect?,
    onCropBoundsChanged: (android.graphics.Rect) -> Unit,
    onApplyCrop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    CropPanel(
        currentWidth = currentWidth,
        currentHeight = currentHeight,
        cropBounds = cropBounds,
        onCropBoundsChanged = onCropBoundsChanged,
        onApplyCrop = onApplyCrop,
        onCancel = onCancel,
        modifier = modifier
    )
}

/**
 * Control panel for resize tool
 */
@Composable
private fun ResizeControlPanel(
    currentWidth: Int?,
    currentHeight: Int?,
    resizeWidth: Int?,
    resizeHeight: Int?,
    onDimensionsChanged: (Int, Int) -> Unit,
    onApplyResize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    ResizePanel(
        currentWidth = currentWidth,
        currentHeight = currentHeight,
        resizeWidth = resizeWidth,
        resizeHeight = resizeHeight,
        onDimensionsChanged = onDimensionsChanged,
        onApplyResize = onApplyResize,
        onCancel = onCancel,
        modifier = modifier
    )
}

/**
 * Control panel for filter tool
 */
@Composable
private fun FilterControlPanel(
    selectedFilter: com.uaialternativa.imageeditor.domain.model.FilterType?,
    filterIntensity: Float,
    appliedFilters: List<AppliedFilter>,
    onFilterSelected: (com.uaialternativa.imageeditor.domain.model.FilterType) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onApplyFilter: () -> Unit,
    onRemoveFilter: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onCancel: () -> Unit,
    isFilterApplied: Boolean,
    modifier: Modifier = Modifier
) {
    FilterPanel(
        selectedFilter = selectedFilter,
        filterIntensity = filterIntensity,
        appliedFilters = appliedFilters,
        onFilterSelected = onFilterSelected,
        onIntensityChanged = onIntensityChanged,
        onApplyFilter = onApplyFilter,
        onRemoveFilter = onRemoveFilter,
        onClearAllFilters = onClearAllFilters,
        onCancel = onCancel,
        isFilterApplied = isFilterApplied,
        modifier = modifier
    )
}

/**
 * Dialog shown when user tries to navigate back with unsaved changes
 */
@Composable
private fun UnsavedChangesDialog(
    onSaveAndExit: () -> Unit,
    onDiscardAndExit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics = LocalAnalytics.current
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = stringResource(R.string.unsaved_changes_title),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.unsaved_changes_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = {
                analytics.logButtonClick("unsaved_save", "Editor")
                onSaveAndExit()
            }) {
                Text(stringResource(R.string.save_and_exit))
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    analytics.logButtonClick("unsaved_discard", "Editor")
                    onDiscardAndExit()
                }) {
                    Text(stringResource(R.string.discard_changes))
                }
                TextButton(onClick = {
                    analytics.logButtonClick("unsaved_cancel", "Editor")
                    onCancel()
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Resize panel with width/height input fields and aspect ratio lock
 */
@Composable
internal fun ResizePanel(
    currentWidth: Int?,
    currentHeight: Int?,
    resizeWidth: Int?,
    resizeHeight: Int?,
    onDimensionsChanged: (Int, Int) -> Unit,
    onApplyResize: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val analytics = LocalAnalytics.current
    var widthText by remember(resizeWidth) { 
        mutableStateOf(resizeWidth?.toString() ?: currentWidth?.toString() ?: "") 
    }
    var heightText by remember(resizeHeight) { 
        mutableStateOf(resizeHeight?.toString() ?: currentHeight?.toString() ?: "") 
    }
    var aspectRatioLocked by remember { mutableStateOf(true) }
    var widthError by remember { mutableStateOf<String?>(null) }
    var heightError by remember { mutableStateOf<String?>(null) }
    
    val keyboardController = LocalSoftwareKeyboardController.current
    
    // Calculate aspect ratio from current dimensions
    val aspectRatio = remember(currentWidth, currentHeight) {
        if (currentWidth != null && currentHeight != null && currentHeight != 0) {
            currentWidth.toFloat() / currentHeight.toFloat()
        } else null
    }
    
    val context = LocalContext.current
    
    // Validation functions
    fun validateWidth(text: String): String? {
        return when {
            text.isBlank() -> context.getString(R.string.width_required)
            text.toIntOrNull() == null -> context.getString(R.string.invalid_number)
            text.toInt() <= 0 -> context.getString(R.string.width_must_be_positive)
            text.toInt() > 4096 -> context.getString(R.string.width_too_large)
            else -> null
        }
    }
    
    fun validateHeight(text: String): String? {
        return when {
            text.isBlank() -> context.getString(R.string.height_required)
            text.toIntOrNull() == null -> context.getString(R.string.invalid_number)
            text.toInt() <= 0 -> context.getString(R.string.height_must_be_positive)
            text.toInt() > 4096 -> context.getString(R.string.height_too_large)
            else -> null
        }
    }
    
    // Update dimensions when text changes
    fun updateDimensions() {
        val width = widthText.toIntOrNull()
        val height = heightText.toIntOrNull()
        
        widthError = validateWidth(widthText)
        heightError = validateHeight(heightText)
        
        if (width != null && height != null && widthError == null && heightError == null) {
            onDimensionsChanged(width, height)
        }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.resize_tool_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        if (currentWidth != null && currentHeight != null) {
            Text(
                text = stringResource(R.string.current_size, currentWidth, currentHeight),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Text(
            text = stringResource(R.string.resize_tool_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Aspect ratio lock toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(
                checked = aspectRatioLocked,
                onCheckedChange = { aspectRatioLocked = it }
            )
            Icon(
                imageVector = if (aspectRatioLocked) Icons.Default.Lock else Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (aspectRatioLocked) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = stringResource(R.string.lock_aspect_ratio),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Dimension input fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Width input
            OutlinedTextField(
                value = widthText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        widthText = newValue
                        
                        // Auto-adjust height if aspect ratio is locked
                        if (aspectRatioLocked && aspectRatio != null && newValue.isNotEmpty()) {
                            val width = newValue.toIntOrNull()
                            if (width != null && width > 0) {
                                val newHeight = (width / aspectRatio).roundToInt()
                                heightText = newHeight.toString()
                            }
                        }
                        
                        updateDimensions()
                    }
                },
                label = { Text(stringResource(R.string.width_label)) },
                suffix = { Text(stringResource(R.string.pixels_unit)) },
                isError = widthError != null,
                supportingText = widthError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Image width in pixels"
                    }
            )
            
            // Height input
            OutlinedTextField(
                value = heightText,
                onValueChange = { newValue ->
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        heightText = newValue
                        
                        // Auto-adjust width if aspect ratio is locked
                        if (aspectRatioLocked && aspectRatio != null && newValue.isNotEmpty()) {
                            val height = newValue.toIntOrNull()
                            if (height != null && height > 0) {
                                val newWidth = (height * aspectRatio).roundToInt()
                                widthText = newWidth.toString()
                            }
                        }
                        
                        updateDimensions()
                    }
                },
                label = { Text(stringResource(R.string.height_label)) },
                suffix = { Text(stringResource(R.string.pixels_unit)) },
                isError = heightError != null,
                supportingText = heightError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { keyboardController?.hide() }
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .semantics {
                        contentDescription = "Image height in pixels"
                    }
            )
        }
        
        // Preview information
        val previewWidth = widthText.toIntOrNull()
        val previewHeight = heightText.toIntOrNull()
        if (previewWidth != null && previewHeight != null && 
            widthError == null && heightError == null &&
            (previewWidth != currentWidth || previewHeight != currentHeight)) {
            
            val scaleFactorWidth = if (currentWidth != null) previewWidth.toFloat() / currentWidth else 1f
            val scaleFactorHeight = if (currentHeight != null) previewHeight.toFloat() / currentHeight else 1f
            val scalePercent = ((scaleFactorWidth + scaleFactorHeight) / 2 * 100).roundToInt()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.preview_size, previewWidth, previewHeight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.scale_percentage, scalePercent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    analytics.logButtonClick("resize_cancel", "Editor")
                    keyboardController?.hide()
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    analytics.logButtonClick("resize_apply", "Editor")
                    keyboardController?.hide()
                    onApplyResize()
                },
                enabled = resizeWidth != null && resizeHeight != null && 
                         widthError == null && heightError == null &&
                         (resizeWidth != currentWidth || resizeHeight != currentHeight),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

/**
 * Filter panel with horizontal list of filter previews and intensity slider
 */
@Composable
private fun FilterPanel(
    selectedFilter: com.uaialternativa.imageeditor.domain.model.FilterType?,
    filterIntensity: Float,
    appliedFilters: List<AppliedFilter>,
    onFilterSelected: (com.uaialternativa.imageeditor.domain.model.FilterType) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onApplyFilter: () -> Unit,
    onRemoveFilter: (String) -> Unit,
    onClearAllFilters: () -> Unit,
    onCancel: () -> Unit,
    isFilterApplied: Boolean = false,
    modifier: Modifier = Modifier
) {
    val analytics = LocalAnalytics.current
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with title and action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_tool_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            analytics.logButtonClick("filter_cancel", "Editor")
                            onCancel()
                        },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel", style = MaterialTheme.typography.labelMedium)
                    }
                    
                    Button(
                        onClick = {
                            analytics.logButtonClick("filter_apply", "Editor")
                            onApplyFilter()
                        },
                        enabled = selectedFilter != null,
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFilterApplied) Icons.Default.Done else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isFilterApplied) "Done" else "Apply", 
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
            
            // Filter selection row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(com.uaialternativa.imageeditor.domain.model.FilterType.values()) { filterType ->
                    CompactFilterCard(
                        filterType = filterType,
                        isSelected = selectedFilter == filterType,
                        onClick = { 
                            analytics.logButtonClick("filter_select_${filterType.name.lowercase()}", "Editor")
                            onFilterSelected(filterType) 
                        }
                    )
                }
            }
            
            // Intensity slider (only show when a filter is selected)
            if (selectedFilter != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Intensity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.width(60.dp)
                    )
                    
                    Slider(
                        value = filterIntensity,
                        onValueChange = onIntensityChanged,
                        valueRange = 0f..1f,
                        steps = 19,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "${(filterIntensity * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(40.dp)
                    )
                }
            }
            
            // Applied filters section (show if any filters are applied)
            if (appliedFilters.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Applied (${appliedFilters.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (appliedFilters.size > 1) {
                        TextButton(
                            onClick = {
                                analytics.logButtonClick("filter_clear_all", "Editor")
                                onClearAllFilters()
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Clear All",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(appliedFilters) { appliedFilter ->
                        CompactAppliedFilterChip(
                            appliedFilter = appliedFilter,
                            onRemove = { 
                                analytics.logButtonClick("filter_remove_${appliedFilter.filterType.name.lowercase()}", "Editor")
                                onRemoveFilter(appliedFilter.id) 
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Compact filter card for horizontal layout
 */
@Composable
private fun CompactFilterCard(
    filterType: com.uaialternativa.imageeditor.domain.model.FilterType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter color indicator
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = getFilterPreviewColor(filterType),
                        shape = RoundedCornerShape(8.dp)
                    )
            )
            
            Text(
                text = getFilterDisplayName(filterType),
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Compact chip showing an applied filter with remove option
 */
@Composable
private fun CompactAppliedFilterChip(
    appliedFilter: AppliedFilter,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Filter color indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        color = getFilterPreviewColor(appliedFilter.filterType),
                        shape = RoundedCornerShape(5.dp)
                    )
            )
            
            Text(
                text = "${getFilterDisplayName(appliedFilter.filterType)} ${(appliedFilter.intensity * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 1
            )
            
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove filter",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * Get display name for filter type
 */
@Composable
private fun getFilterDisplayName(filterType: com.uaialternativa.imageeditor.domain.model.FilterType): String {
    return when (filterType) {
        com.uaialternativa.imageeditor.domain.model.FilterType.BRIGHTNESS -> stringResource(R.string.filter_brightness)
        com.uaialternativa.imageeditor.domain.model.FilterType.CONTRAST -> stringResource(R.string.filter_contrast)
        com.uaialternativa.imageeditor.domain.model.FilterType.SATURATION -> stringResource(R.string.filter_saturation)
        com.uaialternativa.imageeditor.domain.model.FilterType.BLUR -> stringResource(R.string.filter_blur)
        com.uaialternativa.imageeditor.domain.model.FilterType.SEPIA -> stringResource(R.string.filter_sepia)
        com.uaialternativa.imageeditor.domain.model.FilterType.GRAYSCALE -> stringResource(R.string.filter_grayscale)
    }
}

/**
 * Get preview color for filter type
 */
private fun getFilterPreviewColor(filterType: com.uaialternativa.imageeditor.domain.model.FilterType): Color {
    return when (filterType) {
        com.uaialternativa.imageeditor.domain.model.FilterType.BRIGHTNESS -> Color(0xFFFFD700)
        com.uaialternativa.imageeditor.domain.model.FilterType.CONTRAST -> Color(0xFF4A4A4A)
        com.uaialternativa.imageeditor.domain.model.FilterType.SATURATION -> Color(0xFFFF6B6B)
        com.uaialternativa.imageeditor.domain.model.FilterType.BLUR -> Color(0xFF87CEEB)
        com.uaialternativa.imageeditor.domain.model.FilterType.SEPIA -> Color(0xFFA0522D)
        com.uaialternativa.imageeditor.domain.model.FilterType.GRAYSCALE -> Color(0xFF808080)
    }
}

/**
 * Get icon for filter type
 */
private fun getFilterIcon(filterType: com.uaialternativa.imageeditor.domain.model.FilterType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (filterType) {
        com.uaialternativa.imageeditor.domain.model.FilterType.BRIGHTNESS -> Icons.Default.Settings
        com.uaialternativa.imageeditor.domain.model.FilterType.CONTRAST -> Icons.Default.Settings
        com.uaialternativa.imageeditor.domain.model.FilterType.SATURATION -> Icons.Default.Settings
        com.uaialternativa.imageeditor.domain.model.FilterType.BLUR -> Icons.Default.Settings
        com.uaialternativa.imageeditor.domain.model.FilterType.SEPIA -> Icons.Default.Settings
        com.uaialternativa.imageeditor.domain.model.FilterType.GRAYSCALE -> Icons.Default.Settings
    }
}

/**
 * Crop panel with interactive crop overlay
 */
@Composable
internal fun CropPanel(
    currentWidth: Int?,
    currentHeight: Int?,
    cropBounds: android.graphics.Rect?,
    onCropBoundsChanged: (android.graphics.Rect) -> Unit,
    onApplyCrop: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val analytics = LocalAnalytics.current
    var localCropBounds by remember(currentWidth, currentHeight) {
        mutableStateOf(
            cropBounds ?: if (currentWidth != null && currentHeight != null) {
                android.graphics.Rect(
                    currentWidth / 4,
                    currentHeight / 4,
                    currentWidth * 3 / 4,
                    currentHeight * 3 / 4
                )
            } else null
        )
    }
    
    // Update parent when local bounds change
    LaunchedEffect(localCropBounds) {
        localCropBounds?.let { onCropBoundsChanged(it) }
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.crop_tool),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = {
                    analytics.logButtonClick("crop_cancel", "Editor")
                    onCancel()
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = {
                    analytics.logButtonClick("crop_apply", "Editor")
                    onApplyCrop()
                },
                enabled = localCropBounds != null,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Apply")
            }
        }
    }
}

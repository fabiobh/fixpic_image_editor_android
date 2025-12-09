package com.uaialternativa.imageeditor.ui.editor.crop

import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive crop overlay with draggable handles
 */
@Composable
fun CropOverlay(
    imageSize: IntSize,
    cropBounds: Rect?,
    onCropBoundsChanged: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewSize by remember { mutableStateOf(IntSize.Zero) }
    var currentCropBounds by remember(cropBounds, imageSize) {
        mutableStateOf(
            cropBounds ?: Rect(
                imageSize.width / 4,
                imageSize.height / 4,
                imageSize.width * 3 / 4,
                imageSize.height * 3 / 4
            )
        )
    }
    
    // Calculate scale factor to fit image in view
    val scaleFactor = remember(imageSize, viewSize) {
        if (viewSize.width == 0 || viewSize.height == 0 || imageSize.width == 0 || imageSize.height == 0) {
            1f
        } else {
            min(
                viewSize.width.toFloat() / imageSize.width,
                viewSize.height.toFloat() / imageSize.height
            )
        }
    }
    
    // Calculate offset to center image in view
    val imageOffset = remember(imageSize, viewSize, scaleFactor) {
        Offset(
            (viewSize.width - imageSize.width * scaleFactor) / 2f,
            (viewSize.height - imageSize.height * scaleFactor) / 2f
        )
    }
    
    // Convert image coordinates to screen coordinates
    fun imageToScreen(point: Offset): Offset {
        return Offset(
            point.x * scaleFactor + imageOffset.x,
            point.y * scaleFactor + imageOffset.y
        )
    }
    
    // Convert screen coordinates to image coordinates
    fun screenToImage(point: Offset): Offset {
        return Offset(
            (point.x - imageOffset.x) / scaleFactor,
            (point.y - imageOffset.y) / scaleFactor
        )
    }
    
    // Clamp coordinates to image bounds
    fun clampToImage(x: Float, y: Float): Offset {
        return Offset(
            x.coerceIn(0f, imageSize.width.toFloat()),
            y.coerceIn(0f, imageSize.height.toFloat())
        )
    }
    
    var draggedHandle by remember { mutableStateOf<CropHandle?>(null) }
    
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { viewSize = it }
            .pointerInput(imageSize, scaleFactor) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val screenBounds = Rect(
                            (currentCropBounds.left * scaleFactor + imageOffset.x).toInt(),
                            (currentCropBounds.top * scaleFactor + imageOffset.y).toInt(),
                            (currentCropBounds.right * scaleFactor + imageOffset.x).toInt(),
                            (currentCropBounds.bottom * scaleFactor + imageOffset.y).toInt()
                        )
                        
                        draggedHandle = detectHandle(offset, screenBounds, scaleFactor)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        
                        val handle = draggedHandle ?: return@detectDragGestures
                        val imagePoint = screenToImage(change.position)
                        val clamped = clampToImage(imagePoint.x, imagePoint.y)
                        
                        val newBounds = when (handle) {
                            CropHandle.TopLeft -> Rect(
                                clamped.x.toInt().coerceAtMost(currentCropBounds.right - 50),
                                clamped.y.toInt().coerceAtMost(currentCropBounds.bottom - 50),
                                currentCropBounds.right,
                                currentCropBounds.bottom
                            )
                            CropHandle.TopRight -> Rect(
                                currentCropBounds.left,
                                clamped.y.toInt().coerceAtMost(currentCropBounds.bottom - 50),
                                clamped.x.toInt().coerceAtLeast(currentCropBounds.left + 50),
                                currentCropBounds.bottom
                            )
                            CropHandle.BottomLeft -> Rect(
                                clamped.x.toInt().coerceAtMost(currentCropBounds.right - 50),
                                currentCropBounds.top,
                                currentCropBounds.right,
                                clamped.y.toInt().coerceAtLeast(currentCropBounds.top + 50)
                            )
                            CropHandle.BottomRight -> Rect(
                                currentCropBounds.left,
                                currentCropBounds.top,
                                clamped.x.toInt().coerceAtLeast(currentCropBounds.left + 50),
                                clamped.y.toInt().coerceAtLeast(currentCropBounds.top + 50)
                            )
                            CropHandle.Center -> {
                                val dragImageSpace = Offset(
                                    dragAmount.x / scaleFactor,
                                    dragAmount.y / scaleFactor
                                )
                                
                                val width = currentCropBounds.width()
                                val height = currentCropBounds.height()
                                
                                var newLeft = currentCropBounds.left + dragImageSpace.x.toInt()
                                var newTop = currentCropBounds.top + dragImageSpace.y.toInt()
                                
                                // Clamp to image bounds
                                newLeft = newLeft.coerceIn(0, imageSize.width - width)
                                newTop = newTop.coerceIn(0, imageSize.height - height)
                                
                                Rect(newLeft, newTop, newLeft + width, newTop + height)
                            }
                        }
                        
                        currentCropBounds = newBounds
                        onCropBoundsChanged(newBounds)
                    },
                    onDragEnd = {
                        draggedHandle = null
                    }
                )
            }
    ) {
        if (imageSize.width == 0 || imageSize.height == 0) return@Canvas
        
        val screenBounds = Rect(
            (currentCropBounds.left * scaleFactor + imageOffset.x).toInt(),
            (currentCropBounds.top * scaleFactor + imageOffset.y).toInt(),
            (currentCropBounds.right * scaleFactor + imageOffset.x).toInt(),
            (currentCropBounds.bottom * scaleFactor + imageOffset.y).toInt()
        )
        
        // Draw dimmed overlay outside crop area
        val imageScreenBounds = Rect(
            imageOffset.x.toInt(),
            imageOffset.y.toInt(),
            (imageOffset.x + imageSize.width * scaleFactor).toInt(),
            (imageOffset.y + imageSize.height * scaleFactor).toInt()
        )
        
        // Top overlay
        if (screenBounds.top > imageScreenBounds.top) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(imageScreenBounds.left.toFloat(), imageScreenBounds.top.toFloat()),
                size = Size(
                    imageScreenBounds.width().toFloat(),
                    (screenBounds.top - imageScreenBounds.top).toFloat()
                )
            )
        }
        
        // Bottom overlay
        if (screenBounds.bottom < imageScreenBounds.bottom) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(imageScreenBounds.left.toFloat(), screenBounds.bottom.toFloat()),
                size = Size(
                    imageScreenBounds.width().toFloat(),
                    (imageScreenBounds.bottom - screenBounds.bottom).toFloat()
                )
            )
        }
        
        // Left overlay
        if (screenBounds.left > imageScreenBounds.left) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(imageScreenBounds.left.toFloat(), screenBounds.top.toFloat()),
                size = Size(
                    (screenBounds.left - imageScreenBounds.left).toFloat(),
                    screenBounds.height().toFloat()
                )
            )
        }
        
        // Right overlay
        if (screenBounds.right < imageScreenBounds.right) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(screenBounds.right.toFloat(), screenBounds.top.toFloat()),
                size = Size(
                    (imageScreenBounds.right - screenBounds.right).toFloat(),
                    screenBounds.height().toFloat()
                )
            )
        }
        
        // Draw crop rectangle border
        drawRect(
            color = Color.White,
            topLeft = Offset(screenBounds.left.toFloat(), screenBounds.top.toFloat()),
            size = Size(screenBounds.width().toFloat(), screenBounds.height().toFloat()),
            style = Stroke(width = 3f)
        )
        
        // Draw grid lines (rule of thirds)
        val gridColor = Color.White.copy(alpha = 0.5f)
        val thirdWidth = screenBounds.width() / 3f
        val thirdHeight = screenBounds.height() / 3f
        
        // Vertical lines
        drawLine(
            color = gridColor,
            start = Offset(screenBounds.left + thirdWidth, screenBounds.top.toFloat()),
            end = Offset(screenBounds.left + thirdWidth, screenBounds.bottom.toFloat()),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(screenBounds.left + 2 * thirdWidth, screenBounds.top.toFloat()),
            end = Offset(screenBounds.left + 2 * thirdWidth, screenBounds.bottom.toFloat()),
            strokeWidth = 1f
        )
        
        // Horizontal lines
        drawLine(
            color = gridColor,
            start = Offset(screenBounds.left.toFloat(), screenBounds.top + thirdHeight),
            end = Offset(screenBounds.right.toFloat(), screenBounds.top + thirdHeight),
            strokeWidth = 1f
        )
        drawLine(
            color = gridColor,
            start = Offset(screenBounds.left.toFloat(), screenBounds.top + 2 * thirdHeight),
            end = Offset(screenBounds.right.toFloat(), screenBounds.top + 2 * thirdHeight),
            strokeWidth = 1f
        )
        
        // Draw corner handles as circles (easier to tap)
        val circleRadius = 40f  // Doubled from 20f
        val circleColor = Color.White
        val circleStrokeWidth = 8f  // Doubled from 4f
        val centerY = (screenBounds.top + screenBounds.bottom) / 2f
        
        // Top-left circle
        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = Offset(screenBounds.left.toFloat(), screenBounds.top.toFloat()),
            style = Stroke(width = circleStrokeWidth)
        )
        drawCircle(
            color = circleColor.copy(alpha = 0.3f),
            radius = circleRadius,
            center = Offset(screenBounds.left.toFloat(), screenBounds.top.toFloat())
        )
        
        // Top-right circle
        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = Offset(screenBounds.right.toFloat(), screenBounds.top.toFloat()),
            style = Stroke(width = circleStrokeWidth)
        )
        drawCircle(
            color = circleColor.copy(alpha = 0.3f),
            radius = circleRadius,
            center = Offset(screenBounds.right.toFloat(), screenBounds.top.toFloat())
        )
        
        // Bottom-left circle
        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = Offset(screenBounds.left.toFloat(), screenBounds.bottom.toFloat()),
            style = Stroke(width = circleStrokeWidth)
        )
        drawCircle(
            color = circleColor.copy(alpha = 0.3f),
            radius = circleRadius,
            center = Offset(screenBounds.left.toFloat(), screenBounds.bottom.toFloat())
        )
        
        // Bottom-right circle
        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = Offset(screenBounds.right.toFloat(), screenBounds.bottom.toFloat()),
            style = Stroke(width = circleStrokeWidth)
        )
        drawCircle(
            color = circleColor.copy(alpha = 0.3f),
            radius = circleRadius,
            center = Offset(screenBounds.right.toFloat(), screenBounds.bottom.toFloat())
        )
        

    }
}

/**
 * Enum representing different crop handles
 */
private enum class CropHandle {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    Center
}

/**
 * Detect which handle (if any) was touched
 * Returns null if touched outside the crop area
 */
private fun detectHandle(touchPoint: Offset, bounds: Rect, scaleFactor: Float): CropHandle? {
    // Much larger touch area to match the larger visual circles
    val touchRadius = 400f * scaleFactor  // Quadrupled from 100f to match larger circles
    
    // Check corners first (higher priority)
    if (touchPoint.x in (bounds.left - touchRadius)..(bounds.left + touchRadius) &&
        touchPoint.y in (bounds.top - touchRadius)..(bounds.top + touchRadius)) {
        return CropHandle.TopLeft
    }
    
    if (touchPoint.x in (bounds.right - touchRadius)..(bounds.right + touchRadius) &&
        touchPoint.y in (bounds.top - touchRadius)..(bounds.top + touchRadius)) {
        return CropHandle.TopRight
    }
    
    if (touchPoint.x in (bounds.left - touchRadius)..(bounds.left + touchRadius) &&
        touchPoint.y in (bounds.bottom - touchRadius)..(bounds.bottom + touchRadius)) {
        return CropHandle.BottomLeft
    }
    
    if (touchPoint.x in (bounds.right - touchRadius)..(bounds.right + touchRadius) &&
        touchPoint.y in (bounds.bottom - touchRadius)..(bounds.bottom + touchRadius)) {
        return CropHandle.BottomRight
    }
    
    // Check if inside crop area (for moving)
    if (touchPoint.x in bounds.left.toFloat()..bounds.right.toFloat() &&
        touchPoint.y in bounds.top.toFloat()..bounds.bottom.toFloat()) {
        return CropHandle.Center
    }
    
    // Return null if touched outside the crop area
    return null
}

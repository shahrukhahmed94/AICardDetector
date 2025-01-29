package com.shahrukh.aicarddetector.presentation.common

import android.content.Context
import android.util.DisplayMetrics
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PersistentDrawDetectionBox(
    bottomMargin: Float = 0f,
    leftRightPadding: Dp = 16.dp
) {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics

    val screenWidth = displayMetrics.widthPixels.toFloat()
    val screenHeight = displayMetrics.heightPixels.toFloat()

    // Convert padding to pixels
    val horizontalPaddingPx = leftRightPadding.value * displayMetrics.density

    // Calculate box dimensions (1/3 height)
    val boxWidth = screenWidth - (2 * horizontalPaddingPx)
    val boxHeight = screenHeight / 3f

    // Position box in the center vertically
    val topPosition = (screenHeight - boxHeight) / 2f

    val adjustedTop = topPosition - bottomMargin

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.matchParentSize(),
            onDraw = {
                drawRect(
                    color = Color.Red,
                    size = Size(boxWidth, boxHeight),
                    topLeft = Offset(horizontalPaddingPx, adjustedTop),
                    style = Stroke(width = 4f)
                )
            }
        )
    }
}
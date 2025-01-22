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

/**
 * Composable function to draw a persistent detection box on the canvas.
 * This box will have a fixed size representing an ID card or a credit card.
 *
 * @param topMargin The margin from the top provided by the user, in pixels.
 */
@Composable
fun PersistentDrawDetectionBox(bottomMargin: Float = 0f) {

    // Retrieve screen height dynamically using LocalContext
    val context = LocalContext.current
    val screenHeight = getScreenHeight(context)

    // Define fixed size for the persistent box (in pixels, based on typical ID or credit card dimensions)
    val fixedWidth = 600f  // Example width, adjust based on your screen
    val fixedHeight = 420f // Example height, adjust based on your screen

    // Calculate the dynamic Y position (half of the screen height minus half of the box height)
    val dynamicTop = (screenHeight - fixedHeight) / 2f

    // If user provides a top margin, subtract it from the dynamicTop
    val adjustedTop = dynamicTop - bottomMargin

    // Composable Box to hold the Canvas
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.matchParentSize(),
            onDraw = {
                // Draw the persistent box with a fixed size at the adjusted top position
                drawRect(
                    color = Color.Red,  // You can change the color as needed
                    size = Size(fixedWidth, fixedHeight),
                    topLeft = Offset(50f, adjustedTop),  // Position on screen (adjusted)
                    style = Stroke(width = 4f)  // Stroke width for the border
                )
            }
        )
    }
}

/**
 * Helper function to get the screen height
 */
fun getScreenHeight(context: Context): Float {
    val metrics = DisplayMetrics()
    //context.resources.displayMetrics.getMetrics(metrics)
    //return context.resources.displayMetrics.heightPixels.toFloat()

    return  context.resources.displayMetrics.heightPixels.toFloat()
}

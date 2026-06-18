package com.shahrukh.aicarddetector.presentation.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PersistentDrawDetectionBox(
    bottomMargin: Float = 0f,
    leftRightPadding: Dp = 24.dp,
    borderColor: Color = Color(0xFF00FFCC), // Neon Cyan/Green
    maskColor: Color = Color(0xAA0A0E1A) // Cyberpunk dark blue/black mask
) {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val density = displayMetrics.density
    val horizontalPaddingPx = leftRightPadding.value * density

    // Infinite transition for the scanning laser line
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserYProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserY"
    )

    // Pulse animation for the corner guides
    val cornerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cornerAlpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f) // Crucial for BlendMode.Clear to work as transparency
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate box dimensions (standard card aspect ratio ~1.58:1)
            val boxWidth = canvasWidth - (2 * horizontalPaddingPx)
            val boxHeight = boxWidth / 1.58f

            // Position box in the center vertically, adjusted by bottomMargin
            val topPosition = (canvasHeight - boxHeight) / 2f
            val adjustedTop = topPosition - bottomMargin

            // 1. Draw the dark semi-transparent mask over the whole screen
            drawRect(color = maskColor)

            // 2. Clear the rounded rectangular card cutout
            val cornerRadius = 16.dp.toPx()
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(horizontalPaddingPx, adjustedTop),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                blendMode = BlendMode.Clear
            )

            // 3. Draw the card guide border outline
            drawRoundRect(
                color = borderColor.copy(alpha = 0.3f),
                topLeft = Offset(horizontalPaddingPx, adjustedTop),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 2.dp.toPx())
            )

            // 4. Draw bold, glowing corner brackets
            val bracketLength = 24.dp.toPx()
            val strokeWidth = 4.dp.toPx()
            val offsetAdjustment = strokeWidth / 2f
            
            // Left-Top corner
            val ltX = horizontalPaddingPx - offsetAdjustment
            val ltY = adjustedTop - offsetAdjustment
            // horizontal line
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(ltX, ltY),
                end = Offset(ltX + bracketLength, ltY),
                strokeWidth = strokeWidth
            )
            // vertical line
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(ltX, ltY),
                end = Offset(ltX, ltY + bracketLength),
                strokeWidth = strokeWidth
            )

            // Right-Top corner
            val rtX = horizontalPaddingPx + boxWidth + offsetAdjustment
            val rtY = adjustedTop - offsetAdjustment
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(rtX, rtY),
                end = Offset(rtX - bracketLength, rtY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(rtX, rtY),
                end = Offset(rtX, rtY + bracketLength),
                strokeWidth = strokeWidth
            )

            // Left-Bottom corner
            val lbX = horizontalPaddingPx - offsetAdjustment
            val lbY = adjustedTop + boxHeight + offsetAdjustment
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(lbX, lbY),
                end = Offset(lbX + bracketLength, lbY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(lbX, lbY),
                end = Offset(lbX, lbY - bracketLength),
                strokeWidth = strokeWidth
            )

            // Right-Bottom corner
            val rbX = horizontalPaddingPx + boxWidth + offsetAdjustment
            val rbY = adjustedTop + boxHeight + offsetAdjustment
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(rbX, rbY),
                end = Offset(rbX - bracketLength, rbY),
                strokeWidth = strokeWidth
            )
            drawLine(
                color = borderColor.copy(alpha = cornerAlpha),
                start = Offset(rbX, rbY),
                end = Offset(rbX, rbY - bracketLength),
                strokeWidth = strokeWidth
            )

            // 5. Draw the scanning laser line
            val currentLaserY = adjustedTop + (boxHeight * laserYProgress)
            
            // Draw horizontal laser line with gradient glow
            val laserGlowHeight = 8.dp.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        borderColor.copy(alpha = 0f),
                        borderColor.copy(alpha = 0.8f),
                        borderColor.copy(alpha = 0f)
                    ),
                    startY = currentLaserY - laserGlowHeight,
                    endY = currentLaserY + laserGlowHeight
                ),
                topLeft = Offset(horizontalPaddingPx + 4.dp.toPx(), currentLaserY - laserGlowHeight),
                size = Size(boxWidth - 8.dp.toPx(), laserGlowHeight * 2)
            )

            // Draw core laser beam
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(horizontalPaddingPx + 8.dp.toPx(), currentLaserY),
                end = Offset(horizontalPaddingPx + boxWidth - 8.dp.toPx(), currentLaserY),
                strokeWidth = 1.5.dp.toPx()
            )
        }
    }
}
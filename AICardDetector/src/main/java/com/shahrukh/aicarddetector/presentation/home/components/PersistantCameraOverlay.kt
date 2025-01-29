package com.shahrukh.aicarddetector.presentation.home.components



import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.presentation.common.DrawDetectionBox
import com.shahrukh.aicarddetector.presentation.common.PersistentDrawDetectionBox

/**
 * CameraOverlay composable that displays both a persistent detection box
 * (fixed size for ID or credit card) and dynamic detection boxes from AI detection.
 */
@Composable
fun PersistentCameraOverlay(detections: List<Detection>,bottomMargin: Float = 0f) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Draw the persistent detection box first (this will be static on the screen)
        //PersistentDrawDetectionBox()

        PersistentDrawDetectionBox(bottomMargin = bottomMargin)

        // Then, draw dynamic detection boxes based on AI detections
        /**detections.forEach { detection ->
            DrawDetectionBox(detection)
        }*/
    }
}

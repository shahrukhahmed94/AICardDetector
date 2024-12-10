package com.shahrukh.aicarddetector.presentation.home.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.presentation.common.DrawDetectionBox

@Composable
fun CameraOverlay(detections: List<Detection>) {
    Box(modifier = Modifier.fillMaxSize()) {
        detections.forEach { detection ->
            DrawDetectionBox(detection)
        }
    }
}
package com.shahrukh.aicarddetector.presentation.utils

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import com.shahrukh.aicarddetector.domain.manager.ObjectDetectionManager
import com.shahrukh.aicarddetector.domain.model.Detection

/**
 * This analyzer processes camera frames to detect objects, using a custom implementation that
 * interfaces with an object detection model provided by [ObjectDetectionManager].
 *
 * @property onObjectDetectionResults A callback function that processes the list of [Detection]
 * objects returned by the object detection algorithm for each analyzed frame.
 * @property confidenceScoreState A state holding the threshold for the confidence score,
 * used to filter results by the object detection manager.
 *
 * @constructor Creates an instance of the analyzer with necessary dependencies for object detection operations.
 */
class CameraFrameAnalyzer(
    private val objectDetectionManager: ObjectDetectionManager,
    private val onObjectDetectionResults: (List<Detection>) -> Unit,
    private val confidenceScoreState: State<Float>
) : ImageAnalysis.Analyzer {
    private var frameSkipCounter = 0

    override fun analyze(image: ImageProxy) {
        // Analyze only 1 frame every second
        if (frameSkipCounter % 60 == 0) {
            // Rotating the image by transforming it via Matrix using rotationDegrees
            val rotatedImageMatrix: Matrix = Matrix().apply {
                postRotate(image.imageInfo.rotationDegrees.toFloat())
            }

            // Creating a new Bitmap via createBitmap using 'rotatedImageMatrix'
            val rotatedBitmap: Bitmap = Bitmap.createBitmap(
                image.toBitmap(),
                0,
                0,
                image.width,
                image.height,
                rotatedImageMatrix,
                true
            )

            // Obtaining results via objectDetectionManager in Domain Layer
            val objectDetectionResults = objectDetectionManager.detectObjectsInCurrentFrame(
                bitmap = rotatedBitmap,
                rotation = image.imageInfo.rotationDegrees,
                confidenceThreshold = confidenceScoreState.value
            )
            onObjectDetectionResults(objectDetectionResults)
        }
        frameSkipCounter++

        // Fully processed the frame
        image.close()
    }
}

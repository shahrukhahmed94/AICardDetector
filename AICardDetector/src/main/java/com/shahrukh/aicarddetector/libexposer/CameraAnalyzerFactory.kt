package com.shahrukh.aicarddetector.libexposer

import android.util.Log
import androidx.compose.runtime.State
import com.shahrukh.aicarddetector.domain.manager.ObjectDetectionManager
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.presentation.utils.CameraFrameAnalyzer

/**
 * Singleton factory class for creating an instance of CameraFrameAnalyzer.
 */
object CameraFrameAnalyzerFactory {
    private var objectDetectionManager: ObjectDetectionManager? = null

    /**
     * Initializes the factory with the required dependencies.
     *
     * @param manager An instance of [ObjectDetectionManager].
     */
    fun init(manager: ObjectDetectionManager) {
        objectDetectionManager = manager
    }

    /**
     * Checks whether the factory is initialized.
     *
     * @return `true` if the factory is initialized, `false` otherwise.
     */
    fun isInitialized(): Boolean {
        return objectDetectionManager != null
    }

    /**
     * Creates an instance of [CameraFrameAnalyzer] if the factory is initialized.
     *
     * @param onObjectDetectionResults Callback to handle detection results.
     * @param confidenceScoreState State to manage confidence score.
     * @return An instance of [CameraFrameAnalyzer] or throws an exception if the factory is not initialized.
     * @throws IllegalStateException if the factory has not been initialized.
     */
    fun createAICardDetector(
        onObjectDetectionResults: (List<Detection>) -> Unit,
        confidenceScoreState: State<Float>
    ): CameraFrameAnalyzer {

        Log.i("Factory Analyzer","Create AI Card called")
        val manager = objectDetectionManager
            ?: throw IllegalStateException("CameraFrameAnalyzerFactory has not been initialized")

        return CameraFrameAnalyzer(
            manager, // Using the initialized instance
            onObjectDetectionResults,
            confidenceScoreState
        )
    }
}

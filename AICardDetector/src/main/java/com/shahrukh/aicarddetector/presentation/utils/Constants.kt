package com.shahrukh.aicarddetector.presentation.utils

import android.graphics.Bitmap

object Constants {

    // Values
    const val INITIAL_CONFIDENCE_SCORE = 0.5f

    const val ORIGINAL_IMAGE_WIDTH = 480f
    const val ORIGINAL_IMAGE_HEIGHT = 640f

    // TensorFlow Lite
    const val MODEL_MAX_RESULTS_COUNT = 1
    const val MODEL_PATH = "detect_quant_metadata.tflite"


 var capturedImageBit: Bitmap? = null
    var originalImageBitmap: Bitmap? = null

}
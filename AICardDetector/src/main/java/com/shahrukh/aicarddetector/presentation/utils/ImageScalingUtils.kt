package com.shahrukh.aicarddetector.presentation.utils

import android.graphics.RectF

/**
 * Utility object for scaling image dimensions and bounding box coordinates. It provides functions
 * to calculate scale factors for resizing images to fit specific dimensions while maintaining
 * aspect ratio, and to adjust bounding box coordinates from a source image size to match a target
 * preview size, useful in applications involving image processing and object detection where the
 * output coordinates need to be mapped onto a different display resolution.
 */
object ImageScalingUtils {

    /**
     * Maps the size of a given source to fit within a target size while maintaining the aspect ratio.
     *
     * @param targetWidth The width of the target size.
     * @param targetHeight The height of the target size.
     * @return The mapped size that maintains the original aspect ratio but fits within the target dimensions.
     */
    fun getScaleFactors(
        targetWidth: Int,
        targetHeight: Int
    ): FloatArray {
        return try {
            val scaleX = (targetWidth.toFloat() / Constants.ORIGINAL_IMAGE_WIDTH)
            val scaleY = (targetHeight.toFloat() / Constants.ORIGINAL_IMAGE_HEIGHT)
            floatArrayOf(scaleX, scaleY)
        } catch(exception: Exception) {
            floatArrayOf(1f, 1f)
        }
    }


}
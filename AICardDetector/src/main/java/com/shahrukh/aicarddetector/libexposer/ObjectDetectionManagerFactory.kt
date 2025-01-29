package com.shahrukh.aicarddetector.libexposer

import android.content.ContentValues
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.shahrukh.aicarddetector.domain.manager.ObjectDetectionManager
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.manager.ObjectDetectionManagerImpl
import com.shahrukh.aicarddetector.presentation.utils.CameraFrameAnalyzer
import com.shahrukh.aicarddetector.presentation.utils.Constants.capturedImageBit
import com.shahrukh.aicarddetector.presentation.utils.Constants.originalImageBitmap
import com.shahrukh.aicarddetector.utils.toPx


import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

object ObjectDetectionManagerFactory {

    private val _isImageSavedStateFlow = MutableStateFlow(true)

    val isImageSavedStateFlow = _isImageSavedStateFlow.asStateFlow()



    fun create(context: Context): ObjectDetectionManager {
        return ObjectDetectionManagerImpl(context)
    }



    fun prepareCameraController(
        context: Context,
        cameraFrameAnalyzer: CameraFrameAnalyzer
    ): LifecycleCameraController {



        Log.d("Camera Frame Analyzer ", "${cameraFrameAnalyzer.toString()}")

        Log.d("Tag", "prepareCameraController() called")
        return LifecycleCameraController(context).apply {
            setEnabledUseCases(
                CameraController.IMAGE_ANALYSIS or
                        CameraController.IMAGE_CAPTURE

            )




            setImageAnalysisAnalyzer(
                ContextCompat.getMainExecutor(context),
                cameraFrameAnalyzer
            )
        }
    }

    /**
     * Retrieves the selected camera (front or back) based on the current camera selection of the provided camera controller.
     *
     * @param cameraController The controller managing the camera operations.
     * @return Returns [CameraSelector.DEFAULT_FRONT_CAMERA] if the current camera is the back camera,
     *         and [CameraSelector.DEFAULT_BACK_CAMERA] if it's the front camera.
     */
    fun getSelectedCamera(cameraController: LifecycleCameraController): CameraSelector {
        Log.d("TAG", "getSelectedCamera() called")
        return if (cameraController.cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    fun cropImage(bitmap: Bitmap, boundingBox: RectF, tensorImageWidth: Int, tensorImageHeight: Int): Bitmap {

        val originalWidth = bitmap.width
        val originalHeight = bitmap.height


        // Calculate scaling factors
        val scaleX = originalWidth.toFloat() / tensorImageWidth
        val scaleY = originalHeight.toFloat() / tensorImageHeight


        // Convert bounding box coordinates to original image dimensions
        val left = ( (boundingBox.left  ) * scaleX).toInt()
        val top = ( (boundingBox.top  ) * scaleY).toInt()
        val right = ( (boundingBox.right - 5 ) * scaleX).toInt()
        val bottom = ( (boundingBox.bottom - 15 ) * scaleY).toInt()


        // Ensure dimensions are within bounds
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        // Ensure the cropping rectangle is within the bitmap bounds
        val adjustedLeft = left.coerceIn(0, originalWidth - 1)
        val adjustedTop = top.coerceIn(0, originalHeight - 1)
        val adjustedRight = (adjustedLeft + width).coerceAtMost(originalWidth)
        val adjustedBottom = (adjustedTop + height).coerceAtMost(originalHeight)


        // Crop the image
        return Bitmap.createBitmap(
            bitmap,
            adjustedLeft,
            adjustedTop,
            adjustedRight - adjustedLeft,
            adjustedBottom - adjustedTop
        )


    }


    fun cropPersistentImage(bitmap: Bitmap, boundingBox: RectF, screenWidth: Float, screenHeight: Float): Bitmap {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        // Calculate scaling factors based on the screen size vs. bitmap size
        val scaleX = originalWidth / screenWidth
        val scaleY = originalHeight / screenHeight

        // Convert bounding box coordinates to bitmap coordinates
        val left = (boundingBox.left * scaleX).toInt()
        val top = (boundingBox.top * scaleY).toInt()
        val right = (boundingBox.right * scaleX).toInt()
        val bottom = (boundingBox.bottom * scaleY).toInt()

        // Ensure dimensions are within bounds
        val width = (right - left).coerceAtLeast(1)
        val height = (bottom - top).coerceAtLeast(1)

        // Ensure the cropping rectangle is within the bitmap bounds
        val adjustedLeft = left.coerceIn(0, originalWidth - 1)
        val adjustedTop = top.coerceIn(0, originalHeight - 1)
        val adjustedRight = (adjustedLeft + width).coerceAtMost(originalWidth)
        val adjustedBottom = (adjustedTop + height).coerceAtMost(originalHeight)

        // Crop the image
        return Bitmap.createBitmap(
            bitmap,
            adjustedLeft,
            adjustedTop,
            adjustedRight - adjustedLeft,
            adjustedBottom - adjustedTop
        )
    }


    /**
     * Captures a photo using the provided camera controller and processes the captured image.
     *
     * This function initiates a photo capture and once successful, it rotates the image based on
     * its rotation degrees. Also, if the image is captured using the front camera, it
     * inverts the image along the X-axis. After processing the image, the resulting bitmap is saved
     * by calling the 'saveBitmapToDevice' private method,
     *
     * @param context The application's context.
     * @param cameraController The lifecycle-aware camera controller to manage the photo capture.
     */




   /** fun capturePhoto(
        context: Context,
        // navController: NavController,
        cameraController: LifecycleCameraController,
        screenWidth: Float,
        screenHeight: Float,
        detections: List<Detection>
    )  {

        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {

                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    Log.d("TAG", "onCaptureSuccess() called for capturePhoto")

                    val rotatedImageMatrix: Matrix =
                        Matrix().apply {
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

                    val combinedBitmap = overlayDetectionsOnBitmap(
                        rotatedBitmap,
                        detections,
                        screenWidth,
                        screenHeight
                    )

                    val detection = detections.firstOrNull()
                    if (detection != null) {

                        val croppedBitmap = cropImage(
                            rotatedBitmap,
                            detection.boundingBox,
                            detection.tensorImageWidth,
                            detection.tensorImageHeight
                        )

                        // Switching the context to IO for saving the bitmap to device
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                val savedBitmap = saveBitmapToDevice(context, croppedBitmap)
                                val originalBitmap = saveBitmapToDevice(context, combinedBitmap)

                                if (savedBitmap != null) {
                                    // Switching back to Main to update UI and navigate
                                    withContext(Dispatchers.Main) {
                                        capturedImageBit = savedBitmap
                                        originalImageBitmap = originalBitmap
                                        //navController.navigate(Routes.ROUTE_PREVIEW_DETECTED_OBJECT_SCREEN)
                                        Log.i("Saved Bitmap is", savedBitmap.toString())
                                        // Handle the successfully saved bitmap
                                        Log.d("TAG", "Image saved successfully")
                                    }
                                } else {
                                    Log.e("TAG", "Failed to save image")
                                }
                            }
                        }
                    }
                    //return capturedImageBit
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.e("TAG", "onError() called for capturePhoto with: exception = $exception")
                    isPhotoSuccessfullySaved(false)
                }
            }
        )
    }*/

   suspend fun capturePhoto(
       context: Context,
       cameraController: LifecycleCameraController,
       screenWidth: Float,
       screenHeight: Float,
       detections: List<Detection>
   ): Bitmap? = suspendCoroutine { continuation ->
       cameraController.takePicture(
           ContextCompat.getMainExecutor(context),
           object : ImageCapture.OnImageCapturedCallback() {

               override fun onCaptureSuccess(image: ImageProxy) {
                   super.onCaptureSuccess(image)
                   Log.d("TAG", "onCaptureSuccess() called for capturePhoto")

                   try {
                       val rotatedImageMatrix = Matrix().apply {
                           postRotate(image.imageInfo.rotationDegrees.toFloat())
                       }

                       // Convert the captured image to a Bitmap
                       val rotatedBitmap = Bitmap.createBitmap(
                           image.toBitmap(),
                           0,
                           0,
                           image.width,
                           image.height,
                           rotatedImageMatrix,
                           true
                       )

                       val detection = detections.firstOrNull()
                       if (detection != null) {
                           val croppedBitmap = cropImage(
                               rotatedBitmap,
                               detection.boundingBox,
                               detection.tensorImageWidth,
                               detection.tensorImageHeight
                           )

                           // Save cropped bitmap asynchronously
                           CoroutineScope(Dispatchers.IO).launch {
                               try {
                                   saveBitmapToDevice(context, croppedBitmap)
                                   withContext(Dispatchers.Main) {
                                       continuation.resume(croppedBitmap)
                                   }
                               } catch (e: Exception) {
                                   Log.e("TAG", "Failed to save or process the image: $e")
                                   withContext(Dispatchers.Main) {
                                       continuation.resumeWithException(e)
                                   }
                               }
                           }
                       } else {
                           Log.e("TAG", "No detection found.")
                           continuation.resume(null)
                       }
                   } catch (e: Exception) {
                       Log.e("TAG", "Exception in onCaptureSuccess: $e")
                       continuation.resumeWithException(e)
                   } finally {
                       image.close()
                   }
               }

               override fun onError(exception: ImageCaptureException) {
                   super.onError(exception)
                   Log.e("TAG", "onError() called for capturePhoto with: exception = $exception")
                   continuation.resumeWithException(exception)
               }
           }
       )
   }

    suspend fun capturePersistentPhoto(
        context: Context,
        cameraController: LifecycleCameraController,
        screenWidth: Float,
        screenHeight: Float,
        bottomMargin: Float,
        previewWeight: Float // New parameter to handle scaling
    ): Bitmap? = suspendCoroutine { continuation ->
        cameraController.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rotatedImageMatrix = Matrix().apply {
                            postRotate(image.imageInfo.rotationDegrees.toFloat())
                        }

                        // Convert to bitmap with original camera resolution
                        val originalBitmap = image.toBitmap()
                        val rotatedBitmap = Bitmap.createBitmap(
                            originalBitmap,
                            0, 0,
                            originalBitmap.width,
                            originalBitmap.height,
                            rotatedImageMatrix,
                            true
                        )

                        // Calculate preview height scaling factor
                        val previewHeight = screenHeight * previewWeight
                        val previewOffset = (screenHeight - previewHeight) / 2f // Offset added at the top

                        // Dynamic box dimensions (1/3 of the preview height)
                        val displayMetrics = context.resources.displayMetrics
                        val leftRightPadding: Dp = 16.dp
                        val horizontalPaddingPx = leftRightPadding.value * displayMetrics.density
                       /** val boxWidth = screenWidth - (2 * horizontalPaddingPx)

                        val boxHeight = previewHeight / 3f
                        val boxTop = (previewHeight - boxHeight) / 2f + previewOffset

                        // Precise scaling calculation
                        val scaleX = rotatedBitmap.width / screenWidth
                        val scaleY = rotatedBitmap.height / previewHeight

                        // Convert box coordinates to bitmap coordinates
                        val bitmapLeft = ((horizontalPaddingPx) * scaleX).toInt()
                        val bitmapTop = ((boxTop) * scaleY).toInt()
                        val bitmapWidth = ((boxWidth) * scaleX).toInt()
                        val bitmapHeight = ((boxHeight) * scaleY).toInt()*/

                       val boxHeight = previewHeight / 3f
                        val boxTop = (previewHeight - boxHeight) / 2f + previewOffset

                        val scaleY = rotatedBitmap.height / previewHeight
                        val bitmapTop = ((boxTop) * scaleY).toInt()

                     // Assume persistent box is 70% of the screen width and 30% of screen height
                     val boxWidthPercentage = 0.7f
                        val boxHeightPercentage = 0.3f

// Calculate box size and position based on screen dimensions
                        val boxWidth = screenWidth * boxWidthPercentage
                       // val boxHeight = screenHeight * boxHeightPercentage
                        val xPosition = screenWidth * 0.15f // 15% padding from the left
                        //val yPosition = (screenHeight - boxHeight) / 2f

// Convert these values to pixels using scale factors
                        val scaleX = rotatedBitmap.width / screenWidth
                      //  val scaleY = rotatedBitmap.height / screenHeight

// Calculate the bitmap crop area using scaled values
                        val bitmapLeft = (xPosition * scaleX).toInt()
                       // val bitmapTop = (yPosition * scaleY).toInt()
                        val bitmapWidth = (boxWidth * scaleX).toInt()
                        val bitmapHeight = (boxHeight * scaleY).toInt()

// Now crop the image using dynamic values
                        val croppedBitmap = Bitmap.createBitmap(
                            rotatedBitmap,
                            bitmapLeft,
                            bitmapTop,
                            bitmapWidth,
                            bitmapHeight
                        )


                        // Crop with precise bitmap coordinates
                        /**val croppedBitmap = Bitmap.createBitmap(
                            rotatedBitmap,
                           // bitmapLeft, //160
                              390,
                            bitmapTop,
                            2200,
                           // bitmapWidth, //3806
                            bitmapHeight //1032
                        )*/

                        // Save and resume
                        CoroutineScope(Dispatchers.IO).launch {
                            saveBitmapToDevice(context, croppedBitmap)
                            withContext(Dispatchers.Main) {
                                continuation.resume(croppedBitmap)
                            }
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    continuation.resumeWithException(exception)
                }
            }
        )
    }









    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    suspend fun saveBitmapToDevice(
        context: Context,
        capturedImageBitmap: Bitmap
    ): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d("TAG", "saveBitmapToDevice() called for Version = ${Build.VERSION.SDK_INT}")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, generateImageName())
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val uri: Uri? = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                    )

                    uri?.let {
                        context.contentResolver.openOutputStream(it).use { outputStream ->
                            if (outputStream != null) {
                                capturedImageBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    100,
                                    outputStream
                                )
                                outputStream.flush()
                                outputStream.close()
                                isPhotoSuccessfullySaved(true)
                                return@withContext capturedImageBitmap
                            } else {
                                return@withContext null
                            }
                        }
                    }
                }
            } catch (exception: Exception) {
                Log.e("TAG", "saveBitmapToDevice() called with: exception = $exception")
                isPhotoSuccessfullySaved(false)
            }
            null
        }
    }



    /**
     * Updates the state flow with the status of whether the photo has been successfully saved or not.
     *
     * @param isSaved Boolean flag indicating if the photo was successfully saved.
     */
    private fun isPhotoSuccessfullySaved(isSaved: Boolean) {
        Log.d("TAG", "isPhotoSuccessfullySaved() called with: isSaved Flag = $isSaved")
        _isImageSavedStateFlow.value = isSaved
    }


    /**
     * Returns the current system time in a formatted string, prepended with "IMG_".
     * The returned string is in the format "IMG_YYYYMMDD_HHMMSS", which represents the current system time.
     *
     * @return A formatted string representing the current system time, prepended with "IMG_".
     */
    private fun generateImageName(): String {
        Log.d("TAG", "generateImageName() called")
        val currentDateTime = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val formattedDate = dateFormat.format(currentDateTime)
        return "IMG_$formattedDate"
    }

    /**
     * Overlays detection boxes on the provided bitmap image.
     *
     * @param bitmap The original bitmap image where detections will be overlaid.
     * @param detections A list of [Detection] objects representing the detected items.
     * @param screenWidth The width of the screen where the image will be displayed.
     * @param screenHeight The height of the screen where the image will be displayed.
     * @return A new [Bitmap] with overlaid detections.
     */
    fun overlayDetectionsOnBitmap(
        bitmap: Bitmap,
        detections: List<Detection>,
        screenWidth: Float,
        screenHeight: Float
    ): Bitmap {
        val overlayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
        val canvas = Canvas(overlayBitmap)

        // Draw the captured image onto the new bitmap
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Draw detections on the canvas (i.e., on top of the captured image)
        detections.forEach { detection ->
            drawDetectionBox(
                detection = detection,
                originalBitmap = overlayBitmap,
                screenWidth = screenWidth,
                screenHeight = screenHeight
            )
        }
        return overlayBitmap
    }


    /**
     * Draws a detection box around a detected object on a bitmap.
     *
     * @param detection The [Detection] object containing the details of what was detected.
     * @param originalBitmap The bitmap on which the detection box is to be drawn.
     * @param screenWidth The width of the screen for scaling the detection box.
     * @param screenHeight The height of the screen for scaling the detection box.
     * @return The original bitmap with a detection box drawn on it.
     */
    private fun drawDetectionBox(
        detection: Detection,
        originalBitmap: Bitmap,
        screenWidth: Float,
        screenHeight: Float
    ): Bitmap {
        // Prepare a Paint object for drawing
        val paint = Paint().apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            color = getColorForLabel(detection.detectedObjectName)
        }

        val scaleFactor = java.lang.Float.min(
            screenWidth * 1f / detection.tensorImageWidth,
            screenHeight * 1f / detection.tensorImageHeight
        )

        // Scaling adaptively depending on DPI of the device
        val adaptiveScaleFactor: Float = getDeviceDensityValue()

        val scaledBox = RectF(
            detection.boundingBox.left * adaptiveScaleFactor,
            detection.boundingBox.top * adaptiveScaleFactor,
            detection.boundingBox.right * adaptiveScaleFactor,
            detection.boundingBox.bottom * adaptiveScaleFactor
        ).also {
            it.left = it.left.coerceAtLeast(0f)
            it.top = it.top.coerceAtLeast(0f)
            it.right = it.right.coerceAtMost(screenWidth)
            it.bottom = it.bottom.coerceAtMost(screenHeight)
        }

        // Clone the original bitmap to draw onto
        val canvas = Canvas(originalBitmap)

        // Draw the rectangle on the canvas
        canvas.drawRect(scaledBox, paint)

        val text = "${detection.detectedObjectName} ${(detection.confidenceScore * 100).toInt()}%"
        val textPaint = Paint().apply {
            color = paint.color
            textSize = 20f
        }

        // Draw the text on the canvas
        canvas.drawText(text, scaledBox.left, scaledBox.top - 10, textPaint)

        return originalBitmap
    }

    /**
     * Retrieves the device's screen density factor as a float value.
     * This value is used to scale pixel dimensions to match the current screen density.
     *
     * @return A float representing the density factor of the display (e.g., 0.75 for low, 1.0 for medium, etc.).
     * The default return value is 1.0f, corresponding to the baseline screen density (mdpi).
     */
    private fun getDeviceDensityValue(): Float {
        return when (Resources.getSystem().displayMetrics.densityDpi) {
            DisplayMetrics.DENSITY_LOW -> 0.75f
            DisplayMetrics.DENSITY_MEDIUM -> 1.0f
            DisplayMetrics.DENSITY_HIGH -> 1.5f
            DisplayMetrics.DENSITY_XHIGH -> 2.0f
            DisplayMetrics.DENSITY_XXHIGH -> 3.0f
            DisplayMetrics.DENSITY_XXXHIGH -> 4.0f
            else -> 1.0f
        }
    }

    private val labelColorMap = mutableMapOf<String, Int>()

    /**
     * Gets a color associated with a particular label. If a color is not already assigned,
     * it generates a random color and associates it with the label for consistent coloring.
     *
     * @param label The label for which a color is required.
     * @return The color associated with the given label.
     */
    private fun getColorForLabel(label: String): Int {
        return labelColorMap.getOrPut(label) {
            // Generates a random color for the label if it doesn't exist in the map.
            Random.nextInt()
        }
    }






}

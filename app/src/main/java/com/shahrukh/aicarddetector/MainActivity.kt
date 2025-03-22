package com.shahrukh.aicarddetector

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.libexposer.CameraFrameAnalyzerFactory
import com.shahrukh.aicarddetector.libexposer.CropImageBox
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory.isImageSavedStateFlow
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory.saveBitmapToDevice
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory.uriToBitmap
import com.shahrukh.aicarddetector.libexposer.getImageUri
import com.shahrukh.aicarddetector.manager.ObjectDetectionManagerImpl
import com.shahrukh.aicarddetector.presentation.common.ImageButton
import com.shahrukh.aicarddetector.presentation.home.components.CameraOverlay
import com.shahrukh.aicarddetector.presentation.home.components.CameraPreview
import com.shahrukh.aicarddetector.presentation.home.components.PersistentCameraOverlay
import com.shahrukh.aicarddetector.presentation.home.components.RequestPermissions
import com.shahrukh.aicarddetector.presentation.utils.CameraFrameAnalyzer
import com.shahrukh.aicarddetector.presentation.utils.Constants
import com.shahrukh.aicarddetector.presentation.utils.Dimens
import com.shahrukh.aicarddetector.presentation.utils.ImageScalingUtils
import com.shahrukh.aicarddetector.ui.theme.AICardDetectorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)




       // enableEdgeToEdge()
        setContent {
            AICardDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->


                    CardDetectorScreen()
                }
            }
        }
    }
}

//Below commented code is for Real time card detection
/**@Composable
fun CardDetectorScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera states
    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var cameraFrameAnalyzer by remember { mutableStateOf<CameraFrameAnalyzer?>(null) }
    val detections = remember { mutableStateOf<List<Detection>>(emptyList()) }

    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels * 1f
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels * 1f


    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }


    RequestPermissions()
    // Initialize CameraController and FrameAnalyzer
    LaunchedEffect(Unit) {
        val objectDetectionManager = ObjectDetectionManagerImpl(context)
        CameraFrameAnalyzerFactory.init(objectDetectionManager)

        if (CameraFrameAnalyzerFactory.isInitialized()) {

            cameraFrameAnalyzer = CameraFrameAnalyzerFactory.createAICardDetector(
                onObjectDetectionResults = { detections.value = it },
                confidenceScoreState = confidenceScoreState
            )

            cameraController = ObjectDetectionManagerFactory.prepareCameraController(
                context,
                cameraFrameAnalyzer!!
            )
            cameraController?.bindToLifecycle(lifecycleOwner)
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = colorResource(id = R.color.gray_900)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(0.85f)
                //.aspectRatio(4 / 3f)

        ) {
            cameraController?.let {
                CameraPreview(
                    controller = it,
                    modifier = Modifier.fillMaxSize(),
                    onPreviewSizeChanged = { /* Handle size changes */ }
                )
            }
            //CameraOverlay(detections = detections.value)

          PersistentCameraOverlay(detections = detections.value, bottomMargin = 20f)
        }
        // Bottom column with Capture-Image and Threshold Level Slider
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.15f)
                .padding(top = Dimens.Padding8dp),
            verticalArrangement = Arrangement.SpaceAround
        ) {

            ImageButton(
                drawableResourceId = R.drawable.ic_capture,
                contentDescriptionResourceId = R.string.capture_button_description,
                modifier = Modifier
                    .size(Dimens.CaptureButtonSize)
                    .clip(CircleShape)
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        // Ensure you have a CoroutineScope (e.g., lifecycleScope or a ViewModel scope)
                        cameraController?.let {
                            CoroutineScope(Dispatchers.Main).launch {

                               /** val croppedBitmap = ObjectDetectionManagerFactory.capturePhoto(
                                    context = context,
                                    cameraController = it,
                                    screenWidth = screenWidth,
                                    screenHeight = screenHeight,
                                    detections = detections.value
                                )*/

                               // Call the capturePersistentPhoto method instead of capturePhoto
                              val croppedBitmap = ObjectDetectionManagerFactory.capturePersistentPhoto(
                                   context = context,
                                   cameraController = it,
                                   screenWidth = screenWidth,
                                   screenHeight = screenHeight,
                                   //detections = detections.value
                                   bottomMargin = 20f, // Example top margin (adjust according to your UI)
                                   previewWeight = 0.85f, // Same as you specify in your parent layout

                                  )



                                croppedBitmap?.let { bitmap ->
                                    // Use the cropped bitmap here, for example:
                                    // Show it in a UI
                                    // Save it to the device
                                    Log.d("BitmapInfo", "Width: ${bitmap.width}, Height: ${bitmap.height}")
                                    Log.d("BitmapInfo", "Config: ${bitmap.config}")
                                    Log.d("BitmapInfo", "Byte Count: ${bitmap.allocationByteCount}")

                                    Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()
                                } ?: run {
                                    Toast.makeText(context, "Failed to capture photo.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
            )



        }
    }





    }

*/
@Composable
fun CardDetectorScreen() {

    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera states
    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var cameraFrameAnalyzer by remember { mutableStateOf<CameraFrameAnalyzer?>(null) }
    val detections = remember { mutableStateOf<List<Detection>>(emptyList()) }

    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels * 1f
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels * 1f

    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }

    // State for captured image and crop box visibility
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showCropBox by remember { mutableStateOf(false) }

    RequestPermissions()
    // Initialize CameraController and FrameAnalyzer
    LaunchedEffect(Unit) {
        val objectDetectionManager = ObjectDetectionManagerImpl(context)
        CameraFrameAnalyzerFactory.init(objectDetectionManager)

        if (CameraFrameAnalyzerFactory.isInitialized()) {
            cameraFrameAnalyzer = CameraFrameAnalyzerFactory.createAICardDetector(
                onObjectDetectionResults = { detections.value = it },
                confidenceScoreState = confidenceScoreState
            )

            cameraController = ObjectDetectionManagerFactory.prepareCameraController(
                context,
                cameraFrameAnalyzer!!
            )
            cameraController?.bindToLifecycle(lifecycleOwner)
        }
    }

    // Show CropImageBox if an image is captured
    if (showCropBox && capturedBitmap != null) {
        val uri = getImageUri(context, capturedBitmap!!)
        CropImageBox(
            uri = uri,
            onCropDone = { croppedUri ->
                // Handle the cropped image URI

                val croppedBitmap = uriToBitmap(context, croppedUri)
                if(croppedBitmap != null) {

                    CoroutineScope(Dispatchers.IO).launch {
                        saveBitmapToDevice(context, croppedBitmap)
                    }
                    Toast.makeText(context, "Image cropped successfully!", Toast.LENGTH_SHORT)
                        .show()
                    // Reset states
                    capturedBitmap = null
                    showCropBox = false
                }
            },
            onNotAbleToCrop = {
                Toast.makeText(context, "Unable to crop image", Toast.LENGTH_SHORT).show()
                // Reset states
                capturedBitmap = null
                showCropBox = false
            },
            onBackPressed = {
                // Handle back press (cancel cropping)
                capturedBitmap = null
                showCropBox = false
            }
        )
    } else {
        // UI for camera preview and capture button
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.gray_900)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(0.85f)
            ) {
                cameraController?.let {
                    CameraPreview(
                        controller = it,
                        modifier = Modifier.fillMaxSize(),
                        onPreviewSizeChanged = { /* Handle size changes */ }
                    )
                }
                PersistentCameraOverlay(detections = detections.value, bottomMargin = 20f)
            }
            // Bottom column with Capture-Image and Threshold Level Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .padding(top = Dimens.Padding8dp),
                verticalArrangement = Arrangement.SpaceAround
            ) {
                ImageButton(
                    drawableResourceId = R.drawable.ic_capture,
                    contentDescriptionResourceId = R.string.capture_button_description,
                    modifier = Modifier
                        .size(Dimens.CaptureButtonSize)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            cameraController?.let {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val croppedBitmap = ObjectDetectionManagerFactory.capturePersistentPhoto(
                                        context = context,
                                        cameraController = it,
                                        screenWidth = screenWidth,
                                        screenHeight = screenHeight,
                                        bottomMargin = 20f,
                                        previewWeight = 0.85f
                                    )
                                    if (croppedBitmap != null) {
                                        capturedBitmap = croppedBitmap
                                        showCropBox = true

                                        Toast.makeText(context, "Photo captured successfully!", Toast.LENGTH_SHORT).show()


                                        // Run OCR on the captured bitmap
                                        val inputImage = InputImage.fromBitmap(capturedBitmap!!, 0)

                                        textRecognizer.process(inputImage)
                                            .addOnSuccessListener { visionText ->
                                                // Handle recognized text
                                                val detectedText = visionText.text
                                                Log.d("OCR", "Recognized Text: $detectedText")

                                                Toast.makeText(context, "Text: $detectedText", Toast.LENGTH_LONG).show()
                                            }
                                            .addOnFailureListener { e ->
                                                // Handle failure
                                                Log.e("OCR", "Text recognition failed", e)
                                                Toast.makeText(context, "Failed to recognize text.", Toast.LENGTH_SHORT).show()
                                            }


                                    } else {
                                        Toast.makeText(context, "Failed to capture photo.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}




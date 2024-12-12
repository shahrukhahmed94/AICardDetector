package com.shahrukh.aicarddetector

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
import androidx.compose.ui.zIndex
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.libexposer.CameraFrameAnalyzerFactory
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory.isImageSavedStateFlow
import com.shahrukh.aicarddetector.manager.ObjectDetectionManagerImpl
import com.shahrukh.aicarddetector.presentation.common.ImageButton
import com.shahrukh.aicarddetector.presentation.home.components.CameraOverlay
import com.shahrukh.aicarddetector.presentation.home.components.CameraPreview
import com.shahrukh.aicarddetector.presentation.home.components.RequestPermissions
import com.shahrukh.aicarddetector.presentation.utils.CameraFrameAnalyzer
import com.shahrukh.aicarddetector.presentation.utils.Constants
import com.shahrukh.aicarddetector.presentation.utils.Dimens
import com.shahrukh.aicarddetector.presentation.utils.ImageScalingUtils
import com.shahrukh.aicarddetector.ui.theme.AICardDetectorTheme

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



       // enableEdgeToEdge()
        setContent {
            AICardDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    /**Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )*/

                    CardDetectorScreen()
                }
            }
        }
    }
}

@Composable
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

        ) {
            cameraController?.let {
                CameraPreview(
                    controller = it,
                    modifier = Modifier.fillMaxSize(),
                    onPreviewSizeChanged = { /* Handle size changes */ }
                )
            }
            CameraOverlay(detections = detections.value)
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
                        // Capture and Saves Photo

                        cameraController?.let {
                            ObjectDetectionManagerFactory.capturePhoto(
                                context = context,
                                cameraController = it,
                                screenWidth,
                                screenHeight,
                                detections.value
                            )
                        }


                        // Show toast of Save State

                    }
            )


        }
    }





    }




/**@Composable
fun CardDetectorScreen() {

    val context = LocalContext.current

    var isPermissionsGranted by remember { mutableStateOf(false) }

    // Request permissions and handle the result
    RequestPermissions()

    // Preparing Image Analyzer using the factory
    // Initialize the ObjectDetectionManager
    val objectDetectionManager = ObjectDetectionManagerImpl(context)
    // Initialize the CameraFrameAnalyzerFactory
    // State to track if CameraFrameAnalyzerFactory has been initialized
    var isFactoryInitialized by remember { mutableStateOf(false) }

    // State to keep track of the preview size of the camera feed
    val previewSizeState = remember { mutableStateOf(IntSize(0, 0)) }

    // State for the confidence score, influenced by the user through a slider
    val confidenceScoreState = remember { mutableFloatStateOf(Constants.INITIAL_CONFIDENCE_SCORE) }


    // Scale factors to translate coordinates from the detected image to the preview size
    var scaleFactorX = 1f
    var scaleFactorY = 1f

    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels * 1f
    val screenHeight = LocalContext.current.resources.displayMetrics.heightPixels * 1f

    // Global state for cameraFrameAnalyzer and cameraController
    var cameraFrameAnalyzer by remember { mutableStateOf<CameraFrameAnalyzer?>(null) }
    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }




    var detections2 by remember {
        mutableStateOf(emptyList<com.shahrukh.aicarddetector.domain.model.Detection>())
    }

    LaunchedEffect(detections2) {}

    LaunchedEffect(Unit) {
        Log.d("CameraFactory", "Initializing CameraFrameAnalyzerFactory")
        CameraFrameAnalyzerFactory.init(objectDetectionManager)

        if (CameraFrameAnalyzerFactory.isInitialized()) {
            Log.d("CameraFactory", "CameraFrameAnalyzerFactory initialized successfully.")
            isFactoryInitialized = true
            cameraFrameAnalyzer = CameraFrameAnalyzerFactory.createAICardDetector(
                onObjectDetectionResults = { detections2 = it },
                confidenceScoreState = confidenceScoreState
            )


            cameraController = ObjectDetectionManagerFactory.prepareCameraController(
                context,

                cameraFrameAnalyzer!!
            )

            // Ensure cameraController is successfully initialized
            cameraController?.let {
                Log.d("CameraFactory", "CameraController initialized successfully.")

                // Now, create the Preview and ImageAnalysis use cases and bind them



            } ?: Log.e("CameraFactory", "Failed to initialize CameraController.")
        } else {
            Log.e("CameraFactory", "Failed to initialize CameraFrameAnalyzerFactory.")
        }
    }



        Box(
        modifier = Modifier.fillMaxSize()
    ) {



        // Combined Column for Camera Preview, CameraOverlay & Bottom UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(color = colorResource(id = R.color.gray_900)),
        ) {
            // Camera Preview Column with CameraOverlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
            ) {
                // Camera Preview
                remember {
                    cameraController
                }?.let {
                    CameraPreview(
                        controller = it  ,
                        modifier = Modifier.fillMaxSize(),
                        onPreviewSizeChanged = { newSize ->
                            previewSizeState.value = newSize

                            // Get Scale-Factors along X and Y depending on size of camera-preview
                            val scaleFactors = ImageScalingUtils.getScaleFactors(
                                newSize.width,
                                newSize.height
                            )

                            scaleFactorX = scaleFactors[0]
                            scaleFactorY = scaleFactors[1]

                            Log.d(
                                "HomeViewModel",
                                "HomeScreen() called with: newSize = $scaleFactorX & scaleFactorY = $scaleFactorY"
                            )
                        }
                    )
                }

                // Add CameraOverlay here so it overlays on top of CameraPreview



                com.shahrukh.aicarddetector.presentation.home.components.CameraOverlay(detections = detections2)
                /**CameraOverlay(detections = detections) */
            }

            // Bottom column with Capture-Image and Threshold Level Slider
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.2f)
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
                            // Capture and Saves Photo
                            /** viewModel.capturePhoto(
                            context = context,
                            navController = navController,
                            cameraController = cameraController,
                            screenWidth,
                            screenHeight,
                            detections
                            )*/

                            cameraController?.let {
                                ObjectDetectionManagerFactory.capturePhoto(
                                    context = context,
                                    //  navController = navController,
                                    cameraController = it,
                                    screenWidth,
                                    screenHeight,
                                    detections2
                                )
                            }


                            // Show toast of Save State

                        }
                )


            }
        }

        // Column with rotate-camera and detected object count Composable (Overlapping UI)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1f)
                .padding(top = Dimens.Padding32dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {


                // Detected Object Count Composable
                /**  ObjectCounter(objectCount = detections.size) */
                com.shahrukh.aicarddetector.presentation.home.components.ObjectCounter(objectCount = detections2.size)
            }
        }



    }

}
*/
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AICardDetectorTheme {
        Greeting("Android")
    }
}
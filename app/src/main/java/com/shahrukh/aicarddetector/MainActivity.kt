package com.shahrukh.aicarddetector

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.shahrukh.aicarddetector.presentation.home.components.CameraPreview
import com.shahrukh.aicarddetector.presentation.utils.Constants
import com.shahrukh.aicarddetector.presentation.utils.Dimens
import com.shahrukh.aicarddetector.presentation.utils.ImageScalingUtils
import com.shahrukh.aicarddetector.ui.theme.AICardDetectorTheme

class MainActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        enableEdgeToEdge()
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


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        var detections by remember {
            mutableStateOf(emptyList<Detection>())
        }



        LaunchedEffect(detections) {}


// Initialize the CameraFrameAnalyzerFactory inside LaunchedEffect
        LaunchedEffect(Unit) {
            CameraFrameAnalyzerFactory.init(objectDetectionManager)
            isFactoryInitialized =
                CameraFrameAnalyzerFactory.isInitialized() // Check initialization
        }

        val cameraFrameAnalyzer = remember {
            if (isFactoryInitialized) {
                CameraFrameAnalyzerFactory.createAICardDetector(
                    onObjectDetectionResults = {
                        detections = it
                    },
                    confidenceScoreState = confidenceScoreState
                )
            } else {
                null // Handle the case where factory isn't initialized yet
            }
        }







        // Combined Column for Camera Preview, CameraOverlay & Bottom UI







    }

}

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
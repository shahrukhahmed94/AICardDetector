# AiCardDetector & Recognition - Android Card Detection with CameraX and TensorFlow Lite)

AI Card Detector is a real-time card detection library designed to detect and process cards using TensorFlow Lite and CameraX. It allows seamless integration of card detection capabilities into your applications for KYC (Know Your Customer) purposes and other identity verification scenarios. The library is lightweight, fast, and optimized for mobile environments.

## Demo 

[Watch the Demo Video](https://1drv.ms/v/c/df5a2072805306a7/EXUdAFVD9OZKi9d1vHWOmK8BCFX56RKB6GTY8asDN6PWfQ)

## Features

- Real-time card detection using TensorFlow Lite.
- Supports integration with CameraX for capturing real-time images.
- Detects various types of cards, such as IDs, passports, and driver’s licenses.
- Optimized for Android applications.

## Installation

Follow these steps to integrate the AI Card Detector library into your Android project.

### Step 1: Add the JitPack Repository

In your root `build.gradle` file, add the JitPack repository at the end of the `repositories` section:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

 This will allow you to access dependencies hosted on JitPack.

### Step2: Add the Dependency

In your app's build.gradle file, add the following dependency:

```
dependencies {
    implementation 'com.github.shahrukhahmed94:AICardDetector:1.0.3'
}
```

Make sure you sync your project with Gradle after adding the dependency to complete the integration.

## Example Usage

Here’s how you can integrate AI Card Detector into your main activity to capture and process card images. This example demonstrates using CameraX to detect cards in real-time, display detections on a camera preview, and allow users to capture images.

## Step 1: Setup MainActivity

```
package com.shahrukh.aicarddetector

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.libexposer.CameraFrameAnalyzerFactory
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory
import com.shahrukh.aicarddetector.manager.ObjectDetectionManagerImpl
import com.shahrukh.aicarddetector.presentation.common.ImageButton
import com.shahrukh.aicarddetector.presentation.home.components.CameraOverlay
import com.shahrukh.aicarddetector.presentation.home.components.CameraPreview
import com.shahrukh.aicarddetector.presentation.home.components.RequestPermissions
import com.shahrukh.aicarddetector.presentation.utils.Constants
import com.shahrukh.aicarddetector.ui.theme.AICardDetectorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AICardDetectorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
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
                        // Capture image logic
                        cameraController?.let {
                            CoroutineScope(Dispatchers.Main).launch {
                                val croppedBitmap = ObjectDetectionManagerFactory.capturePhoto(
                                    context = context,
                                    cameraController = it,
                                    screenWidth = screenWidth,
                                    screenHeight = screenHeight,
                                    detections = detections.value
                                )

                                croppedBitmap?.let { bitmap ->
                                    // Process the captured image, e.g., save it or display it
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
```

## Step 2: Handle Permissions

Ensure that your app requests the necessary camera permissions by including the following in your AndroidManifest.xml:

```
<uses-permission android:name="android.permission.CAMERA"/>
<uses-feature android:name="android.hardware.camera.any" android:required="true"/>
```

## Explanation

- Camera Setup: We use CameraX's LifecycleCameraController to bind the camera to the lifecycle of the activity and display the camera feed using the CameraPreview composable.
- Card Detection: The real-time card detection is handled by initializing the CameraFrameAnalyzer and ObjectDetectionManager, which process each camera frame to detect cards.
- Capture Image: When the user clicks the capture button, it processes the current frame, crops the detected card region, and logs the captured image's details.
By following these steps, you’ll have a functional real-time card detection system integrated into your Android application.

## Contributions

We welcome contributions to improve the AI Card Detector library! If you would like to contribute, please follow the steps below:

### How to Contribute

- Fork the repository: Click the "Fork" button on the top-right of this page to create a copy of this repository in your own GitHub account.

- Clone your fork: Clone your forked repository to your local machine using the following command:

```
git clone https://github.com/your-username/AICardDetector.git
```

- Create a branch: Create a new branch for your changes. You can do this by running:
```
git checkout -b feature-branch-name

```

- Make changes: Implement your changes or fixes. Be sure to follow the coding style and conventions used in the project.

- Commit your changes: Once you’ve made your changes, commit them with a descriptive message:

```
git commit -m "Describe your changes here"

```
- Push your changes: Push your changes to your forked repository:

```

git push origin feature-branch-name

```
- Create a Pull Request: Go to the original repository on GitHub and open a pull request from your feature branch to the main branch of the original repository.

## Code of Conduct

Please adhere to the following guidelines when contributing to this project:

- Be respectful: Treat others with respect and courtesy. Our community thrives when everyone feels welcome.
- Follow project guidelines: Ensure your code aligns with the project's style and structure. Follow any contribution-specific guidelines in the project.
- Write tests: If applicable, include tests to cover your changes, ensuring they don’t break existing functionality.
- Document your changes: Update documentation (README, comments, etc.) if your changes affect how the library works or its usage.

## Reporting Issues

If you find a bug or have an enhancement request, feel free to open an issue in the repository. Please provide as much detail as possible, including:

- Steps to reproduce the issue.
- Device information (model, OS version).
- Log files or error messages (if any).
- Screenshots (if applicable).

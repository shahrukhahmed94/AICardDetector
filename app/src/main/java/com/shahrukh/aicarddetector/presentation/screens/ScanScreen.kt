package com.shahrukh.aicarddetector.presentation.screens

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.view.LifecycleCameraController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.shahrukh.aicarddetector.R
import com.shahrukh.aicarddetector.domain.model.Detection
import com.shahrukh.aicarddetector.libexposer.CameraFrameAnalyzerFactory
import com.shahrukh.aicarddetector.libexposer.ObjectDetectionManagerFactory
import com.shahrukh.aicarddetector.manager.ObjectDetectionManagerImpl
import java.util.Locale
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.ScanViewModel
import com.shahrukh.aicarddetector.presentation.home.components.CameraPreview
import com.shahrukh.aicarddetector.presentation.home.components.PersistentCameraOverlay
import com.shahrukh.aicarddetector.presentation.home.components.RequestPermissions
import com.shahrukh.aicarddetector.presentation.utils.CameraFrameAnalyzer
import com.shahrukh.aicarddetector.presentation.utils.Constants
import com.shahrukh.aicarddetector.presentation.utils.Dimens
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    RequestPermissions()

    val threshold by viewModel.confidenceThreshold.collectAsState()
    val autoCapture by viewModel.autoCaptureEnabled.collectAsState()
    val stabilizationProgress by viewModel.stabilizationProgress.collectAsState()
    val activeDetection by viewModel.activeDetection.collectAsState()
    val torchEnabled by viewModel.torchEnabled.collectAsState()

    var cameraController by remember { mutableStateOf<LifecycleCameraController?>(null) }
    var cameraFrameAnalyzer by remember { mutableStateOf<CameraFrameAnalyzer?>(null) }

    val confidenceScoreState = remember { mutableStateOf(threshold) }

    val screenWidth = context.resources.displayMetrics.widthPixels * 1f
    val screenHeight = context.resources.displayMetrics.heightPixels * 1f

    // Sync threshold from slider to frame analyzer state
    LaunchedEffect(threshold) {
        confidenceScoreState.value = threshold
    }

    // Initialize Camera
    LaunchedEffect(Unit) {
        val objectDetectionManager = ObjectDetectionManagerImpl(context)
        CameraFrameAnalyzerFactory.init(objectDetectionManager)

        if (CameraFrameAnalyzerFactory.isInitialized()) {
            cameraFrameAnalyzer = CameraFrameAnalyzerFactory.createAICardDetector(
                onObjectDetectionResults = { detections ->
                    viewModel.onDetectionsUpdated(detections)
                },
                confidenceScoreState = confidenceScoreState
            )

            cameraController = ObjectDetectionManagerFactory.prepareCameraController(
                context,
                cameraFrameAnalyzer!!
            )
            cameraController?.bindToLifecycle(lifecycleOwner)
        }
    }

    // Sync Torch (Flashlight) state
    LaunchedEffect(torchEnabled) {
        cameraController?.enableTorch(torchEnabled)
    }

    // Function to perform document capture
    fun capturePhoto() {
        // IMPORTANT: Snapshot detection state NOW before async photo capture,
        // because activeDetection may become null by the time the bitmap is ready.
        val detectionSnapshot = activeDetection
        cameraController?.let { controller ->
            CoroutineScope(Dispatchers.Main).launch {
                val croppedBitmap = ObjectDetectionManagerFactory.capturePersistentPhoto(
                    context = context,
                    cameraController = controller,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    bottomMargin = 20f,
                    previewWeight = 0.85f
                )
                if (croppedBitmap != null) {
                    viewModel.processCapturedCard(
                        croppedBitmap,
                        detectionSnapshot?.detectedObjectName,
                        detectionSnapshot?.confidenceScore
                    )
                } else {
                    Toast.makeText(context, "Capture failed. Please align your card.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Listen to auto-capture requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.captureRequests.collectLatest {
            capturePhoto()
        }
    }

    val backgroundDark = Color(0xFF0A0E1A)
    val surfaceDark = Color(0xFF161F30)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SCANNER NODE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigate(AppScreen.DASHBOARD) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Flashlight toggle
                    IconButton(onClick = { viewModel.torchEnabled.value = !torchEnabled }) {
                        Icon(
                            painter = painterResource(
                                id = if (torchEnabled) R.drawable.ic_capture else R.drawable.ic_capture // Use standard drawables, let's keep it safe
                            ),
                            contentDescription = "Toggle Torch",
                            tint = if (torchEnabled) Color(0xFF00FFCC) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = backgroundDark
                )
            )
        },
        containerColor = backgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Live Preview Box (85% height)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f)
            ) {
                cameraController?.let { controller ->
                    CameraPreview(
                        controller = controller,
                        modifier = Modifier.fillMaxSize(),
                        onPreviewSizeChanged = {}
                    )
                }

                // Cyberpunk overlay guides
                PersistentCameraOverlay(detections = emptyList(), bottomMargin = 20f)

                // Intelligent overlay HUD text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 40.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val detection = activeDetection
                    val statusText = if (detection != null) {
                        val confPercent = (detection.confidenceScore * 100).toInt()
                        val cardName = when (detection.detectedObjectName.lowercase(Locale.ROOT)) {
                            "id_card" -> "ID Card"
                            "passport" -> "Passport"
                            "drivers_license" -> "Driver's License"
                            else -> detection.detectedObjectName
                        }
                        "$cardName Detected ($confPercent%)"
                    } else {
                        "ALIGN CARD WITHIN GUIDE"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x990A0E1A))
                            .border(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = if (detection != null) Color(0xFF00FFCC) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto capture stabilizer progress bar
                    if (autoCapture && stabilizationProgress > 0f) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(200.dp)
                        ) {
                            Text(
                                "Stabilizing document...",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = stabilizationProgress,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF00FFCC),
                                trackColor = Color.White.copy(alpha = 0.2f),
                            )
                        }
                    }
                }
            }

            // Bottom Panel (15% height)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f)
                    .background(surfaceDark)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Column: Slider controls for threshold
                Column(
                    modifier = Modifier.weight(0.6f)
                ) {
                    Text(
                        "CONFIDENCE THRESHOLD: ${(threshold * 100).toInt()}%",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Slider(
                        value = threshold,
                        onValueChange = { viewModel.confidenceThreshold.value = it },
                        valueRange = 0.3f..0.9f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FFCC),
                            activeTrackColor = Color(0xFF00FFCC),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }

                // Shutter capture button on the right
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
                            )
                        )
                        .clickable { capturePhoto() }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_capture),
                        contentDescription = "Capture",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

package com.shahrukh.aicarddetector.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ScanViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val autoCapture by viewModel.autoCaptureEnabled.collectAsState()
    val soundFeedback by viewModel.soundFeedbackEnabled.collectAsState()
    val hapticFeedback by viewModel.hapticFeedbackEnabled.collectAsState()
    val defaultThreshold by viewModel.confidenceThreshold.collectAsState()

    var showClearConfirm by remember { mutableStateOf(false) }

    val backgroundDark = Color(0xFF0A0E1A)
    val surfaceDark = Color(0xFF161F30)
    val neonCyan = Color(0xFF00FFCC)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SYSTEM PREFERENCES",
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "CAMERA CAPTURE ENGINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp
            )

            // Auto-Capture Toggle
            SettingsToggleItem(
                title = "Hands-free Auto Capture",
                subtitle = "Triggers capture automatically once card stabilizes in cyan frame guide.",
                checked = autoCapture,
                onCheckedChange = { viewModel.autoCaptureEnabled.value = it },
                surfaceColor = surfaceDark,
                activeColor = neonCyan
            )

            // Audio & Haptic preferences
            Text(
                "FEEDBACK & DIAGNOSTICS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Sound feedback Toggle
            SettingsToggleItem(
                title = "Audio Scan Beep Alert",
                subtitle = "Play programmatic tone beep upon card capture success.",
                checked = soundFeedback,
                onCheckedChange = { viewModel.soundFeedbackEnabled.value = it },
                surfaceColor = surfaceDark,
                activeColor = neonCyan
            )

            // Haptic vibration Toggle
            SettingsToggleItem(
                title = "Haptic Vibration Tick",
                subtitle = "Trigger tactile vibration feedback when a document is verified.",
                checked = hapticFeedback,
                onCheckedChange = { viewModel.hapticFeedbackEnabled.value = it },
                surfaceColor = surfaceDark,
                activeColor = neonCyan
            )

            // Reset Database Section
            Text(
                "STORAGE MAINTENANCE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.4f),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Clear scans database item
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(surfaceDark)
                    .clickable { showClearConfirm = true }
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color(0xFFFF5E62),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Clear Scanning Logs Database",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                    Text(
                        "Deletes all stored metadata history and crops. This action is permanent.",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Application Build Info Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "KRYPTON AI ENGINE • VERSION 1.0.3",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.3f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    "TensorFlow Lite + CameraX Framework",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            }
        }
    }

    // Clear Database Confirm Dialog
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear scans history log?", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("This will permanently delete all scan details and cropped card image files from storage.", color = Color.White.copy(alpha = 0.7f)) },
            containerColor = surfaceDark,
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllScans()
                        showClearConfirm = false
                    }
                ) {
                    Text("YES, CLEAR ALL", color = Color(0xFFFF5E62), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("CANCEL", color = Color.White)
                }
            }
        )
    }
}

@Composable
fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    surfaceColor: Color,
    activeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(0.8f)
        ) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 15.sp
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
                lineHeight = 16.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = activeColor,
                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

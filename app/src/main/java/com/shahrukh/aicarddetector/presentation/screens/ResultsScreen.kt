package com.shahrukh.aicarddetector.presentation.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.DocumentType
import com.shahrukh.aicarddetector.presentation.OcrSimState
import com.shahrukh.aicarddetector.presentation.ScanViewModel

@Composable
fun ResultsScreen(
    viewModel: ScanViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val bitmap by viewModel.capturedBitmap.collectAsState()
    val label by viewModel.capturedLabel.collectAsState()
    val confidence by viewModel.capturedConfidence.collectAsState()

    val ocrState by viewModel.ocrSimState.collectAsState()
    val ocrProgressText by viewModel.ocrProgressText.collectAsState()

    val parsedData by viewModel.parsedDocumentData.collectAsState()

    val backgroundDark = Color(0xFF0A0E1A)
    val surfaceDark = Color(0xFF161F30)
    val neonCyan = Color(0xFF00FFCC)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundDark)
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Top Section: Screen Title (fixed) ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 20.dp, bottom = 12.dp)
        ) {
            Text(
                "VERIFICATION DECODER",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "AI Document Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // ── Middle Section: Scrollable content (takes remaining space) ──
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cropped Card Display with Neon glowing border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(neonCyan, Color(0xFF4FACFE))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .background(surfaceDark),
                contentAlignment = Alignment.Center
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Cropped Card",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("NO IMAGE CACHE", color = Color.White.copy(alpha = 0.4f))
                }
            }

            // Animated Terminal Progress
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF060913))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    "ANALYZER TERMINAL",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = neonCyan.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))

                TerminalLine(
                    task = "CARD_ALIGNMENT_CHECK",
                    isComplete = ocrState != OcrSimState.ANALYZING,
                    isActive = ocrState == OcrSimState.ANALYZING,
                    neonColor = neonCyan
                )
                TerminalLine(
                    task = "TEXT_DECRYPTION_OCR",
                    isComplete = ocrState == OcrSimState.AUTHENTICATING || ocrState == OcrSimState.COMPLETED,
                    isActive = ocrState == OcrSimState.READING,
                    neonColor = neonCyan
                )
                TerminalLine(
                    task = "AUTHENTICITY_VERIFICATION",
                    isComplete = ocrState == OcrSimState.COMPLETED,
                    isActive = ocrState == OcrSimState.AUTHENTICATING,
                    neonColor = neonCyan
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (ocrState != OcrSimState.COMPLETED) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = neonCyan
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = neonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        text = ocrProgressText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Details Pane — Expandable, context-aware fields
            AnimatedVisibility(
                visible = ocrState == OcrSimState.COMPLETED,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceDark)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label.uppercase(),
                            fontWeight = FontWeight.ExtraBold,
                            color = neonCyan,
                            fontSize = 14.sp,
                            letterSpacing = 1.sp
                        )
                        val score = (confidence * 100).toInt()
                        Text(
                            "MATCH: $score%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Render fields based on document type
                    when (parsedData.documentType) {
                        DocumentType.CREDIT_DEBIT_CARD -> {
                            if (parsedData.cardNetwork.isNotEmpty()) {
                                KycDataRow(label = "CARD NETWORK", value = parsedData.cardNetwork)
                                KycDivider()
                            }
                            KycDataRow(label = "CARD NUMBER", value = parsedData.cardNumber.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "CARDHOLDER NAME", value = parsedData.holderName)
                            KycDivider()
                            KycDataRow(label = "VALID THRU", value = parsedData.cardExpiry.ifEmpty { "Not detected" })
                        }

                        DocumentType.PASSPORT -> {
                            KycDataRow(label = "FULL NAME", value = parsedData.holderName)
                            KycDivider()
                            KycDataRow(label = "PASSPORT NO.", value = parsedData.documentNumber.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "NATIONALITY", value = parsedData.nationality.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "DATE OF BIRTH", value = parsedData.dateOfBirth.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "EXPIRY DATE", value = parsedData.expiryDate.ifEmpty { "Not detected" })
                            if (parsedData.gender.isNotEmpty() && parsedData.gender != "Not detected") {
                                KycDivider()
                                KycDataRow(label = "GENDER", value = parsedData.gender)
                            }
                        }

                        DocumentType.DRIVERS_LICENSE -> {
                            KycDataRow(label = "FULL NAME", value = parsedData.holderName)
                            KycDivider()
                            KycDataRow(label = "LICENSE NO.", value = parsedData.documentNumber.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "DATE OF BIRTH", value = parsedData.dateOfBirth.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "EXPIRY DATE", value = parsedData.expiryDate.ifEmpty { "Not detected" })
                            if (parsedData.licenseClass.isNotEmpty() && parsedData.licenseClass != "Not detected") {
                                KycDivider()
                                KycDataRow(label = "LICENSE CLASS", value = parsedData.licenseClass)
                            }
                        }

                        DocumentType.ID_CARD -> {
                            KycDataRow(label = "FULL NAME", value = parsedData.holderName)
                            KycDivider()
                            KycDataRow(label = "ID NUMBER", value = parsedData.documentNumber.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "DATE OF BIRTH", value = parsedData.dateOfBirth.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "EXPIRY DATE", value = parsedData.expiryDate.ifEmpty { "Not detected" })
                            if (parsedData.nationality.isNotEmpty()) {
                                KycDivider()
                                KycDataRow(label = "NATIONALITY", value = parsedData.nationality)
                            }
                            if (parsedData.gender.isNotEmpty()) {
                                KycDivider()
                                KycDataRow(label = "GENDER", value = parsedData.gender)
                            }
                        }

                        DocumentType.UNKNOWN -> {
                            KycDataRow(label = "HOLDER NAME", value = parsedData.holderName)
                            KycDivider()
                            KycDataRow(label = "DOCUMENT ID", value = parsedData.documentNumber.ifEmpty { "Not detected" })
                            KycDivider()
                            KycDataRow(label = "EXPIRY DATE", value = parsedData.expiryDate.ifEmpty { "Not detected" })
                        }
                    }
                }
            }

            // Bottom spacer so scroll content doesn't stick to buttons
            Spacer(modifier = Modifier.height(4.dp))
        }

        // ── Bottom Section: Action Buttons (fixed, never shrinks) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Discard/Retake Button
            OutlinedButton(
                onClick = { viewModel.navigateTo(AppScreen.SCAN) },
                modifier = Modifier
                    .weight(0.4f)
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("RETAKE", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            // Save Button (only enabled when verification completes)
            val btnColor = Brush.horizontalGradient(
                colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
            )
            val isComplete = ocrState == OcrSimState.COMPLETED
            
            Box(
                modifier = Modifier
                    .weight(0.6f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isComplete) btnColor else Brush.horizontalGradient(
                            colors = listOf(Color.Gray.copy(alpha = 0.2f), Color.Gray.copy(alpha = 0.2f))
                        )
                    )
                    .clickable(enabled = isComplete) { viewModel.saveScanResult() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "SAVE SCAN RECORD",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isComplete) Color.White else Color.White.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun KycDivider() {
    Divider(
        color = Color.White.copy(alpha = 0.05f),
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun TerminalLine(
    task: String,
    isComplete: Boolean,
    isActive: Boolean,
    neonColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusText = when {
            isComplete -> "[ SUCCESS ]"
            isActive -> "[ RUNNING ]"
            else -> "[ PENDING ]"
        }
        val statusColor = when {
            isComplete -> neonColor
            isActive -> Color(0xFF4FACFE)
            else -> Color.White.copy(alpha = 0.2f)
        }

        Text(
            text = "$statusText $task",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun KycDataRow(label: String, value: String) {
    Column {
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )
        Text(
            value,
            fontSize = 15.sp,
            color = if (value == "Not detected") Color.White.copy(alpha = 0.3f) else Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

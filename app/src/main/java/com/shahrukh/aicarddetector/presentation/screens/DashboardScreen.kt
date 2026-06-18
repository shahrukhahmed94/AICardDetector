package com.shahrukh.aicarddetector.presentation.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: ScanViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val totalScans by viewModel.totalScansCount.collectAsState()
    val avgConfidence by viewModel.averageConfidence.collectAsState()
    val scansMap by viewModel.scansByCardType.collectAsState()

    val primaryGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE))
    )

    val backgroundDark = Color(0xFF0A0E1A) // Cyberpunk dark midnight blue
    val surfaceDark = Color(0xFF161F30) // Sleek slate blue

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "KRYPTON AI",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { onNavigate(AppScreen.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF00FFCC)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(primaryGradient)
                        .clickable { onNavigate(AppScreen.SCAN) }
                ) {
                    // Futuristic grid overlay or subtle glow
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "AI Card Detector",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                "KYC & Document Verification",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // Pulsing button hint
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1f,
                            targetValue = 1.05f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Row(
                            modifier = Modifier
                                .scale(pulseScale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "TAP TO START SCAN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Statistics Grid Title
            item {
                Text(
                    "SYSTEM STATISTICS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.5.sp
                )
            }

            // Stats row 1: Counters
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatsCard(
                        title = "TOTAL SCANS",
                        value = totalScans.toString(),
                        subtitle = "Documents saved",
                        modifier = Modifier.weight(1f),
                        surfaceColor = surfaceDark
                    )
                    
                    val confPercentage = (avgConfidence * 100).toInt()
                    StatsCard(
                        title = "AVG ACCURACY",
                        value = "$confPercentage%",
                        subtitle = "Confidence score",
                        modifier = Modifier.weight(1f),
                        surfaceColor = surfaceDark,
                        valueColor = if (avgConfidence > 0.8f) Color(0xFF00FFCC) else Color(0xFFFF5E62)
                    )
                }
            }

            // Stats row 2: Card Type Breakdown
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceDark)
                        .padding(20.dp)
                ) {
                    Text(
                        "DOCUMENT TYPES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val docTypes = listOf(
                        Triple("ID Card", scansMap["ID Card"] ?: 0, Color(0xFF00FFCC)),
                        Triple("Passport", scansMap["Passport"] ?: 0, Color(0xFFE0C3FC)),
                        Triple("Driver's License", scansMap["Driver's License"] ?: 0, Color(0xFF4FACFE))
                    )

                    docTypes.forEachIndexed { index, (label, count, color) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, color = Color.White, fontSize = 14.sp)
                            }
                            Text(
                                "$count verified",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (index < docTypes.lastIndex) {
                            Divider(color = Color.White.copy(alpha = 0.05f))
                        }
                    }
                }
            }

            // Onboarding/Guideline Cards
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(surfaceDark)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00FFCC)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "OPTIMAL SCANNING TIPS",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "• Ensure good ambient lighting.\n" +
                        "• Keep your document aligned within the cyan guides.\n" +
                        "• Avoid camera glare or shadows on the card.\n" +
                        "• Enable auto-capture in settings for hands-free scanning.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = 22.sp
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    surfaceColor: Color,
    valueColor: Color = Color.White
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .padding(20.dp)
    ) {
        Text(
            title,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.4f),
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = valueColor
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subtitle,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

package com.shahrukh.aicarddetector.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.shahrukh.aicarddetector.data.ScanEntity
import com.shahrukh.aicarddetector.presentation.AppScreen
import com.shahrukh.aicarddetector.presentation.ScanViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ScanViewModel,
    onNavigate: (AppScreen) -> Unit
) {
    val scans by viewModel.filteredScans.collectAsState()
    val query by viewModel.searchQuery.collectAsState()
    val filterType by viewModel.selectedCardTypeFilter.collectAsState()

    var selectedScanForDetail by remember { mutableStateOf<ScanEntity?>(null) }

    val backgroundDark = Color(0xFF0A0E1A)
    val surfaceDark = Color(0xFF161F30)
    val neonCyan = Color(0xFF00FFCC)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "SCAN LOG DATABASE",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
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
                .padding(horizontal = 20.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                placeholder = { Text("Search by name, ID or label...", color = Color.White.copy(alpha = 0.4f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.6f)) },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = neonCyan,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = surfaceDark,
                    unfocusedContainerColor = surfaceDark
                ),
                singleLine = true
            )

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    "All" to null,
                    "ID Card" to "ID Card",
                    "Passport" to "Passport",
                    "Driver's License" to "Driver's License"
                )

                filters.forEach { (label, value) ->
                    val isSelected = filterType == value
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedCardTypeFilter.value = value },
                        label = { Text(label, color = if (isSelected) Color.Black else Color.White) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = neonCyan,
                            containerColor = surfaceDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.White.copy(alpha = 0.05f),
                            selectedBorderColor = neonCyan
                        )
                    )
                }
            }

            // Scans List
            if (scans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "NO LOG RECORDS FOUND",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.3f),
                        letterSpacing = 1.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(scans, key = { it.id }) { scan ->
                        ScanLogItem(
                            scan = scan,
                            onClick = { selectedScanForDetail = scan },
                            onDelete = { viewModel.deleteScanItem(scan) },
                            surfaceColor = surfaceDark,
                            accentColor = neonCyan
                        )
                    }
                }
            }
        }
    }

    // Details Dialog overlay
    selectedScanForDetail?.let { scan ->
        ScanDetailsDialog(
            scan = scan,
            onDismiss = { selectedScanForDetail = null },
            surfaceColor = surfaceDark,
            neonColor = neonCyan
        )
    }
}

@Composable
fun ScanLogItem(
    scan: ScanEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    surfaceColor: Color,
    accentColor: Color
) {
    val imageBitmap = remember(scan.imagePath) {
        try {
            BitmapFactory.decodeFile(scan.imagePath)
        } catch (e: Exception) {
            null
        }
    }

    val dateFormatted = remember(scan.timestamp) {
        val date = Date(scan.timestamp)
        val format = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        format.format(date)
    }

    // Determine the appropriate subtitle based on document type
    val isFinancialCard = isFinancialDocType(scan.documentType, scan.label)
    val subtitleLabel = if (isFinancialCard) "Card:" else "ID:"
    val subtitleValue = scan.mockNumber

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(surfaceColor)
            .border(1.dp, Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("ERR", color = Color.Red, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                scan.label.uppercase(Locale.ROOT),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.5.sp
            )
            Text(
                scan.mockName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "$subtitleLabel ${scan.mockNumber}",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Text(
                dateFormatted,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Action Column: Confidence score + Delete button
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight()
        ) {
            val scorePercent = (scan.confidence * 100).toInt()
            Text(
                "$scorePercent%",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (scan.confidence > 0.8f) Color(0xFF00FFCC) else Color(0xFFFF5E62)
            )
            Spacer(modifier = Modifier.height(12.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ScanDetailsDialog(
    scan: ScanEntity,
    onDismiss: () -> Unit,
    surfaceColor: Color,
    neonColor: Color
) {
    val imageBitmap = remember(scan.imagePath) {
        try {
            BitmapFactory.decodeFile(scan.imagePath)
        } catch (e: Exception) {
            null
        }
    }

    val isFinancialCard = isFinancialDocType(scan.documentType, scan.label)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColor)
                .border(1.dp, neonColor.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Title Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOGGED SCANNED DATA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.6f))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Document Crop Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Context-Aware Extracted Info
            KycDetailsBlock(label = "DOCUMENT TYPE", value = scan.label, neonColor = neonColor)

            when {
                isFinancialCard -> {
                    // Credit/Debit Card layout
                    if (scan.cardNetwork.isNotEmpty()) {
                        KycDetailsBlock(label = "CARD NETWORK", value = scan.cardNetwork, neonColor = neonColor)
                    }
                    KycDetailsBlock(label = "CARD NUMBER", value = scan.mockNumber, neonColor = neonColor)
                    KycDetailsBlock(label = "CARDHOLDER NAME", value = scan.mockName, neonColor = neonColor)
                    KycDetailsBlock(label = "VALID THRU", value = scan.mockExpiry, neonColor = neonColor)
                }
                scan.documentType == "PASSPORT" -> {
                    // Passport layout
                    KycDetailsBlock(label = "FULL NAME", value = scan.mockName, neonColor = neonColor)
                    KycDetailsBlock(label = "PASSPORT NO.", value = scan.mockNumber, neonColor = neonColor)
                    if (scan.nationality.isNotEmpty()) {
                        KycDetailsBlock(label = "NATIONALITY", value = scan.nationality, neonColor = neonColor)
                    }
                    if (scan.dateOfBirth.isNotEmpty()) {
                        KycDetailsBlock(label = "DATE OF BIRTH", value = scan.dateOfBirth, neonColor = neonColor)
                    }
                    KycDetailsBlock(label = "EXPIRY DATE", value = scan.mockExpiry, neonColor = neonColor)
                    if (scan.gender.isNotEmpty()) {
                        KycDetailsBlock(label = "GENDER", value = scan.gender, neonColor = neonColor)
                    }
                }
                scan.documentType == "DRIVERS_LICENSE" -> {
                    // Driver's License layout
                    KycDetailsBlock(label = "FULL NAME", value = scan.mockName, neonColor = neonColor)
                    KycDetailsBlock(label = "LICENSE NO.", value = scan.mockNumber, neonColor = neonColor)
                    if (scan.dateOfBirth.isNotEmpty()) {
                        KycDetailsBlock(label = "DATE OF BIRTH", value = scan.dateOfBirth, neonColor = neonColor)
                    }
                    KycDetailsBlock(label = "EXPIRY DATE", value = scan.mockExpiry, neonColor = neonColor)
                    if (scan.licenseClass.isNotEmpty()) {
                        KycDetailsBlock(label = "LICENSE CLASS", value = scan.licenseClass, neonColor = neonColor)
                    }
                }
                scan.documentType == "ID_CARD" -> {
                    // ID Card layout
                    KycDetailsBlock(label = "FULL NAME", value = scan.mockName, neonColor = neonColor)
                    KycDetailsBlock(label = "ID NUMBER", value = scan.mockNumber, neonColor = neonColor)
                    if (scan.dateOfBirth.isNotEmpty()) {
                        KycDetailsBlock(label = "DATE OF BIRTH", value = scan.dateOfBirth, neonColor = neonColor)
                    }
                    KycDetailsBlock(label = "EXPIRY DATE", value = scan.mockExpiry, neonColor = neonColor)
                    if (scan.nationality.isNotEmpty()) {
                        KycDetailsBlock(label = "NATIONALITY", value = scan.nationality, neonColor = neonColor)
                    }
                    if (scan.gender.isNotEmpty()) {
                        KycDetailsBlock(label = "GENDER", value = scan.gender, neonColor = neonColor)
                    }
                }
                else -> {
                    // Generic / Unknown layout (backward compatibility for old scans)
                    KycDetailsBlock(label = "HOLDER FULL NAME", value = scan.mockName, neonColor = neonColor)
                    KycDetailsBlock(label = "CREDENTIAL ID", value = scan.mockNumber, neonColor = neonColor)
                    KycDetailsBlock(label = "EXPIRATION DATE", value = scan.mockExpiry, neonColor = neonColor)
                }
            }

            KycDetailsBlock(
                label = "AI CONFIDENCE VERDICT",
                value = "${(scan.confidence * 100).toInt()}% match probability",
                neonColor = neonColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = neonColor,
                    contentColor = Color.Black
                )
            ) {
                Text("CLOSE DETAILS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Determines if a scan is a financial card type based on documentType or label.
 * Ensures backward compatibility with old scans that may not have documentType set.
 */
private fun isFinancialDocType(documentType: String, label: String): Boolean {
    if (documentType == "CREDIT_DEBIT_CARD") return true
    val lower = label.lowercase(Locale.ROOT)
    return lower.contains("visa") || lower.contains("mastercard") ||
            lower.contains("amex") || lower.contains("discover") ||
            lower.contains("debit") || lower.contains("credit")
}

@Composable
fun KycDetailsBlock(label: String, value: String, neonColor: Color) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            label,
            fontSize = 9.sp,
            color = Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            value,
            fontSize = 14.sp,
            color = if (value == "Not detected") Color.White.copy(alpha = 0.3f) else Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

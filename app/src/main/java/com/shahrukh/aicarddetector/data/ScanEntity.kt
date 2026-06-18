package com.shahrukh.aicarddetector.data

data class ScanEntity(
    val id: Long = 0L,
    val imagePath: String,
    val label: String,
    val confidence: Float,
    val timestamp: Long,
    val mockName: String,
    val mockNumber: String,
    val mockExpiry: String,
    // Extended document-type-specific fields
    val documentType: String = "UNKNOWN",
    val dateOfBirth: String = "",
    val nationality: String = "",
    val licenseClass: String = "",
    val gender: String = "",
    val cardNetwork: String = ""
)

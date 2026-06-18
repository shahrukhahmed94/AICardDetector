package com.shahrukh.aicarddetector.data

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ScanRepository(private val context: Context) {

    private val scansFile = File(context.filesDir, "scans.json")
    private val _allScans = MutableStateFlow<List<ScanEntity>>(emptyList())
    val allScans: Flow<List<ScanEntity>> = _allScans.asStateFlow()

    init {
        loadScans()
    }

    private fun loadScans() {
        try {
            if (scansFile.exists()) {
                val jsonString = scansFile.readText()
                val jsonArray = JSONArray(jsonString)
                val list = mutableListOf<ScanEntity>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        ScanEntity(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            imagePath = obj.optString("imagePath", ""),
                            label = obj.optString("label", ""),
                            confidence = obj.optDouble("confidence", 0.0).toFloat(),
                            timestamp = obj.optLong("timestamp", 0L),
                            mockName = obj.optString("mockName", ""),
                            mockNumber = obj.optString("mockNumber", ""),
                            mockExpiry = obj.optString("mockExpiry", ""),
                            documentType = obj.optString("documentType", "UNKNOWN"),
                            dateOfBirth = obj.optString("dateOfBirth", ""),
                            nationality = obj.optString("nationality", ""),
                            licenseClass = obj.optString("licenseClass", ""),
                            gender = obj.optString("gender", ""),
                            cardNetwork = obj.optString("cardNetwork", "")
                        )
                    )
                }
                list.sortByDescending { it.timestamp }
                _allScans.value = list
            } else {
                _allScans.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e("ScanRepository", "Error loading scans: ${e.message}", e)
            _allScans.value = emptyList()
        }
    }

    private fun saveScans(list: List<ScanEntity>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { scan ->
                val obj = JSONObject().apply {
                    put("id", scan.id)
                    put("imagePath", scan.imagePath)
                    put("label", scan.label)
                    put("confidence", scan.confidence.toDouble())
                    put("timestamp", scan.timestamp)
                    put("mockName", scan.mockName)
                    put("mockNumber", scan.mockNumber)
                    put("mockExpiry", scan.mockExpiry)
                    put("documentType", scan.documentType)
                    put("dateOfBirth", scan.dateOfBirth)
                    put("nationality", scan.nationality)
                    put("licenseClass", scan.licenseClass)
                    put("gender", scan.gender)
                    put("cardNetwork", scan.cardNetwork)
                }
                jsonArray.put(obj)
            }
            scansFile.writeText(jsonArray.toString())
            _allScans.value = list.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("ScanRepository", "Error saving scans: ${e.message}", e)
        }
    }

    suspend fun saveImageToInternalStorage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "card_scans")
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "scan_${UUID.randomUUID()}.jpg"
            val file = File(directory, fileName)
            
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            return@withContext file.absolutePath
        } catch (e: Exception) {
            Log.e("ScanRepository", "Error saving image: ${e.message}", e)
            null
        }
    }

    suspend fun insertScan(
        imagePath: String,
        label: String,
        confidence: Float,
        mockName: String,
        mockNumber: String,
        mockExpiry: String,
        documentType: String = "UNKNOWN",
        dateOfBirth: String = "",
        nationality: String = "",
        licenseClass: String = "",
        gender: String = "",
        cardNetwork: String = ""
    ): Long = withContext(Dispatchers.IO) {
        val nextId = System.currentTimeMillis()
        val newScan = ScanEntity(
            id = nextId,
            imagePath = imagePath,
            label = label,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            mockName = mockName,
            mockNumber = mockNumber,
            mockExpiry = mockExpiry,
            documentType = documentType,
            dateOfBirth = dateOfBirth,
            nationality = nationality,
            licenseClass = licenseClass,
            gender = gender,
            cardNetwork = cardNetwork
        )
        val currentList = _allScans.value.toMutableList()
        currentList.add(newScan)
        saveScans(currentList)
        nextId
    }

    suspend fun deleteScan(scan: ScanEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(scan.imagePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e("ScanRepository", "Error deleting image file: ${e.message}", e)
        }
        val currentList = _allScans.value.filter { it.id != scan.id }
        saveScans(currentList)
    }

    suspend fun deleteAllScans() = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, "card_scans")
            if (directory.exists()) {
                directory.deleteRecursively()
            }
            if (scansFile.exists()) {
                scansFile.delete()
            }
        } catch (e: Exception) {
            Log.e("ScanRepository", "Error clearing files: ${e.message}", e)
        }
        _allScans.value = emptyList()
    }
}

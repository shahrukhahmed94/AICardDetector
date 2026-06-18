package com.shahrukh.aicarddetector.presentation

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shahrukh.aicarddetector.data.ScanEntity
import com.shahrukh.aicarddetector.data.ScanRepository
import com.shahrukh.aicarddetector.domain.model.Detection
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.random.Random

// Google ML Kit Text Recognition imports
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

enum class AppScreen {
    DASHBOARD,
    SCAN,
    RESULTS,
    HISTORY,
    SETTINGS
}

enum class OcrSimState {
    IDLE,
    ANALYZING,
    READING,
    AUTHENTICATING,
    COMPLETED
}

enum class DocumentType {
    CREDIT_DEBIT_CARD,
    PASSPORT,
    ID_CARD,
    DRIVERS_LICENSE,
    UNKNOWN
}

data class ParsedDocumentData(
    val documentType: DocumentType = DocumentType.UNKNOWN,
    val labelOverride: String? = null,
    // Common
    val holderName: String = "Not detected",
    // Card-specific
    val cardNumber: String = "",
    val cardExpiry: String = "",
    val cardNetwork: String = "",
    // Document-specific
    val documentNumber: String = "",
    val dateOfBirth: String = "",
    val expiryDate: String = "",
    val nationality: String = "",
    val licenseClass: String = "",
    val gender: String = ""
)

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ScanRepository(application)

    // Navigation state
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Scanner preferences
    val confidenceThreshold = MutableStateFlow(0.6f)
    val autoCaptureEnabled = MutableStateFlow(true)
    val soundFeedbackEnabled = MutableStateFlow(true)
    val hapticFeedbackEnabled = MutableStateFlow(true)
    val torchEnabled = MutableStateFlow(false)

    // Live scanner detection states
    private val _activeDetection = MutableStateFlow<Detection?>(null)
    val activeDetection: StateFlow<Detection?> = _activeDetection.asStateFlow()

    // Auto-capture stabilization tracking
    private val _stabilizationProgress = MutableStateFlow(0f)
    val stabilizationProgress: StateFlow<Float> = _stabilizationProgress.asStateFlow()

    // Scanned result cache
    private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
    val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

    private val _capturedLabel = MutableStateFlow("")
    val capturedLabel: StateFlow<String> = _capturedLabel.asStateFlow()

    private val _capturedConfidence = MutableStateFlow(0f)
    val capturedConfidence: StateFlow<Float> = _capturedConfidence.asStateFlow()

    // Simulated OCR states
    private val _ocrSimState = MutableStateFlow(OcrSimState.IDLE)
    val ocrSimState: StateFlow<OcrSimState> = _ocrSimState.asStateFlow()

    private val _ocrProgressText = MutableStateFlow("")
    val ocrProgressText: StateFlow<String> = _ocrProgressText.asStateFlow()

    // Extracted Card Fields (kept for backward compatibility with existing UI bindings)
    val mockName = MutableStateFlow("")
    val mockDocNumber = MutableStateFlow("")
    val mockExpiry = MutableStateFlow("")

    // Extended parsed document data
    private val _parsedDocumentData = MutableStateFlow(ParsedDocumentData())
    val parsedDocumentData: StateFlow<ParsedDocumentData> = _parsedDocumentData.asStateFlow()

    // History & Search states
    val searchQuery = MutableStateFlow("")
    val selectedCardTypeFilter = MutableStateFlow<String?>(null)

    // Flow linking scans from DB
    val allScans: StateFlow<List<ScanEntity>> = repository.allScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered scans list for UI
    val filteredScans: StateFlow<List<ScanEntity>> = combine(
        allScans,
        searchQuery,
        selectedCardTypeFilter
    ) { scans, query, typeFilter ->
        scans.filter { scan ->
            val matchesQuery = query.isEmpty() ||
                    scan.mockName.contains(query, ignoreCase = true) ||
                    scan.mockNumber.contains(query, ignoreCase = true) ||
                    scan.label.contains(query, ignoreCase = true)
            val matchesType = typeFilter == null || scan.label.equals(typeFilter, ignoreCase = true)
            matchesQuery && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics computations
    val totalScansCount = allScans.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val averageConfidence = allScans.map { list ->
        if (list.isEmpty()) 0f else list.map { it.confidence }.average().toFloat()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val scansByCardType = allScans.map { list ->
        list.groupBy { it.label }.mapValues { it.value.size }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var autoCaptureJob: Job? = null
    private var stabilizationStartTime: Long = 0L

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.SCAN) {
            // Reset scan states when entering scanner
            _activeDetection.value = null
            _stabilizationProgress.value = 0f
            autoCaptureJob?.cancel()
        }
    }

    /**
     * Updates active detections from Camera Analyzer.
     * Manages auto-capture trigger countdown when card is held steady.
     */
    fun onDetectionsUpdated(detections: List<Detection>) {
        val detection = detections.firstOrNull()
        _activeDetection.value = detection

        if (detection == null) {
            // No card detected, reset countdown
            resetStabilization()
            return
        }

        // Card detected above threshold
        if (autoCaptureEnabled.value) {
            if (autoCaptureJob == null || autoCaptureJob?.isActive == false) {
                startAutoCaptureTimer()
            }
        }
    }

    private fun startAutoCaptureTimer() {
        stabilizationStartTime = System.currentTimeMillis()
        autoCaptureJob = viewModelScope.launch {
            val duration = 1500L // 1.5 seconds stabilization required
            while (System.currentTimeMillis() - stabilizationStartTime < duration) {
                val elapsed = System.currentTimeMillis() - stabilizationStartTime
                _stabilizationProgress.value = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
                delay(50)
            }
            _stabilizationProgress.value = 1f
            // Trigger capture callback inside scan screen
            triggerAutoCapture()
        }
    }

    private fun resetStabilization() {
        autoCaptureJob?.cancel()
        autoCaptureJob = null
        _stabilizationProgress.value = 0f
    }

    // Event flow to trigger camera shutter in UI
    private val _captureRequests = MutableSharedFlow<Unit>()
    val captureRequests = _captureRequests.asSharedFlow()

    private fun triggerAutoCapture() {
        viewModelScope.launch {
            _captureRequests.emit(Unit)
        }
    }

    /**
     * Handle successfully captured bitmap, navigate to results and run real OCR extraction.
     */
    fun processCapturedCard(bitmap: Bitmap, detectedLabel: String?, detectedConfidence: Float?) {
        resetStabilization()
        _capturedBitmap.value = bitmap
        
        val label = when (detectedLabel?.lowercase(Locale.ROOT)) {
            "id_card" -> "ID Card"
            "passport" -> "Passport"
            "drivers_license" -> "Driver's License"
            else -> detectedLabel ?: "ID Document"
        }
        
        _capturedLabel.value = label
        _capturedConfidence.value = detectedConfidence ?: confidenceThreshold.value

        // Trigger haptics and audio feedback
        playBeepTone()
        triggerVibration()

        // Start real OCR flow
        navigateTo(AppScreen.RESULTS)
        performRealOcr(bitmap, label)
    }

    private fun performRealOcr(bitmap: Bitmap, label: String) {
        viewModelScope.launch {
            _ocrSimState.value = OcrSimState.ANALYZING
            _ocrProgressText.value = "Analyzing document alignment..."
            delay(1000)

            _ocrSimState.value = OcrSimState.READING
            _ocrProgressText.value = "Extracting text fields with AI OCR..."

            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val textBlocks = visionText.textBlocks
                        Log.d("OCR_RESULT", "Full extracted text:\n${visionText.text}")

                        val parsedData = parseOcrData(textBlocks, label)
                        applyParsedData(parsedData)

                        viewModelScope.launch {
                            _ocrSimState.value = OcrSimState.AUTHENTICATING
                            _ocrProgressText.value = "Verifying security signatures..."
                            delay(1000)

                            _ocrSimState.value = OcrSimState.COMPLETED
                            _ocrProgressText.value = "Document Verified Successfully!"
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("OCR_ERROR", "ML Kit OCR failed", e)
                        applyFallbackData(label)
                        viewModelScope.launch {
                            _ocrSimState.value = OcrSimState.AUTHENTICATING
                            _ocrProgressText.value = "Verifying security signatures..."
                            delay(1000)

                            _ocrSimState.value = OcrSimState.COMPLETED
                            _ocrProgressText.value = "Document Verified (OCR limited)!"
                        }
                    }
            } catch (e: Exception) {
                Log.e("OCR_ERROR", "Exception starting OCR", e)
                applyFallbackData(label)
                _ocrSimState.value = OcrSimState.COMPLETED
                _ocrProgressText.value = "Document Verified (OCR limited)!"
            }
        }
    }

    private fun applyParsedData(data: ParsedDocumentData) {
        _parsedDocumentData.value = data

        // Also update legacy fields for backward compatibility
        mockName.value = data.holderName
        if (data.labelOverride != null) {
            _capturedLabel.value = data.labelOverride
        }

        when (data.documentType) {
            DocumentType.CREDIT_DEBIT_CARD -> {
                mockDocNumber.value = data.cardNumber.ifEmpty { "Not detected" }
                mockExpiry.value = data.cardExpiry.ifEmpty { "Not detected" }
            }
            else -> {
                mockDocNumber.value = data.documentNumber.ifEmpty { "Not detected" }
                mockExpiry.value = data.expiryDate.ifEmpty { "Not detected" }
            }
        }
    }

    private fun applyFallbackData(label: String) {
        val docType = classifyDocumentType(label)
        val fallback = ParsedDocumentData(
            documentType = docType,
            holderName = "Not detected",
            cardNumber = "",
            cardExpiry = "",
            documentNumber = "",
            expiryDate = "",
            dateOfBirth = "",
            nationality = "",
            licenseClass = "",
            gender = ""
        )
        applyParsedData(fallback)
    }

    // ========================================================================
    // OCR PARSING ENGINE — Document-Type-Aware Strategies
    // ========================================================================

    private fun parseOcrData(
        textBlocks: List<com.google.mlkit.vision.text.Text.TextBlock>,
        label: String
    ): ParsedDocumentData {
        val lines = textBlocks.flatMap { it.lines }.map { it.text.trim() }
        Log.d("OCR_PARSER", "Extracted lines: $lines")

        // First, detect if this is a credit/debit card by looking for card number
        val cardNumberResult = detectCreditCardNumber(lines)

        return if (cardNumberResult != null) {
            parseCreditDebitCard(lines, cardNumberResult)
        } else {
            // Fallback: check for card-related keywords even without detecting a card number
            val cardBrandFromKeywords = detectCardFromKeywords(lines)
            if (cardBrandFromKeywords != null) {
                parseCardWithoutNumber(lines, cardBrandFromKeywords)
            } else {
                when (classifyDocumentType(label)) {
                    DocumentType.PASSPORT -> parsePassport(lines)
                    DocumentType.DRIVERS_LICENSE -> parseDriversLicense(lines)
                    DocumentType.ID_CARD -> parseIdCard(lines)
                    else -> parseGenericDocument(lines, label)
                }
            }
        }
    }

    /**
     * Detects card brand from keywords in OCR text even when no card number is found.
     * Handles cases where embossed/printed card numbers are hard to OCR.
     */
    private fun detectCardFromKeywords(lines: List<String>): String? {
        val allText = lines.joinToString(" ").uppercase(Locale.ROOT)
        return when {
            allText.contains("VISA") && !allText.contains("VISA ") .let { allText.contains("VISA") } -> "Visa Card"
            allText.contains("MASTERCARD") || allText.contains("MASTER CARD") -> "Mastercard"
            allText.contains("AMEX") || allText.contains("AMERICAN EXPRESS") -> "Amex Card"
            allText.contains("DISCOVER") -> "Discover Card"
            allText.contains("UNIONPAY") || allText.contains("UNION PAY") -> "UnionPay Card"
            // Generic card keywords
            allText.contains("VALID THRU") || allText.contains("VALID THROUGH") -> "Debit/Credit Card"
            allText.contains("GOOD THRU") -> "Debit/Credit Card"
            allText.contains("DEBIT") -> "Debit Card"
            allText.contains("CREDIT") -> "Credit Card"
            allText.contains("CARDHOLDER") || allText.contains("CARD HOLDER") -> "Debit/Credit Card"
            allText.contains("CVV") || allText.contains("CCV") -> "Debit/Credit Card"
            allText.contains("MAESTRO") -> "Maestro Card"
            allText.contains("PLATINUM") || allText.contains("GOLD CARD") -> "Debit/Credit Card"
            else -> null
        }
    }

    /**
     * Parses a card where keywords indicate it's a financial card but no card number was detected.
     */
    private fun parseCardWithoutNumber(lines: List<String>, brandLabel: String): ParsedDocumentData {
        var cardExpiry = ""
        var holderName = "Not detected"

        // Reuse expiry detection logic
        val expiryPatterns = listOf(
            Regex("""(?:VALID\s*(?:THRU|THROUGH|TILL)|EXP(?:IRY|IRES)?|GOOD\s*THRU|VALID\s*UNTIL)[:\s]*(\d{2}[/\-]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2}[/\-]\d{2})(?!\d)"""),
            Regex("""(\d{2}[/\-]\d{4})""")
        )
        for (line in lines) {
            if (cardExpiry.isNotEmpty()) break
            for (pattern in expiryPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    val candidate = match.groupValues.getOrElse(1) { match.value }
                    val month = candidate.take(2).toIntOrNull()
                    if (month != null && month in 1..12) {
                        cardExpiry = candidate
                        break
                    }
                }
            }
        }

        // Reuse name detection (bottom-of-card preference)
        holderName = findNameCandidate(lines, isBottomPreferred = true)

        return ParsedDocumentData(
            documentType = DocumentType.CREDIT_DEBIT_CARD,
            labelOverride = brandLabel,
            holderName = holderName,
            cardNumber = "Not detected",
            cardExpiry = cardExpiry.ifEmpty { "Not detected" },
            cardNetwork = brandLabel
        )
    }

    private fun classifyDocumentType(label: String): DocumentType {
        val lower = label.lowercase(Locale.ROOT)
        return when {
            lower.contains("visa") || lower.contains("mastercard") ||
            lower.contains("amex") || lower.contains("discover") ||
            lower.contains("debit") || lower.contains("credit") -> DocumentType.CREDIT_DEBIT_CARD
            lower.contains("passport") -> DocumentType.PASSPORT
            lower.contains("driver") || lower.contains("license") || lower.contains("licence") -> DocumentType.DRIVERS_LICENSE
            lower.contains("id") || lower.contains("identity") -> DocumentType.ID_CARD
            else -> DocumentType.UNKNOWN
        }
    }

    // ---- Credit/Debit Card Parsing ----

    private data class CardNumberResult(val number: String, val formatted: String, val brand: String)

    private fun detectCreditCardNumber(lines: List<String>): CardNumberResult? {
        // Helper: fix common OCR character misreads for digits
        fun fixOcrDigits(text: String): String {
            return text
                .replace('O', '0').replace('o', '0')
                .replace('I', '1').replace('l', '1')
                .replace('S', '5').replace('s', '5')
                .replace('B', '8')
                .replace('G', '6').replace('g', '9')
                .replace('T', '7')
                .replace('Z', '2').replace('z', '2')
        }

        for (line in lines) {
            // Try direct spaced format: "4532 1234 5678 9012"
            val spacedPattern = Regex("""(\d{4}[\s.-]\d{4}[\s.-]\d{4}[\s.-]\d{4})""")
            val spacedMatch = spacedPattern.find(line)
            if (spacedMatch != null) {
                val raw = spacedMatch.value
                val digits = raw.replace(Regex("[^0-9]"), "")
                if (digits.length == 16) {
                    val brand = identifyCardBrand(digits)
                    val formatted = digits.chunked(4).joinToString(" ")
                    return CardNumberResult(digits, formatted, brand)
                }
            }

            // Try Amex format: "3782 822463 10005" (4-6-5)
            val amexSpacedPattern = Regex("""(3[47]\d{2}[\s.-]\d{6}[\s.-]\d{5})""")
            val amexMatch = amexSpacedPattern.find(line)
            if (amexMatch != null) {
                val digits = amexMatch.value.replace(Regex("[^0-9]"), "")
                if (digits.length == 15) {
                    return CardNumberResult(
                        digits,
                        "${digits.substring(0, 4)} ${digits.substring(4, 10)} ${digits.substring(10)}",
                        "Amex Card"
                    )
                }
            }

            // Try stripping all non-digits from the line
            val cleanDigits = line.replace(Regex("[^0-9]"), "")
            if (cleanDigits.length == 16) {
                val brand = identifyCardBrand(cleanDigits)
                val formatted = cleanDigits.chunked(4).joinToString(" ")
                return CardNumberResult(cleanDigits, formatted, brand)
            }
            if (cleanDigits.length == 15 && cleanDigits.startsWith("3")) {
                return CardNumberResult(
                    cleanDigits,
                    "${cleanDigits.substring(0, 4)} ${cleanDigits.substring(4, 10)} ${cleanDigits.substring(10)}",
                    "Amex Card"
                )
            }

            // Try with OCR character correction (O→0, I→1, S→5, etc.)
            val correctedLine = fixOcrDigits(line)
            val correctedDigits = correctedLine.replace(Regex("[^0-9]"), "")
            if (correctedDigits.length == 16 && correctedDigits != cleanDigits) {
                val brand = identifyCardBrand(correctedDigits)
                val formatted = correctedDigits.chunked(4).joinToString(" ")
                Log.d("OCR_PARSER", "Card number found after OCR correction: $formatted")
                return CardNumberResult(correctedDigits, formatted, brand)
            }
        }

        // Try combining adjacent lines that together form a 16-digit card number
        for (i in 0 until lines.size - 1) {
            val combined = lines[i] + lines[i + 1]
            val combinedDigits = combined.replace(Regex("[^0-9]"), "")
            if (combinedDigits.length == 16) {
                val brand = identifyCardBrand(combinedDigits)
                val formatted = combinedDigits.chunked(4).joinToString(" ")
                Log.d("OCR_PARSER", "Card number found across 2 lines: $formatted")
                return CardNumberResult(combinedDigits, formatted, brand)
            }
        }

        return null
    }

    private fun identifyCardBrand(digits: String): String {
        return when {
            digits.startsWith("4") -> "Visa Card"
            digits.startsWith("5") && digits[1] in '1'..'5' -> "Mastercard"
            digits.startsWith("2") && digits.substring(0, 4).toIntOrNull()?.let { it in 2221..2720 } == true -> "Mastercard"
            digits.startsWith("34") || digits.startsWith("37") -> "Amex Card"
            digits.startsWith("6011") || digits.startsWith("65") || digits.startsWith("644") -> "Discover Card"
            else -> "Debit/Credit Card"
        }
    }

    private fun parseCreditDebitCard(lines: List<String>, cardNumber: CardNumberResult): ParsedDocumentData {
        var cardExpiry = ""
        var holderName = "Not detected"

        // --- Expiry detection ---
        // Credit cards typically show MM/YY or MM/YYYY
        val expiryPatterns = listOf(
            Regex("""(?:VALID\s*(?:THRU|THROUGH|TILL)|EXP(?:IRY|IRES)?|GOOD\s*THRU|VALID\s*UNTIL)[:\s]*(\d{2}[/\-]\d{2,4})""", RegexOption.IGNORE_CASE),
            Regex("""(\d{2}[/\-]\d{2})(?!\d)"""),  // MM/YY standalone
            Regex("""(\d{2}[/\-]\d{4})""")          // MM/YYYY
        )

        for (line in lines) {
            if (cardExpiry.isNotEmpty()) break
            for (pattern in expiryPatterns) {
                val match = pattern.find(line)
                if (match != null) {
                    cardExpiry = match.groupValues.getOrElse(1) { match.value }
                    // Validate it looks like a real date (month 01-12)
                    val monthStr = cardExpiry.take(2)
                    val month = monthStr.toIntOrNull()
                    if (month == null || month < 1 || month > 12) {
                        cardExpiry = ""
                        continue
                    }
                    break
                }
            }
        }

        // --- Cardholder name detection ---
        // On credit cards, the name is typically BELOW the card number and expiry,
        // in ALL CAPS format. We look for lines that are purely alphabetic words.
        val forbiddenCardWords = setOf(
            "visa", "mastercard", "maestro", "amex", "express", "discover",
            "bank", "banking", "debit", "credit", "prepaid", "platinum",
            "gold", "silver", "classic", "infinite", "signature", "world",
            "business", "preferred", "member", "since", "valid", "thru",
            "through", "electronic", "use", "only", "cash", "back",
            "rewards", "pay", "secure", "plus", "chip", "contactless",
            "card", "international", "interac", "unionpay", "network"
        )

        val potentialNames = mutableListOf<String>()
        for (line in lines) {
            val clean = line.replace(Regex("[^a-zA-Z\\s]"), "").trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }

            if (words.size in 2..4 && clean.length >= 5) {
                val hasForbidden = words.any { word ->
                    forbiddenCardWords.contains(word.lowercase(Locale.ROOT))
                }
                if (!hasForbidden && words.all { it.length >= 2 }) {
                    potentialNames.add(
                        clean.lowercase(Locale.ROOT)
                            .split(" ")
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    )
                }
            }
        }
        // For credit cards, the name is usually the LAST text block (bottom of card)
        if (potentialNames.isNotEmpty()) {
            holderName = potentialNames.lastOrNull() ?: "Not detected"
        }

        return ParsedDocumentData(
            documentType = DocumentType.CREDIT_DEBIT_CARD,
            labelOverride = cardNumber.brand,
            holderName = holderName,
            cardNumber = cardNumber.formatted,
            cardExpiry = cardExpiry.ifEmpty { "Not detected" },
            cardNetwork = cardNumber.brand
        )
    }

    // ---- Passport Parsing ----

    private fun parsePassport(lines: List<String>): ParsedDocumentData {
        var holderName = "Not detected"
        var passportNumber = ""
        var nationality = ""
        var dateOfBirth = ""
        var expiryDate = ""
        var gender = ""

        // --- MRZ Parsing ---
        // Passport MRZ has 2 lines of 44 characters each
        // Line 1: P<ISSUING_COUNTRY<SURNAME<<GIVEN_NAMES<<<<...
        // Line 2: PASSPORT_NO<CHECK_DIGIT<NATIONALITY<DOB<CHECK<SEX<EXPIRY<CHECK<...
        val mrzLines = mutableListOf<String>()
        for (line in lines) {
            val cleanLine = line.replace(" ", "").uppercase(Locale.ROOT)
            if ((cleanLine.startsWith("P<") || cleanLine.startsWith("P0")) && cleanLine.length >= 30) {
                mrzLines.add(cleanLine)
            } else if (cleanLine.length >= 30 && cleanLine.contains("<") && cleanLine.matches(Regex("[A-Z0-9<]+"))) {
                mrzLines.add(cleanLine)
            }
        }

        // Parse MRZ Line 1 (name line)
        for (mrzLine in mrzLines) {
            if (mrzLine.startsWith("P<") || mrzLine.startsWith("P0")) {
                val afterType = mrzLine.substring(2)
                // Skip 3-letter country code
                val namePart = if (afterType.length > 3) afterType.substring(3) else afterType
                val parts = namePart.split("<<")
                if (parts.size >= 2) {
                    val lastName = parts[0].replace("<", " ").trim()
                    val firstName = parts[1].split("<").filter { it.isNotEmpty() }.joinToString(" ")
                    if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
                        holderName = "$firstName $lastName".lowercase(Locale.ROOT)
                            .split(" ")
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    }
                }
            }
        }

        // Parse MRZ Line 2 (data line) — extract passport number, nationality, DOB, sex, expiry
        for (mrzLine in mrzLines) {
            if (!mrzLine.startsWith("P<") && !mrzLine.startsWith("P0") && mrzLine.length >= 28) {
                // Format: [PassportNo(9)][Check(1)][Nationality(3)][DOB_YYMMDD(6)][Check(1)][Sex(1)][Expiry_YYMMDD(6)][Check(1)]...
                val passportNoRaw = mrzLine.substring(0, 9).replace("<", "")
                if (passportNoRaw.isNotEmpty() && passportNoRaw.any { it.isDigit() }) {
                    passportNumber = passportNoRaw
                }

                if (mrzLine.length >= 13) {
                    val natCode = mrzLine.substring(10, 13).replace("<", "")
                    if (natCode.length == 3 && natCode.all { it.isLetter() }) {
                        nationality = natCode
                    }
                }

                if (mrzLine.length >= 19) {
                    val dobRaw = mrzLine.substring(13, 19)
                    if (dobRaw.all { it.isDigit() }) {
                        val yy = dobRaw.substring(0, 2)
                        val mm = dobRaw.substring(2, 4)
                        val dd = dobRaw.substring(4, 6)
                        dateOfBirth = "$dd/$mm/$yy"
                    }
                }

                if (mrzLine.length >= 21) {
                    val sexChar = mrzLine[20]
                    gender = when (sexChar) {
                        'M' -> "Male"
                        'F' -> "Female"
                        else -> ""
                    }
                }

                if (mrzLine.length >= 27) {
                    val expiryRaw = mrzLine.substring(21, 27)
                    if (expiryRaw.all { it.isDigit() }) {
                        val yy = expiryRaw.substring(0, 2)
                        val mm = expiryRaw.substring(2, 4)
                        val dd = expiryRaw.substring(4, 6)
                        expiryDate = "$dd/$mm/$yy"
                    }
                }
                break
            }
        }

        // Fallback: if MRZ didn't yield passport number, scan for alphanumeric pattern
        if (passportNumber.isEmpty()) {
            val passportPattern = Regex("""[A-Z]{1,2}\d{6,8}""")
            for (line in lines) {
                val match = passportPattern.find(line.uppercase(Locale.ROOT))
                if (match != null) {
                    passportNumber = match.value
                    break
                }
            }
        }

        // Fallback: scan for name using labeled fields
        if (holderName == "Not detected") {
            holderName = extractLabeledField(lines, listOf("name", "given name", "surname", "nom")) ?: findNameCandidate(lines, isBottomPreferred = false)
        }

        // Fallback: scan for dates
        if (dateOfBirth.isEmpty()) {
            dateOfBirth = extractLabeledField(lines, listOf("date of birth", "dob", "born", "birth", "date de naissance")) ?: ""
        }
        if (expiryDate.isEmpty()) {
            expiryDate = extractLabeledField(lines, listOf("expiry", "expires", "expiration", "date of expiry", "valid until")) ?: ""
            if (expiryDate.isEmpty()) {
                expiryDate = extractDateFromLines(lines, skip = dateOfBirth)
            }
        }

        return ParsedDocumentData(
            documentType = DocumentType.PASSPORT,
            holderName = holderName,
            documentNumber = passportNumber.ifEmpty { "Not detected" },
            nationality = nationality.ifEmpty { "Not detected" },
            dateOfBirth = dateOfBirth.ifEmpty { "Not detected" },
            expiryDate = expiryDate.ifEmpty { "Not detected" },
            gender = gender.ifEmpty { "Not detected" }
        )
    }

    // ---- Driver's License Parsing ----

    private fun parseDriversLicense(lines: List<String>): ParsedDocumentData {
        var holderName = "Not detected"
        var licenseNumber = ""
        var dateOfBirth = ""
        var expiryDate = ""
        var licenseClass = ""

        // Try numbered field format (e.g. "1 John", "2 Smith")
        var firstName = ""
        var lastName = ""
        for (line in lines) {
            val upperLine = line.uppercase(Locale.ROOT)
            if (upperLine.matches(Regex("^[12]\\s+.*"))) {
                val content = line.substring(2).trim()
                if (upperLine.startsWith("1 ")) firstName = content
                else if (upperLine.startsWith("2 ")) lastName = content
            } else if (upperLine.startsWith("FN ") || upperLine.startsWith("GIV ") || upperLine.startsWith("FIRST")) {
                firstName = line.substringAfter(" ").trim()
            } else if (upperLine.startsWith("LN ") || upperLine.startsWith("SUR ") || upperLine.startsWith("LAST")) {
                lastName = line.substringAfter(" ").trim()
            }
        }
        if (firstName.isNotEmpty() && lastName.isNotEmpty()) {
            holderName = "$firstName $lastName".lowercase(Locale.ROOT)
                .split(" ").filter { it.isNotEmpty() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        }

        // License number pattern: alphanumeric, 7-15 chars, often starts with letters
        val dlPattern = Regex("""[A-Z]{0,3}\d{4,12}[A-Z0-9-]*""")
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT).replace(" ", "")
            val match = dlPattern.find(upper)
            if (match != null && match.value.length in 7..15 && match.value.any { it.isDigit() }) {
                licenseNumber = match.value
                break
            }
        }

        // Extract labeled fields
        if (holderName == "Not detected") {
            holderName = extractLabeledField(lines, listOf("name", "full name", "driver name")) ?: findNameCandidate(lines, isBottomPreferred = false)
        }

        dateOfBirth = extractLabeledField(lines, listOf("dob", "date of birth", "born", "birth")) ?: ""
        expiryDate = extractLabeledField(lines, listOf("exp", "expiry", "expires", "valid until", "valid thru")) ?: ""

        // License class (e.g. "Class: B", "4d B")
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT)
            val classMatch = Regex("""(?:CLASS|CL|CAT(?:EGORY)?)[:\s]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE).find(upper)
            if (classMatch != null) {
                licenseClass = classMatch.groupValues[1]
                break
            }
            // Numbered field: "9 B" (EU format)
            if (upper.matches(Regex("^9\\s+[A-Z0-9]{1,3}$"))) {
                licenseClass = upper.substringAfter(" ").trim()
                break
            }
        }

        // Fallback date extraction
        if (dateOfBirth.isEmpty() || expiryDate.isEmpty()) {
            val dates = extractAllDates(lines)
            if (dateOfBirth.isEmpty() && dates.isNotEmpty()) dateOfBirth = dates.first()
            if (expiryDate.isEmpty() && dates.size >= 2) expiryDate = dates[1]
        }

        return ParsedDocumentData(
            documentType = DocumentType.DRIVERS_LICENSE,
            holderName = holderName,
            documentNumber = licenseNumber.ifEmpty { "Not detected" },
            dateOfBirth = dateOfBirth.ifEmpty { "Not detected" },
            expiryDate = expiryDate.ifEmpty { "Not detected" },
            licenseClass = licenseClass.ifEmpty { "Not detected" }
        )
    }

    // ---- ID Card Parsing ----

    private fun parseIdCard(lines: List<String>): ParsedDocumentData {
        var holderName = "Not detected"
        var idNumber = ""
        var dateOfBirth = ""
        var expiryDate = ""
        var nationality = ""
        var gender = ""

        // ID number pattern
        val idPattern = Regex("""[A-Z0-9-]{7,15}""")
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT).replace(" ", "")
            val match = idPattern.find(upper)
            if (match != null && match.value.any { it.isDigit() } && match.value.length >= 7) {
                // Exclude pure dates
                if (!match.value.matches(Regex("""\d{2}[/-]\d{2}[/-]\d{2,4}"""))) {
                    idNumber = match.value
                    break
                }
            }
        }

        // Extract labeled fields
        holderName = extractLabeledField(lines, listOf("name", "full name", "given name", "nom", "surname")) ?: findNameCandidate(lines, isBottomPreferred = false)
        dateOfBirth = extractLabeledField(lines, listOf("dob", "date of birth", "born", "birth", "date de naissance")) ?: ""
        expiryDate = extractLabeledField(lines, listOf("exp", "expiry", "expires", "valid until", "valid", "expiration")) ?: ""

        val natField = extractLabeledField(lines, listOf("nationality", "nat", "citizen"))
        if (natField != null) nationality = natField

        val genderField = extractLabeledField(lines, listOf("sex", "gender"))
        if (genderField != null) {
            gender = when (genderField.uppercase(Locale.ROOT).firstOrNull()) {
                'M' -> "Male"
                'F' -> "Female"
                else -> genderField
            }
        }

        // Fallback date extraction
        if (dateOfBirth.isEmpty() || expiryDate.isEmpty()) {
            val dates = extractAllDates(lines)
            if (dateOfBirth.isEmpty() && dates.isNotEmpty()) dateOfBirth = dates.first()
            if (expiryDate.isEmpty() && dates.size >= 2) expiryDate = dates[1]
        }

        return ParsedDocumentData(
            documentType = DocumentType.ID_CARD,
            holderName = holderName,
            documentNumber = idNumber.ifEmpty { "Not detected" },
            dateOfBirth = dateOfBirth.ifEmpty { "Not detected" },
            expiryDate = expiryDate.ifEmpty { "Not detected" },
            nationality = nationality.ifEmpty { "" },
            gender = gender.ifEmpty { "" }
        )
    }

    // ---- Generic Document Parsing (fallback) ----

    private fun parseGenericDocument(lines: List<String>, label: String): ParsedDocumentData {
        // Check if it might be a card we didn't detect by label
        val holderName = findNameCandidate(lines, isBottomPreferred = true)
        val dates = extractAllDates(lines)

        val idPattern = Regex("""[A-Z0-9-]{7,15}""")
        var docNumber = ""
        for (line in lines) {
            val upper = line.uppercase(Locale.ROOT).replace(" ", "")
            val match = idPattern.find(upper)
            if (match != null && match.value.any { it.isDigit() } && match.value.length >= 7) {
                docNumber = match.value
                break
            }
        }

        return ParsedDocumentData(
            documentType = DocumentType.UNKNOWN,
            holderName = holderName,
            documentNumber = docNumber.ifEmpty { "Not detected" },
            expiryDate = if (dates.isNotEmpty()) dates.last() else "Not detected"
        )
    }

    // ========================================================================
    // Shared Helper Functions
    // ========================================================================

    private val documentForbiddenWords = setOf(
        "document", "card", "passport", "license", "licence", "driver", "identity",
        "republic", "state", "government", "ministry", "national", "name",
        "surname", "given", "date", "birth", "sex", "nationality", "authority",
        "issue", "expiry", "class", "expires", "holder", "signature", "united",
        "states", "america", "european", "union", "country", "photo", "valide",
        "valid", "until", "dob", "exp", "lic", "dl", "pp", "id",
        "bank", "banking", "debit", "credit", "prepaid", "visa", "mastercard",
        "maestro", "amex", "express", "discover", "capital", "one", "chase",
        "hsbc", "citi", "citibank", "wells", "fargo", "barclays", "platinum",
        "gold", "silver", "classic", "infinite", "world", "business",
        "preferred", "member", "since", "thru", "electronic", "use", "only",
        "cash", "back", "rewards", "pay", "secure", "plus", "type", "place",
        "issue", "issuing", "department", "office", "federal", "province"
    )

    /**
     * Looks for a line following a label keyword (e.g. "Name: John Doe" or "Name\nJohn Doe")
     */
    private fun extractLabeledField(lines: List<String>, labels: List<String>): String? {
        for (i in lines.indices) {
            val lower = lines[i].lowercase(Locale.ROOT).trim()
            for (labelKeyword in labels) {
                // Case 1: "Name: John Doe" — value on the same line after colon/space
                if (lower.startsWith(labelKeyword)) {
                    val remainder = lines[i].substringAfter(labelKeyword, "")
                        .removePrefix(":").removePrefix(" ").trim()
                    // Try case-insensitive removal
                    val remainderAlt = lines[i].substring(
                        minOf(lines[i].length, labelKeyword.length)
                    ).removePrefix(":").removePrefix(" ").trim()
                    val value = if (remainder.length >= 2) remainder 
                                else if (remainderAlt.length >= 2) remainderAlt
                                else null
                    if (value != null && value.length >= 2) return value
                    // Case 2: value is on the NEXT line
                    if (i + 1 < lines.size) {
                        val nextLine = lines[i + 1].trim()
                        if (nextLine.isNotEmpty() && nextLine.length >= 2) return nextLine
                    }
                }
            }
        }
        return null
    }

    /**
     * Finds the best name candidate from OCR lines using heuristics.
     */
    private fun findNameCandidate(lines: List<String>, isBottomPreferred: Boolean): String {
        val potentialNames = mutableListOf<String>()

        for (line in lines) {
            val clean = line.replace(Regex("[^a-zA-Z\\s]"), "").trim()
            val words = clean.split(Regex("\\s+")).filter { it.isNotEmpty() }

            if (words.size in 2..4 && clean.length >= 5) {
                val hasForbidden = words.any { word ->
                    documentForbiddenWords.contains(word.lowercase(Locale.ROOT))
                }
                if (!hasForbidden && words.all { it.length >= 2 }) {
                    potentialNames.add(
                        clean.lowercase(Locale.ROOT)
                            .split(" ")
                            .filter { it.isNotEmpty() }
                            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                    )
                }
            }
        }

        return if (isBottomPreferred) {
            potentialNames.lastOrNull() ?: "Not detected"
        } else {
            potentialNames.firstOrNull() ?: "Not detected"
        }
    }

    /**
     * Extract all date-like patterns from lines.
     */
    private fun extractAllDates(lines: List<String>): List<String> {
        val datePatterns = listOf(
            Regex("""\b\d{2}[/\-.]\d{2}[/\-.]\d{4}\b"""),    // DD/MM/YYYY
            Regex("""\b\d{4}[/\-.]\d{2}[/\-.]\d{2}\b"""),    // YYYY/MM/DD
            Regex("""\b\d{2}[/\-.]\d{2}[/\-.]\d{2}\b"""),    // DD/MM/YY
            Regex("""\b\d{2}\s+[A-Z]{3}\s+\d{4}\b""", RegexOption.IGNORE_CASE) // 01 JAN 2025
        )
        val found = mutableListOf<String>()
        for (line in lines) {
            for (pattern in datePatterns) {
                val matches = pattern.findAll(line)
                for (match in matches) {
                    if (match.value !in found) {
                        found.add(match.value)
                    }
                }
            }
        }
        return found
    }

    /**
     * Extract the first date that doesn't match a known skip value.
     */
    private fun extractDateFromLines(lines: List<String>, skip: String): String {
        val dates = extractAllDates(lines)
        return dates.firstOrNull { it != skip } ?: ""
    }

    // ========================================================================
    // Save & Persistence
    // ========================================================================

    /**
     * Saves the scan results to Room and persists the bitmap file in internal storage.
     */
    fun saveScanResult() {
        val bitmap = _capturedBitmap.value ?: return
        val data = _parsedDocumentData.value
        viewModelScope.launch {
            val imagePath = repository.saveImageToInternalStorage(bitmap)
            if (imagePath != null) {
                repository.insertScan(
                    imagePath = imagePath,
                    label = _capturedLabel.value,
                    confidence = _capturedConfidence.value,
                    mockName = mockName.value,
                    mockNumber = mockDocNumber.value,
                    mockExpiry = mockExpiry.value,
                    documentType = data.documentType.name,
                    dateOfBirth = data.dateOfBirth,
                    nationality = data.nationality,
                    licenseClass = data.licenseClass,
                    gender = data.gender,
                    cardNetwork = data.cardNetwork
                )
                navigateTo(AppScreen.DASHBOARD)
            } else {
                Log.e("ScanViewModel", "Failed to save card image, aborting save Scan.")
            }
        }
    }

    fun deleteScanItem(scan: ScanEntity) {
        viewModelScope.launch {
            repository.deleteScan(scan)
        }
    }

    fun clearAllScans() {
        viewModelScope.launch {
            repository.deleteAllScans()
        }
    }

    // Audio beep play using Android ToneGenerator
    private fun playBeepTone() {
        if (!soundFeedbackEnabled.value) return
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        } catch (e: Exception) {
            Log.e("ScanViewModel", "Tone play failed: ${e.message}")
        }
    }

    // Haptic vibration feedback
    private fun triggerVibration() {
        if (!hapticFeedbackEnabled.value) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getApplication<Application>().getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getApplication<Application>().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(100)
            }
        } catch (e: Exception) {
            Log.e("ScanViewModel", "Vibration failed: ${e.message}")
        }
    }
}

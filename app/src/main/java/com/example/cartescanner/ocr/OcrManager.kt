package com.example.cartescanner.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrResult(val rawText: String, val qrCode: String?)

/**
 * Gère l'extraction brute de texte et du QR Code à partir d'un fichier image via Google ML Kit.
 */
class OcrManager(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    suspend fun extractFromImage(imagePath: String): OcrResult {
        val file = File(imagePath)
        if (!file.exists()) throw IllegalArgumentException("Fichier introuvable : $imagePath")

        val image = InputImage.fromFilePath(context, Uri.fromFile(file))

        val text = extractText(image)
        val qr = extractQr(image)

        return OcrResult(rawText = text, qrCode = qr)
    }

    private suspend fun extractText(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { continuation.resume(it.text) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }

    private suspend fun extractQr(image: InputImage): String? = suspendCancellableCoroutine { continuation ->
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes -> continuation.resume(barcodes.firstOrNull()?.rawValue) }
            .addOnFailureListener { continuation.resumeWithException(it) }
    }
}
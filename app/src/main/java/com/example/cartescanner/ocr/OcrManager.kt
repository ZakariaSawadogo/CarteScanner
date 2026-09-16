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

/**
 * Conteneur des donnees extraites visuellement de l'image.
 *
 * @property rawText L'integralite du texte detecte sur l'image.
 * @property qrCode Le contenu eventuel d'un QR code detecte (null si absent).
 */
data class OcrResult(val rawText: String, val qrCode: String?)

/**
 * Service charge de l'analyse d'images via Google ML Kit pour extraire
 * le texte (OCR) et les codes-barres (QR Codes).
 *
 * @property context Contexte applicatif requis par l'API ML Kit.
 */
class OcrManager(private val context: Context) {

    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )

    /**
     * Traite un fichier image physique pour en extraire le texte et un potentiel QR Code.
     *
     * @param imagePath Le chemin absolu du fichier image local.
     * @return [OcrResult] combinant le texte brut et le contenu du QR code.
     * @throws IllegalArgumentException Si le fichier pointe n'existe pas.
     */
    suspend fun extractFromImage(imagePath: String): OcrResult {
        val file = File(imagePath)
        if (!file.exists()) {
            throw IllegalArgumentException("Fichier introuvable : $imagePath")
        }

        val image = InputImage.fromFilePath(context, Uri.fromFile(file))

        val text = extractText(image)
        val qr = extractQr(image)

        return OcrResult(rawText = text, qrCode = qr)
    }

    /**
     * Adapte l'API asynchrone (callback) de TextRecognition aux Coroutines Kotlin.
     */
    private suspend fun extractText(image: InputImage): String = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(image)
            .addOnSuccessListener { result ->
                continuation.resume(result.text)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }

    /**
     * Adapte l'API asynchrone (callback) de BarcodeScanning aux Coroutines Kotlin.
     */
    private suspend fun extractQr(image: InputImage): String? = suspendCancellableCoroutine { continuation ->
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                continuation.resume(barcodes.firstOrNull()?.rawValue)
            }
            .addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
    }
}
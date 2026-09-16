package com.example.cartescanner.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    // Kamera çekimleri için benzersiz isimle boş bir dosya referansı üretir
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_${timeStamp}.jpg"
        return File(context.filesDir, fileName)
    }

    // Galeriden seçilen Uri görselini Context.filesDir içine kopyalayıp yolunu döner
    fun saveUriToFile(context: Context, imageUri: Uri): String? {
        return try {
            val destinationFile = createImageFile(context)
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val outputStream = FileOutputStream(destinationFile)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
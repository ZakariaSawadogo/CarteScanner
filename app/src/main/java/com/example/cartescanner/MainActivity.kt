package com.example.cartescanner

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cartescanner.ui.ScanScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScanScreen(
                onImageAcquired = { imagePath ->
                    Log.d("CarteScanner", "Görsel kaydedildi: $imagePath")
                    Toast.makeText(this, "Kayıt Yolu: $imagePath", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

package com.example.cartescanner.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cartescanner.R
import com.example.cartescanner.data.SettingsManager

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val settingsManager = SettingsManager(this)
        val editApiKey = findViewById<EditText>(R.id.editApiKey)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        editApiKey.setText(settingsManager.getGroqApiKey() ?: "")

        btnSave.setOnClickListener {
            val key = editApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                settingsManager.saveGroqApiKey(key)
                Toast.makeText(this, "Clé sauvegardée et chiffrée", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "La clé ne peut pas être vide", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
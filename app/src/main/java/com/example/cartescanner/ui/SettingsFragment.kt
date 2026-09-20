package com.example.cartescanner.ui

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.cartescanner.R
import com.example.cartescanner.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val settingsManager = SettingsManager(requireContext())

        val editApiKey = view.findViewById<EditText>(R.id.editApiKey)
        val btnSave = view.findViewById<Button>(R.id.btnSaveSettings)
        val indicatorApiKey = view.findViewById<ImageView>(R.id.indicatorApiKey) // Le voyant

        val radioGroupTheme = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioGroupLanguage = view.findViewById<RadioGroup>(R.id.radioGroupLanguage)

        val savedKey = settingsManager.getGroqApiKey() ?: ""
        editApiKey.setText(savedKey)

        if (savedKey.isEmpty()) {
            indicatorApiKey.setImageResource(android.R.drawable.presence_away) // Voyant Jaune
        } else {
            verifyGroqApiKey(savedKey, indicatorApiKey)
        }

        btnSave.setOnClickListener {
            val key = editApiKey.text.toString().trim()
            if (key.isNotEmpty()) {
                settingsManager.saveGroqApiKey(key)
                Toast.makeText(requireContext(), getString(R.string.toast_key_saved), Toast.LENGTH_SHORT).show()
                verifyGroqApiKey(key, indicatorApiKey)
            } else {
                indicatorApiKey.setImageResource(android.R.drawable.presence_away)
                Toast.makeText(requireContext(), getString(R.string.toast_key_empty), Toast.LENGTH_SHORT).show()
            }
        }

        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        when {
            currentLocale.contains("en") -> radioGroupLanguage.check(R.id.radioLangEn)
            currentLocale.contains("tr") -> radioGroupLanguage.check(R.id.radioLangTr)
            else -> radioGroupLanguage.check(R.id.radioLangSystem)
        }

        radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            val localeTag = when (checkedId) {
                R.id.radioLangEn -> "en"
                R.id.radioLangTr -> "tr"
                else -> ""
            }
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(localeTag))
        }

        when (settingsManager.getThemeMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioGroupTheme.check(R.id.radioThemeLight)
            AppCompatDelegate.MODE_NIGHT_YES -> radioGroupTheme.check(R.id.radioThemeDark)
            else -> radioGroupTheme.check(R.id.radioThemeSystem)
        }

        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioThemeLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioThemeDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            settingsManager.saveThemeMode(mode)
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    /**
     * Envoie une requête réseau non-bloquante pour vérifier si la clé Groq est valide.
     */
    private fun verifyGroqApiKey(apiKey: String, indicator: ImageView) {
        indicator.setImageResource(android.R.drawable.presence_away)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var isKeyValid = false
            try {
                val url = URL("https://api.groq.com/openai/v1/models")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                isKeyValid = (responseCode == 200)

                connection.disconnect()
            } catch (e: Exception) {
                isKeyValid = false
            }

            withContext(Dispatchers.Main) {
                if (isKeyValid) {
                    indicator.setImageResource(android.R.drawable.presence_online)
                } else {
                    indicator.setImageResource(android.R.drawable.presence_busy)
                }
            }
        }
    }
}
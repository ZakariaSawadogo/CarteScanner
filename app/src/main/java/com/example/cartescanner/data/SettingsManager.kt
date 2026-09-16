package com.example.cartescanner.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Gère la persistance des préférences utilisateur (comme les clés API).
 */
class SettingsManager(context: Context) {

    /** Crée un fichier XML local et privé sur le téléphone **/
    private val prefs: SharedPreferences = context.getSharedPreferences("scanner_prefs", Context.MODE_PRIVATE)

    /**
     * Sauvegarde la clé API saisie par l'utilisateur.
     * Cette fonction sera appelée par ton collègue depuis l'interface graphique.
     */
    fun saveGroqApiKey(apiKey: String) {
        prefs.edit { putString("GROQ_API_KEY", apiKey) }
    }

    /**
     * Récupère la clé API. Retourne null si l'utilisateur ne l'a pas encore renseignée.
     */
    fun getGroqApiKey(): String? {
        return prefs.getString("GROQ_API_KEY", null)
    }
}
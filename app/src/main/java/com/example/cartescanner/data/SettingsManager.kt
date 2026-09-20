package com.example.cartescanner.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gère la persistance des préférences utilisateur (comme les clés API).
 */
class SettingsManager(context: Context) {

    // Creation d'une cle maitresse cryptographique
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    /** Crée un fichier XML local et privé sur le téléphone **/
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_scanner_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
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

    /**
     * Sauvegarde le choix du theme (Clair, Sombre, ou Systeme).
     */
    fun saveThemeMode(mode: Int) {
        prefs.edit { putInt("THEME_MODE", mode) }
    }

    /**
     * Recupere le theme sauvegarde (par defaut : suit le systeme).
     */
    fun getThemeMode(): Int {
        return prefs.getInt("THEME_MODE", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}
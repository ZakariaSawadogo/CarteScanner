package com.example.cartescanner.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLEncoder


/**
 * Composant de recherche furtive exploitant l'interface HTML statique de DuckDuckGo en requete GET.
 *
 * @property client Instance OkHttpClient utilisee pour executer la requete HTTP.
 */
class DdgSearcher(private val client: OkHttpClient) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val siteLooked= 5
    /**
     * Execute une recherche DuckDuckGo et extrait les snippets textuels des premiers resultats.
     *
     * @param query Termes de recherche a envoyer.
     * @return Extraits textuels concatenes, ou une chaine vide en cas d'erreur ou d'absence de resultats.
     */
    suspend fun fetchSnippets(query: String): String = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://html.duckduckgo.com/html/?q=$encodedQuery"

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", userAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Accept-Language", "fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.e("DdgSearcher", "Erreur HTTP ${response.code}")
                    return@withContext ""
                }

                val doc = Jsoup.parse(html)
                val snippets = doc.select(".result__snippet, .result-snippet")
                    .map { it.text().trim() }
                    .filter { it.isNotBlank() }

                return@withContext snippets.take(siteLooked).joinToString("\n---\n")
            }
        } catch (e: Exception) {
            Log.e("DdgSearcher", "Exception during search: ${e.message}", e)
            return@withContext ""
        }
    }
}
package com.example.cartescanner.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder

/**
 * Composant de recherche furtive exploitant l'interface HTML statique de DuckDuckGo.
 * Récupère le texte ET l'URL décodée des résultats.
 */
class DdgSearcher(private val client: OkHttpClient) {

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    private val siteLooked = 5

    suspend fun fetchSnippets(query: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "https://html.duckduckgo.com/html/"

            val formBody = FormBody.Builder()
                .add("q", query)
                .add("b", "")
                .add("kl", "")
                .add("df", "")
                .build()

            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", userAgent)
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .addHeader("Origin", "https://html.duckduckgo.com")
                .addHeader("Referer", "https://html.duckduckgo.com/")
                .addHeader("Upgrade-Insecure-Requests", "1")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                val html = response.body?.string().orEmpty()

                if (!response.isSuccessful) {
                    Log.e("DdgSearcher", "Erreur HTTP ${response.code}")
                    return@withContext ""
                }

                if (html.contains("if(window.location.href.indexOf('df=')>-1)")) {
                    Log.w("DdgSearcher", "DuckDuckGo a renvoyé un Captcha.")
                    return@withContext ""
                }

                val doc = Jsoup.parse(html)

                val results = doc.select(".result")

                val snippets = results.mapNotNull { element ->
                    val linkElement = element.selectFirst(".result__snippet") ?: element.selectFirst("a.result__url")
                    var rawUrl = linkElement?.attr("href") ?: ""

                    if (rawUrl.contains("uddg=")) {
                        try {
                            val encodedUrl = rawUrl.substringAfter("uddg=").substringBefore("&")
                            rawUrl = URLDecoder.decode(encodedUrl, "UTF-8")
                        } catch (e: Exception) {
                            Log.e("DdgSearcher", "Erreur de décodage URL", e)
                        }
                    } else if (rawUrl.startsWith("//")) {
                        rawUrl = "https:$rawUrl"
                    }

                    val text = element.selectFirst(".result__snippet")?.text()?.trim() ?: ""

                    if (text.isNotBlank() && rawUrl.isNotBlank()) {
                        "URL: $rawUrl\nDescription: $text"
                    } else if (text.isNotBlank()) {
                        "Description: $text"
                    } else {
                        null
                    }
                }

                return@withContext snippets.take(siteLooked).joinToString("\n---\n")
            }
        } catch (e: Exception) {
            Log.e("DdgSearcher", "Exception during search: ${e.message}", e)
            return@withContext ""
        }
    }
}
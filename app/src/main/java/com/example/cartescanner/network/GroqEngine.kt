package com.example.cartescanner.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Donnees d'enrichissement extraites et structurees par le modele de langage.
 *
 * @property name Prenom du contact.
 * @property surname Nom de famille du contact.
 * @property orgName Nom de l'entreprise ou de l'organisation.
 * @property tel Numero de telephone privilegie.
 * @property email Adresse email.
 * @property linkedin Lien vers le profil LinkedIn.
 * @property twitter Lien vers le profil Twitter/X.
 * @property location Localisation geographique.
 * @property others Informations complementaires (ex: poste, notes).
 */
data class AiEnrichedData(
    val name: String?,
    val surname: String?,
    val orgName: String?,
    val position: String?,
    val tel: String?,
    val email: String?,
    val linkedin: String?,
    val twitter: String?,
    val location: String?,
    val others: String?
)

/**
 * DTO interne tolerant permettant d'intercepter tout type JSON pour le champ 'others'
 * et de mapper l'ensemble des champs du contact.
 */
private data class RawAiEnrichedData(
    val name: String?,
    val surname: String?,
    val orgName: String?,
    val position: String?,
    val tel: String?,
    val email: String?,
    val linkedin: String?,
    val twitter: String?,
    val location: String?,
    val others: JsonElement?
)

private data class GroqRequest(
    val model: String,
    val messages: List<Message>,
    @SerializedName("response_format")
    val responseFormat: ResponseFormat
)

private data class Message(
    val role: String,
    val content: String
)

private data class ResponseFormat(
    val type: String
)

private data class GroqResponse(
    val choices: List<Choice>?
)

private data class Choice(
    val message: Message
)

/**
 * Moteur d'inference communiquant avec l'API Groq pour analyser, corriger et synthetiser les donnees.
 *
 * @property client Instance OkHttpClient partagee pour l'execution des requetes HTTP.
 * @property apiKey Cle d'authentification API Groq.
 */
class GroqEngine(private val client: OkHttpClient, private val apiKey: String) {

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Fusionne les donnees locales et les donnees web pour produire un profil de contact complet.
     *
     * @param baseInfo Extractions locales basees sur des regles ou Regex (offline).
     * @param ocrRawText Texte brut issu du scan OCR.
     * @param searchSnippets Extraits web OSINT collectes.
     * @return [AiEnrichedData] contenant les donnees consolidees, ou null en cas d'erreur.
     */
    suspend fun synthesizeData(
        baseInfo: String,
        ocrRawText: String,
        searchSnippets: String
    ): AiEnrichedData? = withContext(Dispatchers.IO) {
        val prompt = """
            Tu es un expert en OSINT et en traitement de donnees non structurees.
            
            CONTEXTE DE LA DONNEE :
            Nous traitons une carte de visite. Voici 3 sources de donnees a ta disposition :
            1. [EXTRACTION LOCALE REGEX] (Fiable mais potentiellement incomplete) : $baseInfo
            2. [TEXTE BRUT OCR] (Peut contenir des fautes de frappe ou erreurs de scan) : $ocrRawText
            3. [RECHERCHE WEB OSINT] (Pour validation et enrichissement) : $searchSnippets
            
            MISSION :
            Croise ces trois sources pour extraire, corriger et enrichir les informations du contact.
            
            REGLES METIER STRICTES :
            - name / surname : Utilise le web pour corriger les erreurs de l'OCR.
            - position : Identifie la position de l'individu dans l'organisation, s'il ne s'agit pas d'individu null.
            - tel : S'il y a plusieurs numeros, privilegie le numero mobile ou la ligne directe.
            - email : Assure-toi que la syntaxe est valide.
            - linkedin / twitter : Doit etre une URL valide.
            - location : Isole la ville, le pays ou l'adresse la plus precise.
            - others : Une seule phrase courte contenant des informations cruciales supplementaires. AUCUN objet imbrique.
            
            CONTRAINTE DE SORTIE :
            Renvoie STRICTEMENT un objet JSON plat. 
            Si une donnee est introuvable dans les 3 sources, assigne la valeur null.
            
            FORMAT ATTENDU :
            {
                "name": "prenom ou null",
                "surname": "nom ou null",
                "orgName": "entreprise ou null",
                "position": "position ou null",
                "tel": "numero ou null",
                "email": "email ou null",
                "linkedin": "url ou null",
                "twitter": "url ou null",
                "location": "lieu ou null",
                "others": "chaine courte ou null"
            }
        """.trimIndent()

        val requestBody = GroqRequest(
            model = "openai/gpt-oss-20b",
            messages = listOf(
                Message(role = "system", content = "Tu es un parseur de donnees JSON strict. Tu ne renvoies que du JSON plat."),
                Message(role = "user", content = prompt)
            ),
            responseFormat = ResponseFormat(type = "json_object")
        )

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(gson.toJson(requestBody).toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e("GroqEngine", "Erreur HTTP ${response.code}: $responseBody")
                    return@withContext null
                }

                if (responseBody.isNullOrBlank()) {
                    return@withContext null
                }

                val groqRes = gson.fromJson(responseBody, GroqResponse::class.java)
                val jsonString = groqRes.choices?.firstOrNull()?.message?.content ?: return@withContext null

                val rawData = gson.fromJson(jsonString, RawAiEnrichedData::class.java)

                val formattedOthers = when {
                    rawData.others == null || rawData.others.isJsonNull -> null
                    rawData.others.isJsonPrimitive -> rawData.others.asString
                    else -> rawData.others.toString()
                }

                return@withContext AiEnrichedData(
                    name = rawData.name,
                    surname = rawData.surname,
                    orgName = rawData.orgName,
                    position = rawData.position,
                    tel = rawData.tel,
                    email = rawData.email,
                    linkedin = rawData.linkedin,
                    twitter = rawData.twitter,
                    location = rawData.location,
                    others = formattedOthers
                )
            }
        } catch (e: Exception) {
            Log.e("GroqEngine", "Exception during synthesis: ${e.message}", e)
            return@withContext null
        }
    }
}
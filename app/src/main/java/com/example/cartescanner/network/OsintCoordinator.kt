package com.example.cartescanner.network

import android.util.Log
import com.example.cartescanner.data.AppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Orchestre l'enrichissement et la correction des donnees d'un contact via la recherche Web et l'IA.
 *
 * @property dao Interface d'acces a la base de donnees locale.
 * @property searcher Composant charge de recuperer les extraits Web pertinents.
 * @property aiEngine Moteur d'inference LLM pour synthetiser et corriger les donnees.
 */
class OsintCoordinator(
    private val dao: AppDao,
    private val searcher: DdgSearcher,
    private val aiEngine: GroqEngine
) {

    /**
     * Execute le pipeline asynchrone complet d'enrichissement d'un contact.
     */
    suspend fun enrichContactData(
        contactId: Long,
        imageId: Long,
        personId: Long?,
        orgId: Long?,
        name: String?,
        surname: String?,
        orgName: String?,
        ocrRawText: String?
    ) = withContext(Dispatchers.IO) {

        val sanitize: (String?) -> String = { text ->
            text?.replace(Regex("[^\\p{L}\\p{N}\\s-]"), "")
                ?.replace(Regex("\\s+"), " ")
                ?.trim() ?: ""
        }

        val cleanName = sanitize(name)
        val cleanSurname = sanitize(surname)
        val cleanOrg = sanitize(orgName)

        val query = when {
            cleanOrg.isNotBlank() && (cleanName.isNotBlank() || cleanSurname.isNotBlank()) ->
                "$cleanName $cleanSurname $cleanOrg linkedin OR twitter OR instagram OR facebook".trim()
            cleanOrg.isNotBlank() ->
                cleanOrg
            else ->
                "$cleanName $cleanSurname linkedin OR twitter".trim()
        }

        var snippets = ""
        if (query.isNotBlank()) {
            snippets = searcher.fetchSnippets(query)

            if (snippets.isBlank() && cleanOrg.isNotBlank()) {
                snippets = searcher.fetchSnippets(cleanOrg)
            }
        }

        if (snippets.isBlank()) {
            Log.w("OsintCoordinator", "Aucun extrait web trouve. L'IA va utiliser uniquement le texte OCR.")
            snippets = "Aucun resultat web disponible. Concentre-toi sur la correction du texte brut de la carte."
        }

        val baseInfo = "Nom: $cleanName $cleanSurname, Organisation: $cleanOrg"

        val enrichedData = aiEngine.synthesizeData(
            baseInfo = baseInfo,
            ocrRawText = ocrRawText ?: "",
            searchSnippets = snippets
        )

        if (enrichedData == null) {
            Log.w("OsintCoordinator", "Echec de la synthese LLM.")
            return@withContext
        }

        val contact = dao.getContactById(contactId)
        if (contact != null) {
            val updatedContact = contact.copy(
                tel = enrichedData.tel ?: contact.tel,
                email = enrichedData.email ?: contact.email,
                linkedin = enrichedData.linkedin ?: contact.linkedin,
                twitter = enrichedData.twitter ?: contact.twitter,
                facebook = enrichedData.facebook ?: contact.facebook,
                instagram = enrichedData.instagram ?: contact.instagram,
                location = enrichedData.location ?: contact.location,
                others = enrichedData.others ?: contact.others
            )
            dao.updateContact(updatedContact)
        }

        personId?.let { id ->
            val person = dao.getPersonById(id)
            if (person != null) {
                val updatedPerson = person.copy(
                    name = enrichedData.name ?: person.name,
                    surname = enrichedData.surname ?: person.surname,
                    position = enrichedData.position ?: person.position
                )
                dao.updatePerson(updatedPerson)
            }
        }

        orgId?.let { id ->
            val organisation = dao.getOrganisationById(id)
            if (organisation != null) {
                val updatedOrg = organisation.copy(
                    organisationName = enrichedData.orgName ?: organisation.organisationName
                )
                dao.updateOrganisation(updatedOrg)
            }
        }

        val image = dao.getImageById(imageId)
        if (image != null) {
            dao.updateImage(image.copy(processCompleted = true))
        }
    }
}
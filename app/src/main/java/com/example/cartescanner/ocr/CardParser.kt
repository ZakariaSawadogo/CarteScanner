package com.example.cartescanner.ocr

/**
 * Modèle intermédiaire contenant les informations extraites après analyse du texte OCR.
 */
data class ParsedCardData(
    val name: String? = null,
    val surname: String? = null,
    val organisationName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val linkedin: String? = null,
    val location: String? = null,
    val others: String? = null,
    val qrCodeText: String? = null
)

/**
 * Analyse le texte brut pour catégoriser les champs pertinents d'une carte de visite.
 */
class CardParser {

    private val phoneRegex = Regex("""(\+?[0-9]{1,4}[-.\s]?)?(\(?\d{2,4}\)?[-.\s]?)?[\d\s.-]{6,14}""")
    private val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val linkedinRegex = Regex("""(?:https?://)?(?:www\.)?linkedin\.com/in/[a-zA-Z0-9_-]+""")

    /**
     * Parse le texte brut et le transforme en objet structuré [ParsedCardData].
     */
    fun parse(rawText: String, qrCode: String? = null): ParsedCardData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val email = emailRegex.find(rawText)?.value
        val linkedin = linkedinRegex.find(rawText)?.value
        val phone = lines.firstOrNull { phoneRegex.matches(it) || it.contains(phoneRegex) }
            ?.let { phoneRegex.find(it)?.value?.trim() }

        // Exclusion des lignes techniques déjà identifiées pour isoler nom et organisation
        val contentLines = lines.filter { line ->
            line != phone && line != email && line != linkedin &&
                    !line.contains("@") && !line.startsWith("www.", ignoreCase = true)
        }

        // Heuristique : première ligne valide = identité, deuxième ligne = organisation/titre
        val fullName = contentLines.getOrNull(0)
        val orgCandidate = contentLines.getOrNull(1)

        val (name, surname) = splitFullName(fullName)

        return ParsedCardData(
            name = name,
            surname = surname,
            organisationName = orgCandidate,
            phone = phone,
            email = email,
            linkedin = linkedin,
            location = null,
            others = rawText.take(500),
            qrCodeText = qrCode
        )
    }

    private fun splitFullName(fullName: String?): Pair<String?, String?> {
        if (fullName.isNullOrBlank()) return Pair(null, null)
        val parts = fullName.split(" ").filter { it.isNotBlank() }
        return when {
            parts.size >= 2 -> Pair(parts.first(), parts.drop(1).joinToString(" "))
            parts.size == 1 -> Pair(parts.first(), null)
            else -> Pair(null, null)
        }
    }
}
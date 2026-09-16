package com.example.cartescanner.ocr

/**
 * Modele de transfert de donnees (DTO) contenant les informations extraites localement.
 * Ces donnees servent de socle de base (hors-ligne) avant la correction sementique par l'IA.
 *
 * @property name Prenom deduit du texte.
 * @property surname Nom de famille deduit du texte.
 * @property organisationName Nom de l'entreprise deduit.
 * @property phone Numero de telephone identifie.
 * @property email Adresse email identifiee.
 * @property linkedin Lien LinkedIn identifie.
 * @property location Localisation (generalement vide a ce stade).
 * @property others Informations complementaires.
 * @property qrCodeText Texte brut decode depuis un eventuel QR Code.
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
 * Analyseur syntaxique base sur des expressions regulieres (Regex) pour categoriser
 * le texte brut d'une carte de visite sans necessiter de connexion Internet.
 */
class CardParser {

    private val phoneRegex = Regex("""(\+?[0-9]{1,4}[-.\s]?)?(\(?\d{2,4}\)?[-.\s]?)?[\d\s.-]{6,14}""")
    private val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val linkedinRegex = Regex("""(?:https?://)?(?:www\.)?linkedin\.com/in/[a-zA-Z0-9_-]+""")

    /**
     * Evalue le texte brut et tente d'isoler les champs structurables.
     *
     * @param rawText Texte brut genere par l'OCR.
     * @param qrCode Texte extrait du QR Code (le cas echeant).
     * @return [ParsedCardData] contenant les donnees isolees localement.
     */
    fun parse(rawText: String, qrCode: String? = null): ParsedCardData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }

        val email = emailRegex.find(rawText)?.value
        val linkedin = linkedinRegex.find(rawText)?.value
        val phone = lines.firstOrNull { phoneRegex.matches(it) || it.contains(phoneRegex) }
            ?.let { phoneRegex.find(it)?.value?.trim() }

        val contentLines = lines.filter { line ->
            line != phone && line != email && line != linkedin &&
                    !line.contains("@") && !line.startsWith("www.", ignoreCase = true)
        }

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
            others = null,
            qrCodeText = qrCode
        )
    }

    /**
     * Separe une chaine de caracteres representant un nom complet en prenom et nom.
     *
     * @param fullName La chaine complete a scinder.
     * @return Une paire (Prenom, Nom de famille).
     */
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
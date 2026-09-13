package com.example.cartescanner.data

/**
 * Dépôt (Repository) centralisant la logique d'insertion et de mise à jour des scans.
 *
 * @property appDao L'objet d'accès aux données généré par Room.
 */
class ScanRepository(private val appDao: AppDao) {

    /**
     * Enregistre le chemin d'une nouvelle image capturée.
     *
     * @param imagePath Le chemin absolu de l'image stockée localement.
     * @return L'identifiant (ID) de l'image nouvellement insérée.
     */
    suspend fun saveInitialImage(imagePath: String): Long {
        val newImage = ImageEntity(imagePath = imagePath, processed = false, processCompleted = false)
        return appDao.insertImage(newImage)
    }

    /**
     * Sauvegarde les entités structurées extraites par l'OCR et crée les relations.
     *
     * @param imageId L'identifiant de l'image source.
     * @param name Le prénom extrait (optionnel).
     * @param surname Le nom de famille extrait (optionnel).
     * @param orgName Le nom de l'organisation extrait (optionnel).
     * @param phone Le numéro de téléphone extrait (optionnel).
     */
    suspend fun saveExtractedData(
        imageId: Long,
        name: String?,
        surname: String?,
        orgName: String?,
        phone: String?,
        qrCode: String?
    ) {
        val contactId = appDao.insertContact(
            ContactEntity(tel = phone, linkedin = null, location = null, twitter = null, whatsapp = null, facebook = null, others = null, qrCodeText = qrCode)
        )

        var orgId: Long? = null
        if (!orgName.isNullOrEmpty()) {
            orgId = appDao.insertOrganisation(
                OrganisationEntity(organisationName = orgName, organisationType = null, contactId = contactId)
            )
        }

        var personId: Long? = null
        if (!name.isNullOrEmpty() || !surname.isNullOrEmpty()) {
            personId = appDao.insertPerson(
                PersonEntity(name = name, surname = surname, position = null, organisationId = orgId, contactId = contactId)
            )
        }

        appDao.insertScan(ScanEntity(imageId = imageId, personId = personId, organisationId = orgId))

        val imageToUpdate = appDao.getImageById(imageId)
        if (imageToUpdate != null) {
            appDao.updateImage(imageToUpdate.copy(processed = true))
        }
    }

    /**
     * Exécute le pipeline complet : extraction OCR, parsing des champs et persistance en base.
     *
     * @param imageId L'identifiant de l'enregistrement dans tbl_image.
     * @param imagePath Le chemin physique du fichier à analyser.
     * @param ocrManager Instance pour l'extraction de texte.
     * @param parser Instance pour l'analyse lexicale.
     */
    suspend fun processImagePipeline(
        imageId: Long,
        imagePath: String,
        ocrManager: com.example.cartescanner.ocr.OcrManager,
        parser: com.example.cartescanner.ocr.CardParser
    ) {
        // 1. Extraction OCR
        val result = ocrManager.extractFromImage(imagePath)

        // 2. Structuration lexicale
        val parsedData = parser.parse(rawText=result.rawText, qrCode = result.qrCode)

        // 3. Persistance dans les tables relationnelles
        saveExtractedData(
            imageId = imageId,
            name = parsedData.name,
            surname = parsedData.surname,
            orgName = parsedData.organisationName,
            phone = parsedData.phone,
            qrCode = parsedData.qrCodeText
        )
    }
}
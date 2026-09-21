package com.example.cartescanner.data

import androidx.room.withTransaction

/**
 * Depot (Repository) centralisant la logique d'insertion et de mise a jour des scans.
 *
 * @property database L'instance de la base de donnees pour gerer les transactions.
 */
class ScanRepository(private val database: AppDatabase) {

    private val appDao = database.appDao()

    /**
     * Enregistre le chemin d'une nouvelle image capturee.
     *
     * @param imagePath Le chemin absolu de l'image stockee localement.
     * @return L'identifiant (ID) de l'image nouvellement inseree.
     */
    suspend fun saveInitialImage(imagePath: String): Long {
        val newImage = ImageEntity(imagePath = imagePath, processed = false, processCompleted = false)
        return appDao.insertImage(newImage)
    }

    /**
     * Sauvegarde les entites structurees extraites par l'OCR et cree les relations.
     */
    suspend fun saveExtractedData(
        imageId: Long,
        name: String?,
        surname: String?,
        orgName: String?,
        phone: String?,
        email: String?,
        qrCode: String?,
        rawText: String?
    ) {
        database.withTransaction {
            val contactId = appDao.insertContact(
                ContactEntity(
                    tel = phone,
                    email = email,
                    linkedin = null,
                    location = null,
                    twitter = null,
                    whatsapp = null,
                    instagram = null,
                    facebook = null,
                    others = null,
                    qrCodeText = qrCode
                )
            )

            var orgId: Long? = null
            if (!orgName.isNullOrEmpty()) {
                orgId = appDao.insertOrganisation(
                    OrganisationEntity(
                        organisationName = orgName,
                        organisationType = null,
                        contactId = contactId
                    )
                )
            }

            var personId: Long? = null
            if (!name.isNullOrEmpty() || !surname.isNullOrEmpty()) {
                personId = appDao.insertPerson(
                    PersonEntity(
                        name = name,
                        surname = surname,
                        position = null,
                        organisationId = orgId,
                        contactId = contactId
                    )
                )
            }

            appDao.insertScan(
                ScanEntity(
                    imageId = imageId,
                    personId = personId,
                    organisationId = orgId,
                    rawText = rawText
                )
            )

            val imageToUpdate = appDao.getImageById(imageId)
            if (imageToUpdate != null) {
                appDao.updateImage(imageToUpdate.copy(processed = true))
            }
        }
    }

    /**
     * Execute le pipeline complet : extraction OCR, parsing des champs et persistance en base.
     */
    suspend fun processImagePipeline(
        imageId: Long,
        imagePath: String,
        ocrManager: com.example.cartescanner.ocr.OcrManager,
        parser: com.example.cartescanner.ocr.CardParser
    ) {
        val result = ocrManager.extractFromImage(imagePath)
        val parsedData = parser.parse(rawText = result.rawText, qrCode = result.qrCode)

        saveExtractedData(
            imageId = imageId,
            name = parsedData.name,
            surname = parsedData.surname,
            orgName = parsedData.organisationName,
            phone = parsedData.phone,
            email = parsedData.email,
            qrCode = parsedData.qrCodeText,
            rawText = result.rawText
        )
    }
}
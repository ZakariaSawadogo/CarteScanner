package com.example.cartescanner.network

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.cartescanner.data.AppDatabase
import okhttp3.OkHttpClient

/**
 * Tache d'arriere-plan garantie par le systeme pour enrichir les contacts en attente.
 */
class OsintWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.appDao()
        val client = OkHttpClient()

        val settings = com.example.cartescanner.data.SettingsManager(applicationContext)
        val apiKey = settings.getGroqApiKey()

        if (apiKey.isNullOrBlank()) {
            return Result.failure()
        }

        val searcher = DdgSearcher(client)
        val aiEngine = GroqEngine(client, apiKey)
        val coordinator = OsintCoordinator(dao, searcher, aiEngine)

        val pendingImages = dao.getPendingImages()

        if (pendingImages.isEmpty()) {
            return Result.success()
        }

        for (image in pendingImages) {
            val scan = dao.getScanByImageId(image.id) ?: continue
            val person = scan.personId?.let { dao.getPersonById(it) }
            val org = scan.organisationId?.let { dao.getOrganisationById(it) }

            val contactId = person?.contactId ?: org?.contactId ?: continue

            coordinator.enrichContactData(
                contactId = contactId,
                imageId = image.id,
                personId = person?.id,
                orgId = org?.id,
                name = person?.name,
                surname = person?.surname,
                orgName = org?.organisationName,
                ocrRawText = scan.rawText
            )
        }

        return Result.success()
    }
}
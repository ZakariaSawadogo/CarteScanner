package com.example.cartescanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cartescanner.data.AppDatabase
import com.example.cartescanner.data.ScanRepository
import com.example.cartescanner.network.OsintWorker
import com.example.cartescanner.ocr.CardParser
import com.example.cartescanner.ocr.OcrManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Représente les différents états de l'écran de scan.
 */
sealed class ScannerState {
    object Idle : ScannerState()
    object Processing : ScannerState()
    data class Success(val imageId: Long) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

/**
 * Le pont entre l'interface utilisateur et la logique backend.
 */
class ScannerViewModel(
    application: Application,
    private val repository: ScanRepository,
    private val ocrManager: OcrManager,
    private val parser: CardParser
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ScannerState>(ScannerState.Idle)
    val state: StateFlow<ScannerState> = _state.asStateFlow()

    /**
     * Fonction appelée par l'interface dès que la photo est validée.
     */
    fun processScannedImage(imagePath: String) {
        viewModelScope.launch {
            _state.value = ScannerState.Processing

            try {
                val imageId = repository.saveInitialImage(imagePath)

                repository.processImagePipeline(imageId, imagePath, ocrManager, parser)

                scheduleOsintWork()

                _state.value = ScannerState.Success(imageId)

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = ScannerState.Error(e.message ?: "Erreur lors de l'extraction")
            }
        }
    }

    /**
     * Planifie le Worker d'enrichissement. Il ne s'exécutera que si le réseau est disponible.
     */
    private fun scheduleOsintWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<OsintWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication()).enqueue(workRequest)
    }

    /**
     * Remet l'état à zéro pour scanner une nouvelle carte.
     */
    fun resetState() {
        _state.value = ScannerState.Idle
    }

    /**
    * L'usine (Factory) qui indique à Android comment créer ce ViewModel
    * avec toutes ses dépendances (Base de données, OCR, Parser).
    **/
    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])

                val database = AppDatabase.getDatabase(application)
                val repository = ScanRepository(database)
                val ocrManager = OcrManager(application)
                val parser = CardParser()

                return ScannerViewModel(application, repository, ocrManager, parser) as T
            }
        }
    }
}
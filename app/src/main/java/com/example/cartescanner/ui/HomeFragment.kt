package com.example.cartescanner.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cartescanner.R
import com.example.cartescanner.data.AppDatabase
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: ScannerViewModel by viewModels { ScannerViewModel.Factory }

    private lateinit var adapter: ScanHistoryAdapter

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            processGalleryImage(uri)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cardScan = view.findViewById<MaterialCardView>(R.id.cardScanCamera)
        val cardImport = view.findViewById<MaterialCardView>(R.id.cardImportGallery)
        val recyclerRecent = view.findViewById<RecyclerView>(R.id.recyclerRecentScans)

        adapter = ScanHistoryAdapter()
        recyclerRecent.layoutManager = LinearLayoutManager(requireContext())
        recyclerRecent.adapter = adapter

        cardScan.setOnClickListener {
            startActivity(Intent(requireContext(), ScannerActivity::class.java))
        }

        cardImport.setOnClickListener {
            pickMedia.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        val dao = AppDatabase.getDatabase(requireContext()).appDao()
        viewLifecycleOwner.lifecycleScope.launch {
            dao.getAllScansFlow().collect { scans ->
                adapter.submitList(scans.take(3))
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ScannerState.Processing -> {
                        Toast.makeText(requireContext(), "Analyse de l'image...", Toast.LENGTH_SHORT).show()
                    }
                    is ScannerState.Success -> {
                        val intent = Intent(requireContext(), ResultActivity::class.java)
                        intent.putExtra("IMAGE_ID", state.imageId)
                        startActivity(intent)
                        viewModel.resetState()
                    }
                    is ScannerState.Error -> {
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }
    }

    /**
     * Copie l'image sélectionnée depuis la galerie vers le cache de l'app,
     * puis lance le pipeline OSINT dessus.
     */
    private fun processGalleryImage(uri: Uri) {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val tempFile = File(requireContext().cacheDir, "imported_card_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)

            inputStream?.copyTo(outputStream)

            inputStream?.close()
            outputStream.close()

            viewModel.processScannedImage(tempFile.absolutePath)

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Erreur lors de l'importation", Toast.LENGTH_SHORT).show()
        }
    }
}
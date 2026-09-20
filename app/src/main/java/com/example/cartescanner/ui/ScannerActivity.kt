package com.example.cartescanner.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cartescanner.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ScannerActivity : AppCompatActivity() {

    private val viewModel: ScannerViewModel by viewModels { ScannerViewModel.Factory }

    private var imageCapture: ImageCapture? = null
    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: FloatingActionButton
    private lateinit var loadingOverlay: View
    private lateinit var progressBar: View

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Permission caméra requise", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scanner)

        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        progressBar = findViewById(R.id.progressBar)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        btnCapture.setOnClickListener { takePhoto() }

        observeViewModel()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("ScannerActivity", "Echec du binding de la camera", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Creation d'un fichier temporaire dans le cache
        val photoFile = File(
            cacheDir,
            SimpleDateFormat("yyyyMMdd-HHmmss", Locale.FRANCE).format(System.currentTimeMillis()) + ".jpg"
        )
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("ScannerActivity", "Echec capture photo: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // La photo est prise, on lance le pipeline de traitement backend !
                    viewModel.processScannedImage(photoFile.absolutePath)
                }
            }
        )
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {
                    is ScannerState.Idle -> {
                        loadingOverlay.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        btnCapture.isEnabled = true
                    }
                    is ScannerState.Processing -> {
                        loadingOverlay.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        btnCapture.isEnabled = false
                    }
                    is ScannerState.Success -> {
                        Toast.makeText(this@ScannerActivity, "Extraction réussie !", Toast.LENGTH_SHORT).show()
                        val intent = android.content.Intent(this@ScannerActivity, ResultActivity::class.java)
                        intent.putExtra("IMAGE_ID", state.imageId)
                        startActivity(intent)

                        viewModel.resetState()
                        finish()
                    }
                    is ScannerState.Error -> {
                        Toast.makeText(this@ScannerActivity, state.message, Toast.LENGTH_LONG).show()
                        viewModel.resetState()
                    }
                }
            }
        }
    }
}
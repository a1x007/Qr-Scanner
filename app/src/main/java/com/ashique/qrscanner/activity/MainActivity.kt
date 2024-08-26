package com.ashique.qrscanner.activity

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Surface
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.ScanImageActivity.Companion.OPEN_GALLERY
import com.ashique.qrscanner.databinding.ActivityMainBinding
import com.ashique.qrscanner.fragments.ResultFragment
import com.ashique.qrscanner.services.PermissionManager.initPermissionManager
import com.ashique.qrscanner.services.PermissionManager.isCameraPermissionGranted
import com.ashique.qrscanner.services.PermissionManager.requestCameraPermission
import com.ashique.qrscanner.utils.Extensions.applyDayNightTheme
import com.ashique.qrscanner.utils.Extensions.navigateTo
import com.ashique.qrscanner.utils.Extensions.setOnBackPressedAction
import com.ashique.qrscanner.utils.Extensions.showToast
import com.ashique.qrscanner.utils.Prefs
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private lateinit var ui: ActivityMainBinding


    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "MainActivity"
    }

    private lateinit var cameraSelector: CameraSelector
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var previewUseCase: Preview
    private lateinit var analysisUseCase: ImageAnalysis


    private var flashEnabled = false
    private var isProcessingResult = false

    private val screenAspectRatio: Int by lazy {
        val metrics = DisplayMetrics().also { ui.previewView.display?.getRealMetrics(it) }
        aspectRatio(metrics.widthPixels, metrics.heightPixels)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ui = ActivityMainBinding.inflate(layoutInflater)

        applyDayNightTheme()

        setContentView(ui.root)

        Prefs.initialize(this)

        initPermissionManager()

        // Set initial state based on current mode
        ui.switchDayNight.isChecked = isNightModeActive()

        // Handle switch toggle
        ui.switchDayNight.setOnCheckedListener { isChecked ->
            if (isChecked) {
                // Night mode is selected
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                ui.switchDayNight.setIcon(R.drawable.ic_night)

            } else {
                // Day mode is selected
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                ui.switchDayNight.setIcon(R.drawable.ic_day)
            }
        }
        ui.upload.setOnClickListener {
            navigateTo<ScanImageActivity>(Bundle().apply {
                putBoolean(OPEN_GALLERY, true)  // Pass a flag to indicate gallery should be opened
            })
        }

        ui.overlay.setViewFinder()
       // ui.settingBtn.setOnClickListener { navigateTo<SettingsActivity>() }
       // ui.generatorBtn.setOnClickListener { navigateTo<GeneratorActivity>() }

        ui.btnGenerate.setOnClickListener {
            navigateTo<QrGenerator>()
        }


        ui.cameraError.setOnClickListener { requestCameraPermission() }

        if (isCameraPermissionGranted()) {
            setupCamera()
            ui.cameraError.visibility = GONE
        } else {
            showToast("Requesting permission for camera")
            ui.cameraError.visibility = VISIBLE
            ui.scanner.visibility = GONE
        }


        checkCameraPermission()

        onBackPress()


    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            setupCamera()
        }
    }




    private fun openResultActivity(result: String) {
        val intent = ResultActivity.newIntent(this, result)
        startActivity(intent)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setupCamera()
                ui.cameraError.visibility = GONE
                ui.scanner.visibility = VISIBLE
                showToast("Permission for camera has been granted")
            } else {
                // Handle the case where the user denied the permission
                ui.cameraError.visibility = VISIBLE
                ui.scanner.visibility = GONE
                showToast("Permission for camera has been denied")
            }
        }
    }

    private fun onBackPress() {
        // Set back press action using extension function
        setOnBackPressedAction {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } /*else if (ui.cropImageView.drawable != null) {
                // Remove the bitmap
                ui.cropImageView.setImageDrawable(null)
                ui.cropBackground.visibility = GONE
                ui.toolbar.visibility = GONE
            }
             */
            else {
                finish()
            }
        }
    }


    private fun isNightModeActive(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }


    private fun setupCamera() {
        cameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                bindCameraUseCases()
            },
            ContextCompat.getMainExecutor(this)
        )
    }

    private fun bindCameraUseCases() {
        bindPreviewUseCase()
        bindAnalyseUseCase()
    }

    private fun bindPreviewUseCase() {
        if (::previewUseCase.isInitialized)
            cameraProvider.unbind(previewUseCase)

        previewUseCase = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(Surface.ROTATION_0)
            .build()
        previewUseCase.setSurfaceProvider(ui.previewView.surfaceProvider)

        try {
            val camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                previewUseCase
            )
            if (camera.cameraInfo.hasFlashUnit()) {

                ui.flash.setOnClickListener {
                    camera.cameraControl.enableTorch(!flashEnabled)
                }

                camera.cameraInfo.torchState.observe(this) {
                    it?.let { torchState ->
                        ui.flash.apply {
                            if (torchState == TorchState.ON) {
                                flashEnabled = true
                                setMinAndMaxFrame(0, 23)

                            } else {
                                flashEnabled = false
                                setMinAndMaxFrame(23, 33)

                            }
                            speed = 0.2f
                            repeatCount = 0
                            playAnimation()
                        }
                    }
                }
            }

        } catch (illegalStateException: IllegalStateException) {
            illegalStateException.printStackTrace()
        } catch (illegalArgumentException: IllegalArgumentException) {
            illegalArgumentException.printStackTrace()
        }
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun bindAnalyseUseCase() {
        if (::analysisUseCase.isInitialized)
            cameraProvider.unbind(analysisUseCase)

        analysisUseCase = ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(Surface.ROTATION_0)
            .build()

        val cameraExecutor = Executors.newSingleThreadExecutor()

        analysisUseCase.setAnalyzer(cameraExecutor) { imageProxy ->
            processImageProxy(imageProxy)
        }
        try {
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                analysisUseCase
            )
        } catch (illegalStateException: IllegalStateException) {
            illegalStateException.printStackTrace()
        } catch (illegalArgumentException: IllegalArgumentException) {
            illegalArgumentException.printStackTrace()
        }
    }


    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        imageProxy: ImageProxy
    ) {
        val barcodeScanner = BarcodeScanning.getClient()
        if (isProcessingResult) {
            imageProxy.close()
            return
        }

        imageProxy.image?.let { image ->
            val inputImage =
                InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    barcodes.forEach { barcode ->
                        barcode.rawValue?.let {
                            isProcessingResult = true
                            openResultActivity(it)
                        }
                    }
                }
                .addOnFailureListener { error ->
                    error.printStackTrace()
                    showToast("Error: $error")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }


    override fun onResume() {
        super.onResume()
        isProcessingResult = false
        ui.cameraError.isVisible = !isCameraPermissionGranted()
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}

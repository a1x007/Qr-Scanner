package com.ashique.qrscanner.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.ashique.qrscanner.databinding.ActivityMainBinding
import com.ashique.qrscanner.fragments.ResultFragment
import com.ashique.qrscanner.helper.BitmapHelper
import com.ashique.qrscanner.helper.BitmapHelper.invertColors
import com.ashique.qrscanner.helper.BitmapHelper.isGrayscale
import com.ashique.qrscanner.helper.Extensions.applyDayNightTheme
import com.ashique.qrscanner.helper.Extensions.navigateTo
import com.ashique.qrscanner.helper.Extensions.setOnBackPressedAction
import com.ashique.qrscanner.helper.Extensions.showToast
import com.ashique.qrscanner.services.PermissionManager.initPermissionManager
import com.ashique.qrscanner.services.PermissionManager.isAllFilesAccessGranted
import com.ashique.qrscanner.services.PermissionManager.isCameraPermissionGranted
import com.ashique.qrscanner.services.PermissionManager.requestCameraPermission
import com.ashique.qrscanner.services.PermissionManager.requestExternalStoragePermissions
import com.ashique.qrscanner.services.PermissionManager.requestManageAllFilesPermission
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.isseiaoki.simplecropview.CropImageView
import com.isseiaoki.simplecropview.callback.CropCallback
import com.isseiaoki.simplecropview.callback.LoadCallback
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

    private lateinit var getImageLauncher: ActivityResultLauncher<Intent>


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

        initPermissionManager()

        // Set initial state based on current mode
        ui.switchDayNight.isChecked = isNightModeActive()

        // Handle switch toggle
        ui.switchDayNight.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Night mode is selected
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                // Day mode is selected
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
        ui.upload.setOnClickListener { openGalleryAndScanQR() }
        ui.overlay.setViewFinder()
        ui.resultBtn.setOnClickListener { displayResult("this is test") }

        ui.generateBtn.setOnClickListener {
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
        onImagePicked()
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


    private fun displayResult(result: String) {
        val fragment = ResultFragment.newInstance(result)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
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
                ui.fragmentContainer.visibility = GONE
            } else if (ui.cropImageView.drawable != null) {
                // Remove the bitmap
                ui.cropImageView.setImageDrawable(null)
                getImageLauncher.unregister()
                ui.cropBackground.visibility = GONE
                ui.toolbar.visibility = GONE
            } else {
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


    private fun openGalleryAndScanQR() {
        if (isAllFilesAccessGranted()) {
            // Permissions are already granted
            launchGallery()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Log the condition for debugging
                Log.d("PermissionCheck", "Requesting Manage All Files permission")
                requestManageAllFilesPermission()
            } else {
                // Log the condition for debugging
                Log.d("PermissionCheck", "Requesting External Storage permissions")
                requestExternalStoragePermissions()
            }
        }
    }


    private fun launchGallery() {
        Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }.also { pickIntent ->
            getImageLauncher.launch(pickIntent)
        }
    }


    private fun onImagePicked() {
        // Initialize the image selection launcher
        getImageLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    val imageUri = result.data?.data
                    imageUri?.let { uri ->
                        // Use a cropping library or your own cropping logic here
                        ui.cropImageView.load(uri).execute(object : LoadCallback {
                            override fun onError(e: Throwable?) {
                                Log.e(TAG, "Error loading image", e)
                            }

                            override fun onSuccess() {
                                ui.cropBackground.visibility = VISIBLE
                                ui.toolbar.visibility = VISIBLE
                                // rotation
                                ui.rotateLeft.setOnClickListener {
                                    ui.cropImageView.rotateImage(
                                        CropImageView.RotateDegrees.ROTATE_M90D
                                    )
                                }
                                ui.rotateRight.setOnClickListener {
                                    ui.cropImageView.rotateImage(
                                        CropImageView.RotateDegrees.ROTATE_90D
                                    )
                                }

                                // Set up cropping
                                ui.crop.setOnClickListener {
                                    ui.cropImageView.crop(uri)
                                        .execute(object : CropCallback {
                                            override fun onSuccess(bitmap: Bitmap) {
                                                val grayscaleBitmap =
                                                    if (isGrayscale(bitmap)) {
                                                        BitmapHelper.resizeBitmap(
                                                            bitmap,
                                                            400,
                                                            400
                                                        )
                                                    } else {
                                                        invertColors(
                                                            bitmap
                                                        )
                                                    }

                                                Log.i(TAG, "onSuccess: crop: $bitmap")
                                                processBitmap(grayscaleBitmap)
                                            }

                                            override fun onError(e: Throwable) {
                                                Log.e(TAG, "Error cropping image", e)
                                            }
                                        })
                                }
                            }
                        })
                    }
                }
            }
    }


    private fun processBitmap(bitmap: Bitmap) {
        val barcodeScanner = BarcodeScanning.getClient()

        val inputImage = InputImage.fromBitmap(bitmap, 0) // Rotation is 0 for bitmap

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach { barcode ->
                    barcode.rawValue?.let {
                        Log.d("TAG", "QR Code content: $it")
                        // You can call your function to handle the QR code result here
                        openResultActivity(it)
                    }
                }
            }
            .addOnFailureListener { error ->
                error.printStackTrace()
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
        getImageLauncher.unregister()
    }

}

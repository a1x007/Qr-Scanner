package com.ashique.qrscanner.activity

import android.app.ComponentCaller
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ashique.qrscanner.R
import com.ashique.qrscanner.activity.ResultActivity.Companion.EXTRA_RESULT_URL
import com.ashique.qrscanner.custom.CropImageView
import com.ashique.qrscanner.databinding.ActivityScanImageBinding
import com.ashique.qrscanner.utils.Extensions.navigateTo
import com.ashique.qrscanner.utils.Extensions.parcelable
import com.ashique.qrscanner.utils.Extensions.showToast
import com.ashique.qrscanner.helper.QrScanner.scanQrBitmap
import com.ashique.qrscanner.services.PermissionManager.isAllFilesAccessGranted
import com.ashique.qrscanner.services.PermissionManager.requestExternalStoragePermissions
import com.ashique.qrscanner.services.PermissionManager.requestManageAllFilesPermission
import com.isseiaoki.simplecropview.callback.LoadCallback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.isseiaoki.simplecropview.CropImageView as cropImage

class ScanImageActivity : AppCompatActivity() {

    private lateinit var ui: ActivityScanImageBinding

    private lateinit var cropImageView: CropImageView
    private var scanJob: Job? = null
    private var lastCroppedBitmap: Bitmap? = null

    private lateinit var photoPickerLauncher: ActivityResultLauncher<String>
    private var qrContents: String? = ""


    companion object {
        private const val TAG = "ScanImageActivity"
        const val OPEN_GALLERY = "OPEN_GALLERY"
        const val IMAGE_URI = "IMAGE_URI"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityScanImageBinding.inflate(layoutInflater)
        setContentView(ui.root)

        cropImageView = ui.cropImageView

        // Initialize the ActivityResultLauncher
        photoPickerLauncher = registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            uri?.let {
                handleImageUri(uri)
            }
        }

        handleIncomingIntent(intent)

        // Observe touch events
        cropImageView.onScanRequested = {
            handleTouchEvent()
        }

        ui.gallery.setOnClickListener { openGalleryAndScanQR() }
        ui.crop.setOnClickListener { qrContents?.let { result -> openResultActivity(result) } }

        ui.rotateLeft.setOnClickListener {
            ui.cropImageView.rotateImage(
                cropImage.RotateDegrees.ROTATE_M90D
            )
        }

        ui.rotateRight.setOnClickListener {
            ui.cropImageView.rotateImage(
                cropImage.RotateDegrees.ROTATE_90D
            )
        }
    }


    private fun openGalleryAndScanQR() {
        if (isAllFilesAccessGranted()) {
            // Permissions are already granted
            photoPickerLauncher.launch("image/*")
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


    private fun handleIncomingIntent(intent: Intent) {
        when {
            intent.getBooleanExtra(OPEN_GALLERY, false) -> openGalleryAndScanQR()
            intent.action == Intent.ACTION_SEND -> {
                intent.parcelable<Uri>(Intent.EXTRA_STREAM)?.let {
                    handleImageUri(it)
                } ?: run {
                    showToast("No image URI found")
                    Log.e(TAG, "No image URI found")
                }
            }

            else -> {
                Log.e(TAG, "Unhandled intent action: ${intent.action}")
            }
        }
    }


    private fun handleImageUri(uri: Uri) {
        cropImageView.load(uri).execute(object : LoadCallback {
            override fun onError(e: Throwable?) {
                Log.e(TAG, "Error loading image", e)
            }

            override fun onSuccess() {
                handleTouchEvent()
            }
        })
    }

    private fun handleTouchEvent() {
        scanJob?.cancel()
        scanJob = lifecycleScope.launch(Dispatchers.IO) {
            delay(400)
            val croppedBitmap = cropImageView.croppedBitmap
            if (croppedBitmap != lastCroppedBitmap) {
                lastCroppedBitmap = croppedBitmap
                qrContents = scanQrBitmap(croppedBitmap)
                withContext(Dispatchers.Main) {
                    if (qrContents != null) {
                        ui.qrDetectedText.apply {
                            setTextColor(Color.GREEN)
                            text = "QR CODE DETECTED."
                        }
                        ui.icon.setIcon(R.drawable.ic_success, Color.GREEN)
                        showToast("Scanned: $qrContents")
                    } else {
                        ui.qrDetectedText.apply {
                            setTextColor(Color.RED)
                            text = "NO QR CONTENT FOUND."
                        }
                        ui.icon.setIcon(R.drawable.ic_error, Color.RED)
                        showToast("No Qr Content Found")
                    }
                }
            }
        }
    }

    private fun openResultActivity(result: String) {
        navigateTo<ResultActivity>(Bundle().apply {
            putString(EXTRA_RESULT_URL, result)
        })

    }

    override fun onNewIntent(intent: Intent, caller: ComponentCaller) {
        super.onNewIntent(intent, caller)
        handleIncomingIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        scanJob?.cancel()
    }
}

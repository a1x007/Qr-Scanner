package com.ashique.qrscanner.services

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ashique.qrscanner.helper.Extensions.showToast

object PermissionManager {

    private var activity: AppCompatActivity? = null

    private var manageAllFilesPermissionLauncher: ActivityResultLauncher<Intent>? = null
    private var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>? = null

    fun Activity.initPermissionManager() {
        if (this is AppCompatActivity) {
            activity = this

            manageAllFilesPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.StartActivityForResult()
            ) {
                // Handle the result if needed
                if (activity?.isAllFilesAccessGranted() == true) {
                    // Permission granted
                    showToast("Permission has been granted")
                } else {
                    // Permission denied
                    showToast("Permission has been denied! Retrying...")
                    // Optionally re-request permission or guide user
                }
            }

            requestPermissionsLauncher = registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                permissions.entries.forEach {
                    if (!it.value) {
                        // Permission denied
                        showToast("Permission has been denied! Retrying...")
                        // Optionally re-request permission or guide user
                    }
                }
                // Permissions granted or already had them
                showToast("Permissions have already been granted")
            }
        } else {
            throw IllegalStateException("Activity must be an instance of AppCompatActivity")
        }
    }

    fun AppCompatActivity.isAllFilesAccessGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun Context.isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun Context.requestManageAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                (this as? AppCompatActivity)?.let {
                    manageAllFilesPermissionLauncher?.launch(intent)
                }
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                (this as? AppCompatActivity)?.let {
                    manageAllFilesPermissionLauncher?.launch(intent)
                }
            }
        }
    }

    fun Context.requestExternalStoragePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            (this as? AppCompatActivity)?.let {
                requestPermissionsLauncher?.launch(
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                )
            }
        }
    }

    fun Context.requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this as Activity,
                    Manifest.permission.CAMERA
                )) {
                // Explain to the user why you need the permission
                showToast("Camera permission is required to scan QR codes.")

                // Show rationale and request permission again
                requestPermissionsLauncher?.launch(
                    arrayOf(
                        Manifest.permission.CAMERA
                    )
                )
            } else {
                // User has denied permission and selected "Don't ask again", guide to settings
                showToast("Camera permission is required for this feature. Please enable it in settings.")
                val intent = Intent()
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:${this.packageName}")
                (this as? AppCompatActivity)?.startActivity(intent)
            }
        }
    }
}

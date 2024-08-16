package com.ashique.qrscanner.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.ImageView
import com.ashique.qrscanner.helper.BitmapHelper.toPath
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

object QrHelper {

    interface QrScanCallback {
        fun onBarcodeScanned(contents: String)
        fun onScanError(errorMessage: String)
    }

    fun scanBitmap(bitmap: Bitmap, callback: QrScanCallback) {
        // Start a coroutine for concurrent scanning
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            // Create a job for the ML Kit scanning
            val mlKitResult = async(Dispatchers.IO) {
                scanWithMlKit(bitmap)
            }

            // Create a job for ZXing scanning
            val zxingResult = async(Dispatchers.IO) {
                scanBitmapZxing(bitmap)
            }

            // Wait for the first successful result
            val result = select {
                mlKitResult.onAwait { it }
                zxingResult.onAwait { it }
            }

            // Handle the result
            result?.let {
                callback.onBarcodeScanned(it)
            } ?: callback.onScanError("No barcodes found.")
        }
    }

    private suspend fun scanWithMlKit(bitmap: Bitmap): String? {
        return suspendCancellableCoroutine { continuation ->
            val barcodeScanner = BarcodeScanning.getClient()
            val inputImage = InputImage.fromBitmap(bitmap, 0) // Rotation is 0 for bitmap

            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    // If barcodes are found, return the first one
                    continuation.resume(barcodes.firstOrNull()?.rawValue)
                }
                .addOnFailureListener {
                    // Return null on failure
                    continuation.resume(null)
                }
        }
    }

    // ZXing scanner function
    fun scanBitmapZxing(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val hints = mutableMapOf<DecodeHintType, Any>(DecodeHintType.TRY_HARDER to true)
            val result = reader.decode(binaryBitmap, hints)

            result.text
        } catch (e: Exception) {
            null
        }
    }

    suspend fun scanQrCodeMl(bitmap: Bitmap): String? {
        return withContext(Dispatchers.IO) {
            val barcodeScanner = BarcodeScanning.getClient()
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
            val inputImage = InputImage.fromBitmap(scaledBitmap, 0) // Rotation is 0 for bitmap

            try {
                val barcodes = Tasks.await(barcodeScanner.process(inputImage))
                barcodes.firstOrNull()?.rawValue
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }


    fun scanQrCode(bitmap: Bitmap): String? {
        try {
            // Resize the image if necessary
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)

            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val pixels = IntArray(width * height)
            scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))


            val reader = MultiFormatReader()
            val hints = mutableMapOf<DecodeHintType, Any>(DecodeHintType.TRY_HARDER to true)
            reader.decode(binaryBitmap,hints)

            val result = reader.decode(binaryBitmap)

            return result.text
        } catch (e: NotFoundException) {
            // Handle case where no QR code is found
            return null
        } catch (e: Exception) {
            // Handle other exceptions
            return null
        }
    }

    fun scanBitmapRealtime(
        bitmap: Bitmap,
        onSuccess: (String?) -> Unit,
        onFailure: () -> Unit
    ) {
        val barcodeScanner = BarcodeScanning.getClient()
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 2, bitmap.height / 2, true)
        val inputImage = InputImage.fromBitmap(scaledBitmap, 0)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull()
                if (barcode?.rawValue != null) {
                    onSuccess(barcode.rawValue)
                } else {
                    // Fallback to ZXing if ML Kit doesn't find anything
                    val result = scanQrCode(bitmap)
                    onSuccess(result)
                }
            }
            .addOnFailureListener {
                // Fallback to ZXing if ML Kit fails
                val result = scanQrCode(bitmap)
                onSuccess(result)
            }
    }

    fun combine(
        qrBitmap: Bitmap,
        bgBitmap: Bitmap,
        colorized: Boolean,
        contrast: Float,
        brightness: Float,
        saveDir: File,
        saveName: String? = null
    ): String? {
        // Resize the background to match the QR code size
        val resizedBgBitmap = Bitmap.createScaledBitmap(bgBitmap, qrBitmap.width, qrBitmap.height, false)

        // Adjust contrast and brightness of the background
        val enhancedBgBitmap = adjustContrastAndBrightness(resizedBgBitmap, contrast, brightness)

        // Create a mutable bitmap to draw the combined image
        val combinedBitmap = Bitmap.createBitmap(qrBitmap.width, qrBitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(combinedBitmap)
        val paint = Paint()

        // Draw the enhanced background first
        canvas.drawBitmap(enhancedBgBitmap, 0f, 0f, paint)

        // Draw the QR code on top
        paint.isFilterBitmap = true
        canvas.drawBitmap(qrBitmap, 0f, 0f, paint)

        // Save the combined image to the specified directory
        val saveFile = File(saveDir, saveName ?: "combined_qr.png")
        val outputStream = FileOutputStream(saveFile)
        combinedBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.flush()
        outputStream.close()

        return saveFile.absolutePath
    }

    // Adjust the contrast and brightness of a bitmap
    private fun adjustContrastAndBrightness(bitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val paint = Paint()
        val contrastScale = contrast
        val brightnessOffset = brightness * 255

        val colorMatrix = android.graphics.ColorMatrix()
        val colorMatrixValues = floatArrayOf(
            contrastScale, 0f, 0f, 0f, brightnessOffset,
            0f, contrastScale, 0f, 0f, brightnessOffset,
            0f, 0f, contrastScale, 0f, brightnessOffset,
            0f, 0f, 0f, 1f, 0f
        )

        colorMatrix.set(colorMatrixValues)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)

        val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config!!)
        val canvas = Canvas(adjustedBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return adjustedBitmap
    }

    fun ImageView.loadAndCombineQr(
        qrUri: Uri,
        bgUri: Uri,
        colorized: Boolean,
        contrast: Float,
        brightness: Float,
        saveName: String? = null
    ) {
        val context: Context = this.context

        val qrPath = qrUri.toPath(context)
        val bgPath = bgUri.toPath(context)

        if (qrPath == null || bgPath == null) {
            Log.e("ImageError", "One or both file paths are null")
            return
        }

        val qrBitmap = BitmapFactory.decodeFile(qrPath)
        val bgBitmap = BitmapFactory.decodeFile(bgPath)

        if (qrBitmap == null || bgBitmap == null) {
            Log.e("BitmapError", "Failed to decode one or both bitmaps")
            return
        }
        val saveDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Qr")
        if (!saveDir.exists()) {
            saveDir.mkdirs() // Creates the directory if it doesn't exist
        }

        val combinedImagePath = combine(qrBitmap, bgBitmap, colorized, contrast, brightness, saveDir, saveName)
        val combinedBitmap = BitmapFactory.decodeFile(combinedImagePath)
        this.setImageBitmap(combinedBitmap)
    }





}
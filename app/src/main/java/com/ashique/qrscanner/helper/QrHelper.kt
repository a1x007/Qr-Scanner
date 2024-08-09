package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
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
}
package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

object QrScanner {
    const val TAG = "QrScanner"
    suspend fun scanQrBitmap(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        val zxingResult = try {
            // Start the Zxing scanning process asynchronously
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = QRCodeMultiReader()
            val hints = mapOf(DecodeHintType.TRY_HARDER to true)

            val results = reader.decodeMultiple(binaryBitmap, hints)
            results.firstOrNull()?.text
        } catch (e: NotFoundException) {
            // Zxing did not find a QR code
            null
        } catch (e: Exception) {
            // Handle other exceptions
            e.printStackTrace()
            null
        }

        if (zxingResult != null) {
            // Return Zxing result if successful
            return@withContext zxingResult
        }

        try {
            // If Zxing failed, try ML Kit scanning
            val barcodeScanner = BarcodeScanning.getClient()
            val inputImage = InputImage.fromBitmap(bitmap, 0) // Rotation is 0 for bitmap
            val barcodes = Tasks.await(barcodeScanner.process(inputImage))
            barcodes.firstOrNull()?.rawValue
        } catch (mlKitException: Exception) {
            // Handle ML Kit exceptions
            mlKitException.printStackTrace()
            null
        }
    }

}
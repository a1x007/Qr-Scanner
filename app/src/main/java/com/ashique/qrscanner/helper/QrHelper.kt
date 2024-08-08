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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrHelper {




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
package com.ashique.qrscanner.helper

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object QrScanner {

    suspend fun scanQrCode(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            // Determine if the image needs to be scaled down
            val maxDimension = 1024
            val shouldResize = bitmap.width > maxDimension || bitmap.height > maxDimension

            val scaledBitmap = if (shouldResize) {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val width = if (aspectRatio > 1) maxDimension else (maxDimension * aspectRatio).toInt()
                val height = if (aspectRatio > 1) (maxDimension / aspectRatio).toInt() else maxDimension
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            } else {
                bitmap
            }

            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val pixels = IntArray(width * height)
            scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            val reader = MultiFormatReader()
            val hints = mapOf(
                DecodeHintType.TRY_HARDER to true,
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.PURE_BARCODE to false
            )

            val result = reader.decode(binaryBitmap, hints)

            result.text
        } catch (e: NotFoundException) {
            null // Handle case where no QR code is found
        } catch (e: Exception) {
            null // Handle other exceptions
        }
    }
}